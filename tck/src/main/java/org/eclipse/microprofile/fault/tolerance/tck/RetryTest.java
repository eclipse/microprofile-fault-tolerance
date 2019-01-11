/*
 *******************************************************************************
 * Copyright (c) 2016-2019 Contributors to the Eclipse Foundation
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

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClassLevelClientForMaxRetries;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientForMaxRetries;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientWithDelay;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientWithNoDelayAndJitter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;
/**
 * Test when maxDuration is reached, no more retries will be perfomed.
 * Test the delay and jitter were taken into consideration.
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class RetryTest extends Arquillian {

    private @Inject RetryClientForMaxRetries clientForMaxRetry;
    private @Inject RetryClientWithDelay clientForDelay;
    private @Inject RetryClassLevelClientForMaxRetries clientForClassLevelMaxRetry;
    private @Inject
    RetryClientWithNoDelayAndJitter retryClientWithNoDelayAndJitter;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftRetry.jar")
                .addClasses(RetryClientForMaxRetries.class,
                            RetryClientWithDelay.class,
                            RetryClassLevelClientForMaxRetries.class,
                            RetryClientWithNoDelayAndJitter.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetry.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test maxRetries. 
     * 
     * As serviceA is annotated with maxRetries = 5, serviceA should be executed 6 times.
     */
    @Test
    public void testRetryMaxRetries() {
        try {
            clientForMaxRetry.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryMaxRetries");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForMaxRetry.getRetryCountForConnectionService(), 6, "The max number of execution should be 6");
    }
    
    @Test
    public void testRetryMaxDuration() {
        try {
            clientForMaxRetry.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testRetryMaxDuration");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int retries = clientForMaxRetry.getRetryCountForWritingService();
        Assert.assertTrue(retries< 11, "The max retry counter should be less than 11 but it was " + retries);
    }

    @Test
    public void testRetryMaxDurationSeconds() {
        try {
            clientForMaxRetry.serviceC();
            Assert.fail("serviceC should throw a RuntimeException in testRetryMaxDuration");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int retries = clientForMaxRetry.getRetryCountForWritingService();

        Assert.assertTrue(retries< 11, "The max retry counter should be less than 11 but it was " + retries);
    }
    
    @Test
    public void testRetryWithDelay() {
        try {
            clientForDelay.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryWithDelay");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        int retryCountForConnectionService = clientForDelay.getRetryCountForConnectionService();

        Assert.assertTrue(retryCountForConnectionService > 4,
            "The max number of execution should be greater than 4 but it was " + retryCountForConnectionService);
        Assert.assertTrue(clientForDelay.isDelayInRange(), "The delay between each retry should be 0-800ms");
    }

    /**
     * Testing whether the {@code @Retry} annotation on method serviceB overrides the Class level
     * {@code @Retry} annotation.
     *
     * Delay is 0 and jitter 400ms. Invocation takes 3200ms and effective delay must be between 0 and 400ms.
     */
    @Test
    public void testRetryWithNoDelayAndJitter() {
        try {
            retryClientWithNoDelayAndJitter.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryWithDelay");
        }
        catch (RuntimeException ex) {
            // Expected
        }

        final int retryCountForConnectionService = retryClientWithNoDelayAndJitter.getRetryCountForConnectionService();

        // we have positive delays
        Assert.assertTrue(retryClientWithNoDelayAndJitter.positiveDelays() > 0,
            "Using jitter must cause some effective delay even when delay is set to 0");

        Assert.assertTrue(retryCountForConnectionService > 8 && retryCountForConnectionService < 100,
            "The max number of execution should be between 8 and 100 but it was " + retryCountForConnectionService +
                ". Too many retries mean jitter is not being applied.");

        Assert.assertTrue(retryClientWithNoDelayAndJitter.isDelayInRange(),
            "The delay between each retry should be 0-400ms");
    }

    /**
     * Analogous to testRetryMaxRetries but using a Class level rather
     * than method level annotation.
     * 
     * With maxRetries = 2, serviceA should be executed 3 times.
     */
    @Test
    public void testClassLevelRetryMaxRetries() {
        try {
            clientForClassLevelMaxRetry.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testClassLevelRetryMaxRetries");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        int retries = clientForClassLevelMaxRetry.getRetryCountForConnectionService();

        Assert.assertEquals(retries, 3, "The max number of execution should be 3 but it was " + retries);
    }

    /**
     * Analogous to testRetryMaxDuration, testing whether the {@code @Retry} annotation on method serviceB overrides the Class level
     * {@code @Retry} annotation.
     * 
     * Ensure that serviceB is executed more than the maxRetries of 2 specified at the Class level.
     */
    @Test
    public void testClassLevelRetryMaxDuration() {
        try {
            clientForClassLevelMaxRetry.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelRetryMaxDuration");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int retryCountforWritingService = clientForClassLevelMaxRetry.getRetryCountForWritingService();        
        Assert.assertTrue(retryCountforWritingService< 11,
            "The max retry counter should be less than 11 but it was " + retryCountforWritingService);
        
        // Further test that we have retried more than the maximum number of retries specified in the Class level {@code @Retry} annotation
        Assert.assertTrue(retryCountforWritingService> 3,
            "The max retry counter should be greater than 3 but it was " + retryCountforWritingService);
    }

    /**
     * Analogous to testRetryMaxDurationSeconds, testing whether the {@code @Retry} annotation on method serviceB overrides the Class level
     * {@code @Retry} annotation.
     * 
     * Ensure that serviceB is executed more than the maxRetries of 2 specified at the Class level.
     */
    @Test
    public void testClassLevelRetryMaxDurationSeconds() {
        try {
            clientForClassLevelMaxRetry.serviceC();
            Assert.fail("serviceC should throw a RuntimeException in testClassLevelRetryMaxDurationSeconds");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int retries = clientForClassLevelMaxRetry.getRetryCountForWritingService();
        Assert.assertTrue(retries < 11, "The max retry counter should be less than 11 but it was" + retries);
    }
}
