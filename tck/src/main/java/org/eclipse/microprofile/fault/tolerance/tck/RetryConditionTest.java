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

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.asyncretry.clientserver.AsyncRetryClient;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClassLevelClientAbortOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClassLevelClientRetryOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientAbortOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientRetryOn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the retryOn and abortOn conditions.
 * If retryOn condition is not met, no retry will be performed.
 * If abortOn condition is met, no retry will be performed.
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class RetryConditionTest extends Arquillian {

    private @Inject RetryClientRetryOn clientForRetryOn;
    private @Inject RetryClientAbortOn clientForAbortOn;
    private @Inject RetryClassLevelClientRetryOn clientForClassLevelRetryOn;
    private @Inject RetryClassLevelClientAbortOn clientForClassLevelAbortOn;
    private @Inject AsyncRetryClient asyncRetryClient;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftRetryCondition.jar")
                        .addClasses(RetryClientAbortOn.class, RetryClientRetryOn.class,
                                        RetryClassLevelClientRetryOn.class,
                                        RetryClassLevelClientAbortOn.class,
                                        AsyncRetryClient.class,
                                        CompletableFutureHelper.class)
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                        .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryCondition.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation is encountered.
     *
     * serviceA is configured to retry on a RuntimeException. The service should be retried 3 times.
     */
    @Test
    public void testRetryOnTrue() {
        try {
            clientForRetryOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryOnTrue");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForConnectionService(), 4, "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Test that no retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation
     * is NOT encountered.
     *
     * serviceB is configured to retry on an IOException. In practice the only exception that the service
     * will throw is a RuntimeException, therefore no retries should be executed.
     */
    @Test
    public void testRetryOnFalse() {
        try {
            clientForRetryOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testRetryOnFalse");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForWritingService(), 1,
            "The max invocation counter should be 1 as the retry condition is false");
    }

    /**
     * Test that the default number of retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation
     * is NOT encountered.
     *
     * serviceA is configured to abort on an IOException. In practice the only exception that the service
     * will throw is a RuntimeException, therefore the default number of 3 retries should be executed.
     */
    @Test
    public void testRetryWithAbortOnFalse() {
        try {
            clientForAbortOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryWithAbortOnFalse");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForAbortOn.getRetryCountForConnectionService(), 4, "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Test that no retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation
     * is encountered.
     *
     * serviceB is configured to abort on a RuntimeException. The service should not be retried.
     */
    @Test
    public void testRetryWithAbortOnTrue() {
        try {
            clientForAbortOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testRetryWithAbortOnTrue");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForAbortOn.getRetryCountForWritingService(), 1,
            "The max invocation counter should be 1 as the abort condition is true");
    }

    /**
     * Analogous to testRetryOnTrue but using a Class level rather than method level annotation.
     *
     * serviceA is configured to retry on a RuntimeException. The service should be retried 3 times.
     */
    @Test
    public void testClassLevelRetryOnTrue() {
        try {
            clientForClassLevelRetryOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testClassLevelRetryOnTrue");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelRetryOn.getRetryCountForConnectionService(), 4, "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Analogous to testRetryonFalse, testing whether the {@code @Retry} annotation on method serviceB overrides the Class level
     * {@code @Retry} annotation.
     *
     * serviceB is configured to retry on an IOException. In practice the only exception that the service
     * will throw is a RuntimeException, therefore no retries should be executed.
     */
    @Test
    public void testClassLevelRetryOnFalse() {
        try {
            clientForClassLevelRetryOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelRetryOnFalse");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelRetryOn.getRetryCountForWritingService(), 1,
            "The execution count should be 1 as the retry condition is false");
    }

    /**
     * Analogous to testRetryWithAbortOnFalse but using a Class level rather than method level {@code @Retry} annotation.
     * Test that the default number of retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation
     * is NOT encountered.
     *
     * The Class, and therefore serviceA, is configured to abort on an IOException. In practice the only exception that the service
     * will throw is a RuntimeException, therefore the default number of 3 retries should be executed.
     */
    @Test
    public void testClassLevelRetryWithAbortOnFalse() {
        try {
            clientForClassLevelAbortOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testClassLevelRetryWithAbortOnFalse");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelAbortOn.getRetryCountForConnectionService(), 4, "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Analogous to testRetryWithAbortOnTrue, testing whether the {@code @Retry} annotation on method serviceB overrides the Class level
     * {@code @Retry} annotation.
     *
     * Test that no retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation
     * is encountered.
     *
     * serviceB is configured to abort on a RuntimeException. The service should not be retried.
     */
    @Test
    public void testClassLevelRetryWithAbortOnTrue() {
        try {
            clientForClassLevelAbortOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelRetryWithAbortOnTrue");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelAbortOn.getRetryCountForWritingService(), 1,
            "The max invocation counter should be 1 as the abort condition is true");
    }


    /**
     * Persistent Error condition. Will retry 2 times and still throw exception.
     * ServiceA uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and will always return IOException.
     */
    @Test
    public void testAsyncRetryExceptionally() {
        final CompletionStage<String> future = asyncRetryClient.serviceA();

        assertCompleteExceptionally(future, IOException.class, "Simulated error");
        assertEquals(3, asyncRetryClient.getCountInvocationsServA());
    }

    /**
     * Persistent Error condition inside a CompletableFuture.
     * Will not retry because method is not marked with @Asynchronous
     * ServiceB will always complete exceptionally with IOException.
     */
    @Test
    public void testNoAsynWilNotRetryExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();

        assertCompleteExceptionally(asyncRetryClient.serviceBFailExceptionally(future),
            IOException.class, "Simulated error");
        // no retries
        assertEquals("No retries are expected",1,
            asyncRetryClient.getCountInvocationsServBFailExceptionally());
    }

    /**
     * Persistent Error condition outside the CompletableFuture.
     * Will retry because
     * ServiceB will always throw IOException.
     */
    @Test
    public void testNoAsynRetryOnMethodException() {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            asyncRetryClient.serviceBFailException(future);
            fail("Was expecting an exception");
        }
        catch (RuntimeException e) {
            assertEquals("Simulated error", e.getMessage());
        }
        // 2 retries
        assertEquals(3, asyncRetryClient.getCountInvocationsServBFailException());
    }

    /**
     * Temporary error. Will retry 3 times, the first 2 will fail.
     * ServiceC uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous}.
     */
    @Test
    public void testRetrySuccess() {
        final CompletionStage<String> future = asyncRetryClient.serviceC();

        assertCompleteOk(future, "Success");
        assertEquals(3, asyncRetryClient.getCountInvocationsServC());
    }

    /**
     * Temporary error. Will retry 3 times, the first 2 will fail deep in a CompletableFuture chained execution.
     * ServiceD uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and chains 2 CompletableFutures.
     */
    @Test
    public void testRetryChainSuccess() {
        final CompletionStage<String> future = asyncRetryClient.serviceD();

        assertCompleteOk(future, "Success");
        assertEquals(3, asyncRetryClient.getCountInvocationsServD());
    }

    /**
     * Persistent Error condition. Will retry 3 times and still throw exception.
     * ServiceE will always return IOException.
     */
    @Test
    public void testRetryChainExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();

        asyncRetryClient.serviceE();

        assertCompleteExceptionally(future, IOException.class, "Simulated error");
        assertEquals(3, asyncRetryClient.getCountInvocationsServE());
    }

    /**
     * Persistent Error condition. Will retry 3 times and still throw exception.
     * ServiceF will always return IOException.
     */
    @Test
    public void testRetryParallelExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();

        asyncRetryClient.serviceG();

        assertCompleteExceptionally(future, IOException.class, "Simulated error");
        assertEquals(3, asyncRetryClient.getCountInvocationsServG());
    }

    /**
     * Temporary error. Will retry 3 times, the first 2 will fail in a CompletableFuture parallel execution.
     * ServiceG uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and 2 CompletableFutures.
     */
    @Test
    public void testRetryParallelSuccess() {
        CompletableFuture<String> future = new CompletableFuture<>();

        asyncRetryClient.serviceF();

        assertCompleteOk(future, "Success then Success");
        assertEquals(3, asyncRetryClient.getCountInvocationsServF());
    }

    private void assertCompleteExceptionally(final CompletionStage<String> future,
                                             final Class<? extends Throwable> exceptionClass,
                                             final String exceptionMessage) {
        try {
            CompletableFutureHelper.toCompletableFuture(future).get(200, TimeUnit.MILLISECONDS);
            fail("We were expecting an exception: " + exceptionClass.getName() + " with message: " + exceptionMessage);
        }
        catch (InterruptedException | TimeoutException e) {
            fail("Unexpected exception" + e);
        }
        catch (ExecutionException ee) {
            Assert.assertTrue(
                exceptionClass.isInstance(ee.getCause()), "Cause of ExecutionException was " + ee.getCause());
            assertEquals(exceptionMessage, ee.getCause().getMessage());
        }
    }

    private void assertCompleteOk(final CompletionStage<String> future, final String expectedMessage) {
        try {
            assertEquals(
                expectedMessage, CompletableFutureHelper.toCompletableFuture(future).get(1000, TimeUnit.MILLISECONDS));
        }
        catch (Exception e) {
            fail("Unexpected exception" + e);
        }
    }
}
