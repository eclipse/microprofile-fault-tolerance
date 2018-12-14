/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.asynctimeout.clientserver.AsyncClassLevelTimeoutClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynctimeout.clientserver.AsyncTimeoutClient;

import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;
/**
 * Test the combination of the @Asynchronous and @Timeout annotations.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class AsyncTimeoutTest extends Arquillian {

    private static final long TEST_FUTURE_THRESHOLD = 2000; // Used to detect Futures that return too slowly
    private static final int TEST_TIMEOUT_SERVICEA = 2000; // The @Timeout specified on serviceA
    private static final int TEST_TIME_UNIT = 1000; // One second unit
    
    private @Inject AsyncTimeoutClient clientForAsyncTimeout;
    private @Inject AsyncClassLevelTimeoutClient clientForClassLevelAsyncTimeout;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsyncTimeout.jar")
                .addClasses(AsyncTimeoutClient.class,AsyncClassLevelTimeoutClient.class,Connection.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftAsyncTimeout.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that an Asynchronous Service times out as expected where the service is annotated with both
     * the @Asynchronous and @Timeout annotations.
     * 
     * A timeout is configured for serviceA but serviceA has a 5 second sleep so that, in this case, the 
     * service should generate Timeout exceptions.
     */
    @Test
    public void testAsyncTimeout() { 

        // Call serviceA. As it is annotated @Asynchronous, serviceA should return a future straight away even though 
        // the method has a 5s sleep in it
        long start = System.nanoTime();

        Future<Connection> future = null;
        try {
            future = clientForAsyncTimeout.serviceA();
        }
        catch (InterruptedException e1) {
            throw new AssertionError("testAsyncTimeout: unexpected InterruptedException calling serviceA");
        }
        
        long end = System.nanoTime();

        long duration = end - start;
        if (duration > TEST_FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes TEST_FUTURE_THRESHOLD 
            // then there is something wrong
            throw new AssertionError("testAsyncTimeout: Method did not return quickly enough: " + duration);
        }
        
        // serviceA is slow (5 second sleep) but is configured with a 2 second Timeout. It should complete after 2 seconds
        // throwing a wrapped TimeoutException.
        
        // First check that the future hasn't completed prematurely
        if (future.isDone()) {
            throw new AssertionError("testAsyncTimeout: Future completed too fast");
        }

        // Call future.get() with a timeout (3 seconds) that is longer than the annotated timeout (2 seconds) specified on
        // the service but shorter than the overall service duration (5 seconds sleep)
        try {
            future.get(TEST_TIMEOUT_SERVICEA + TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
            throw new AssertionError("testAsyncTimeout: Future not interrupted");
        }
        catch (ExecutionException e) {
            Assert.assertSame(e.getCause().getClass(), 
                              org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class, 
                              "Should be a wrapped TimeoutException");
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsyncTimeout: unexpected InterruptedException on future.get()");
        }
        catch (TimeoutException e) {
            throw new AssertionError("testAsyncTimeout: unexpected TimeoutException on future.get()");
        }
        end = System.nanoTime();

        duration = end - start;
        if (Duration.ofNanos(duration) < Duration.ofMillis(TEST_TIMEOUT_SERVICEA)) { // duration should be greater than the timeout configured on the service 
            throw new AssertionError("testAsyncTimeout: the service duration was less than the configured timeout - " + duration);
        }
    }

    /**
     * Test that an Asynchronous Service does not throw a TimeoutException where the service completes more
     * quickly than the specified time out. The service is annotated with both @Asynchronous and @Timeout.
     * 
     * A 2 second timeout is configured for serviceB but serviceB has a 0.5 second sleep so that, in this case, the 
     * service should NOT generate Timeout exceptions.
     */
    @Test
    public void testAsyncNoTimeout() {
        // Call serviceB. As it is annotated @Asynchronous, serviceB should return a future straight away even though 
        // the method has a 0.5s sleep in it
        long start = System.nanoTime();

        Future<Connection> future = null;
        try {
            future = clientForAsyncTimeout.serviceB();
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsyncNoTimeout: unexpected InterruptedException calling serviceB");
        }
        long end = System.nanoTime();

        long duration = end - start;
        if (duration > TEST_FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes TEST_FUTURE_THRESHOLD 
            // then there is something wrong
            throw new AssertionError("testAsyncNoTimeout: Method did not return quickly enough: " + duration);
        }
        
        // serviceB is fast and should return normally after 0.5 seconds but check for premature
        if (future.isDone()) {
            throw new AssertionError("testAsyncNoTimeout: Future completed too fast");
        }

        // The service should complete normally, there should be no FT TimeoutException
        try {
            Connection conn = future.get(TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
        } 
        catch (Exception t) {
            // Not Expected
            Assert.fail("serviceB should not throw an Exception in testAsyncNoTimeout");
        }
    }
    
    /**
     * Analogous to testAsyncTimeout but using Class level rather than method level annotations.
     * 
     * A timeout is configured for serviceA but serviceA has a 5 second sleep so that, in this case, the 
     * service should generate Timeout exceptions.
     */
    @Test
    public void testAsyncClassLevelTimeout() {
        // Call serviceA. As it is annotated @Asynchronous, serviceA should return a future straight away even though 
        // the method has a 5s sleep in it
        long start = System.nanoTime();

        Future<Connection> future = null;
        try {
            future = clientForClassLevelAsyncTimeout.serviceA();
        }
        catch (InterruptedException e1) {
            throw new AssertionError("testAsyncClassLevelTimeout: unexpected InterruptedException calling serviceA");
        }
        long end = System.nanoTime();

        long duration = end - start;
        if (duration > TEST_FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes TEST_FUTURE_THRESHOLD 
            // then there is something wrong
            throw new AssertionError("testAsyncClassLevelTimeout: Method did not return quickly enough: " + duration);
        }
        
        // serviceA is slow (5 second sleep) but is configured with a 2 second Timeout. It should complete after 2 seconds
        // throwing a wrapped TimeoutException.
        
        // First check that the future hasn't completed prematurely
        if (future.isDone()) {
            throw new AssertionError("testAsyncClassLevelTimeout: Future completed too fast");
        }

        // Call future.get() with a timeout (3 seconds) that is longer than the annotated timeout (2 seconds) specified on
        // the service but shorter than the overall service duration (5 seconds sleep)
        start = System.nanoTime();
        try {
            future.get(TEST_TIMEOUT_SERVICEA + TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
            throw new AssertionError("testAsyncClassLevelTimeout: Future not interrupted");
        }
        catch (ExecutionException e) {
             Assert.assertSame(e.getCause().getClass(), 
                               org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class, 
                               "Should be a wrapped TimeoutException");
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsyncClassLevelTimeout: unexpected InterruptedException on future.get()");
        }
        catch (TimeoutException e) {
            throw new AssertionError("testAsyncClassLevelTimeout: unexpected TimeoutException on future.get()");
        }
        end = System.nanoTime();

        duration = end - start;
        if (Duration.ofNanos(duration) < Duration.ofMillis(TEST_TIMEOUT_SERVICEA)) { // duration should be greater than the timeout configured on the service 
            throw new AssertionError("testAsyncClassLevelTimeout: the service duration was less than the configured timeout - " + duration);
        }
    }
}
