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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.asyncretry.clientserver.AsyncRetryClient;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClassLevelClientAbortOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClassLevelClientRetryOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientAbortOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientRetryOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.exceptions.RetryChildException;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.exceptions.RetryParentException;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCallerExecutor;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test the retryOn and abortOn conditions. If retryOn condition is not met, no retry will be performed. If abortOn
 * condition is met, no retry will be performed.
 * 
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
                        AsyncCallerExecutor.class,
                        AsyncCaller.class,
                        AsyncRetryClient.class,
                        CompletableFutureHelper.class,
                        RetryChildException.class,
                        RetryParentException.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryCondition.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation is
     * encountered.
     *
     * serviceA is configured to retry on a RuntimeException. The service should be retried 3 times.
     */
    @Test
    public void testRetryOnTrue() {
        try {
            clientForRetryOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryOnTrue");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForConnectionService(), 4,
                "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Test that no retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation is NOT
     * encountered.
     *
     * serviceB is configured to retry on an IOException. In practice the only exception that the service will throw is
     * a RuntimeException, therefore no retries should be executed.
     */
    @Test
    public void testRetryOnFalse() {
        try {
            clientForRetryOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testRetryOnFalse");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForWritingService(), 1,
                "The max invocation counter should be 1 as the retry condition is false");
    }

    /**
     * Test that retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation is
     * encountered by inheritance.
     *
     * Service that throws a child custom exception but in the retry on list is configured child's parent custom
     * exception
     */
    @Test
    public void testRetryOnTrueThrowingAChildCustomException() {
        try {
            clientForRetryOn.serviceC();
            Assert.fail("serviceC should throw a RetryChildException in testRetryOnTrueThrowingAChildCustomException");
        } catch (RetryChildException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForConnectionService(), 4,
                "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Test that retries are executed where a failure declared as "retry on" in the {@code @Retry} annotation is
     * encountered by inheritance.
     *
     * Service that throws a child custom exception but in the retry on list is configured child's parent custom
     * exception and in the abort on list is configured the child custom exception.
     *
     * For this case the retry on will be false and the abort on will be true due the class configured in the abort on
     * list is equals to the exception that is throwing by the serviceD not like in the retry on list where is
     * configured the parent exception class of the throwing by the serviceD. So the highest priority will be when the
     * exception type is equals
     */
    @Test
    public void testRetryOnFalseAndAbortOnTrueThrowingAChildCustomException() {
        try {
            clientForRetryOn.serviceD();
            Assert.fail(
                    "serviceC should throw a RetryChildException in testRetryOnFalseAndAbortOnTrueThrowingAChildCustomException");
        } catch (RetryChildException ex) {
            // Expected
        }
        Assert.assertEquals(clientForRetryOn.getRetryCountForConnectionService(), 1,
                "The max invocation counter should be 1 as the retry condition is false");
    }

    /**
     * Test that the default number of retries are executed where a failure declared as "abort on" in the {@code @Retry}
     * annotation is NOT encountered.
     *
     * serviceA is configured to abort on an IOException. In practice the only exception that the service will throw is
     * a RuntimeException, therefore the default number of 3 retries should be executed.
     */
    @Test
    public void testRetryWithAbortOnFalse() {
        try {
            clientForAbortOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryWithAbortOnFalse");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForAbortOn.getRetryCountForConnectionService(), 4,
                "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Test that no retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation is
     * encountered.
     *
     * serviceB is configured to abort on a RuntimeException. The service should not be retried.
     */
    @Test
    public void testRetryWithAbortOnTrue() {
        try {
            clientForAbortOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testRetryWithAbortOnTrue");
        } catch (RuntimeException ex) {
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
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelRetryOn.getRetryCountForConnectionService(), 4,
                "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Analogous to testRetryonFalse, testing whether the {@code @Retry} annotation on method serviceB overrides the
     * Class level {@code @Retry} annotation.
     *
     * serviceB is configured to retry on an IOException. In practice the only exception that the service will throw is
     * a RuntimeException, therefore no retries should be executed.
     */
    @Test
    public void testClassLevelRetryOnFalse() {
        try {
            clientForClassLevelRetryOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelRetryOnFalse");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelRetryOn.getRetryCountForWritingService(), 1,
                "The execution count should be 1 as the retry condition is false");
    }

    /**
     * Analogous to testRetryWithAbortOnFalse but using a Class level rather than method level {@code @Retry}
     * annotation. Test that the default number of retries are executed where a failure declared as "abort on" in the
     * {@code @Retry} annotation is NOT encountered.
     *
     * The Class, and therefore serviceA, is configured to abort on an IOException. In practice the only exception that
     * the service will throw is a RuntimeException, therefore the default number of 3 retries should be executed.
     */
    @Test
    public void testClassLevelRetryWithAbortOnFalse() {
        try {
            clientForClassLevelAbortOn.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testClassLevelRetryWithAbortOnFalse");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelAbortOn.getRetryCountForConnectionService(), 4,
                "The execution count should be 4 (3 retries + 1)");
    }

    /**
     * Analogous to testRetryWithAbortOnTrue, testing whether the {@code @Retry} annotation on method serviceB overrides
     * the Class level {@code @Retry} annotation.
     *
     * Test that no retries are executed where a failure declared as "abort on" in the {@code @Retry} annotation is
     * encountered.
     *
     * serviceB is configured to abort on a RuntimeException. The service should not be retried.
     */
    @Test
    public void testClassLevelRetryWithAbortOnTrue() {
        try {
            clientForClassLevelAbortOn.serviceB();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelRetryWithAbortOnTrue");
        } catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(clientForClassLevelAbortOn.getRetryCountForWritingService(), 1,
                "The max invocation counter should be 1 as the abort condition is true");
    }

    /**
     * Persistent Error condition. Will retry 2 times and still throw exception. ServiceA uses
     * {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and will always return IOException.
     */
    @Test
    public void testAsyncRetryExceptionally() {
        final CompletionStage<String> future = asyncRetryClient.serviceA();

        assertCompleteExceptionally(future, IOException.class, "Simulated error");
        assertEquals(asyncRetryClient.getCountInvocationsServA(), 3);
    }

    /**
     * Persistent Error condition inside a CompletableFuture. Will not retry because method is not marked
     * with @Asynchronous ServiceB will always complete exceptionally with IOException.
     */
    @Test
    public void testNoAsynWilNotRetryExceptionally() {
        CompletableFuture<String> future = new CompletableFuture<>();

        assertCompleteExceptionally(asyncRetryClient.serviceBFailExceptionally(future),
                IOException.class, "Simulated error");
        // no retries
        assertEquals(asyncRetryClient.getCountInvocationsServBFailExceptionally(), 1, "No retries are expected");
    }

    /**
     * Persistent Error condition outside the CompletableFuture. Will retry because ServiceB will always throw
     * IOException.
     */
    @Test
    public void testNoAsynRetryOnMethodException() {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            asyncRetryClient.serviceBFailException(future);
            fail("Was expecting an exception");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Simulated error");
        }
        // 2 retries
        assertEquals(asyncRetryClient.getCountInvocationsServBFailException(), 3);
    }

    /**
     * Temporary error. Will retry 2 times, the first 2 executions will fail. ServiceC uses
     * {@link org.eclipse.microprofile.faulttolerance.Asynchronous}.
     */
    @Test
    public void testRetrySuccess() {
        final CompletionStage<String> future = asyncRetryClient.serviceC();

        assertCompleteOk(future, "Success");
        assertEquals(asyncRetryClient.getCountInvocationsServC(), 3);
    }

    /**
     * Temporary error. Will retry 2 times, the first 2 executions will fail deep in a CompletableFuture chained
     * execution. ServiceD uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and chains 2
     * CompletableFutures.
     */
    @Test
    public void testRetryChainSuccess() {
        final CompletionStage<String> future = asyncRetryClient.serviceD();

        assertCompleteOk(future, "Success");
        assertEquals(asyncRetryClient.getCountInvocationsServD(), 3);
    }

    /**
     * Persistent Error condition. Will retry 3 times and still throw exception. ServiceE will always return
     * IOException.
     */
    @Test
    public void testRetryChainExceptionally() {
        assertCompleteExceptionally(asyncRetryClient.serviceE(), RuntimeException.class, "Simulated error");
        assertEquals(asyncRetryClient.getCountInvocationsServE(), 3);
    }

    /**
     * Persistent Error condition. Will retry 3 times and still throw exception. ServiceF will always return
     * IOException.
     */
    @Test
    public void testRetryParallelExceptionally() {
        assertCompleteExceptionally(asyncRetryClient.serviceG(), RuntimeException.class, "Simulated error");
        assertEquals(asyncRetryClient.getCountInvocationsServG(), 3);
    }

    /**
     * Temporary error. Will retry 2 times, the first 2 executions fail in a CompletableFuture parallel execution.
     * ServiceG uses {@link org.eclipse.microprofile.faulttolerance.Asynchronous} and 2 CompletableFutures.
     */
    @Test
    public void testRetryParallelSuccess() {
        assertCompleteOk(asyncRetryClient.serviceF(), "Success then Success");
        assertEquals(asyncRetryClient.getCountInvocationsServF(), 3);
    }

    /**
     * Temporary error. Will retry 2 times, the first 2 executions fail and the method will throw an exception.
     */
    @Test
    public void testRetryCompletionStageWithException() {
        assertCompleteOk(asyncRetryClient.serviceH(), "Success");
        assertEquals(asyncRetryClient.getCountInvocationsServH(), 3);
    }

    private void assertCompleteExceptionally(final CompletionStage<String> future,
            final Class<? extends Throwable> exceptionClass,
            final String exceptionMessage) {
        try {
            CompletableFutureHelper.toCompletableFuture(future).get(TCKConfig.getConfig().getTimeoutInMillis(1000),
                    TimeUnit.MILLISECONDS);
            fail("We were expecting an exception: " + exceptionClass.getName() + " with message: " + exceptionMessage);
        } catch (InterruptedException | TimeoutException e) {
            fail("Unexpected exception " + e, e);
        } catch (ExecutionException ee) {
            assertThat("Cause of ExecutionException", ee.getCause(), instanceOf(exceptionClass));
            assertEquals(ee.getCause().getMessage(), exceptionMessage);
        }
    }

    private void assertCompleteOk(final CompletionStage<String> future, final String expectedMessage) {
        try {
            assertEquals(CompletableFutureHelper
                    .toCompletableFuture(future)
                    .get(TCKConfig.getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS), expectedMessage);
        } catch (Exception e) {
            fail("Unexpected exception" + e);
        }
    }
}
