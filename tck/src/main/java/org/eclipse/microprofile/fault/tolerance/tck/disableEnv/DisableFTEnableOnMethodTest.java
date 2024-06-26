/*
 *******************************************************************************
 * Copyright (c) 2016-2018 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.disableEnv;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that annotations can be disabled at the class level and then re-enabled at the method level.
 *
 * @author <a href="mailto:antoine@sabot-durand.net">Antoine Sabot-Durand</a>
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 */
public class DisableFTEnableOnMethodTest extends Arquillian {

    @Inject
    private DisableAnnotationClient disableClient;

    @Deployment
    public static WebArchive deploy() {
        Asset config = new DisableConfigAsset()
                .enable(DisableAnnotationClient.class, "failAndRetryOnce", Retry.class)
                .enable(DisableAnnotationClient.class, "failWithCircuitBreaker", CircuitBreaker.class)
                .enable(DisableAnnotationClient.class, "failWithTimeout", Timeout.class)
                .enable(DisableAnnotationClient.class, "asyncWaitThenReturn", Asynchronous.class)
                .enable(DisableAnnotationClient.class, "failRetryOnceThenFallback", Fallback.class)
                .enable(DisableAnnotationClient.class, "waitWithBulkhead", Bulkhead.class)
                .disableGlobally()
                .enable(AsyncCaller.class, Asynchronous.class); // Needed by AsyncTaskManager;

        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftDisableGloballyEnableMethod.jar")
                .addClasses(DisableAnnotationClient.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftDisableGloballyEnableMethod.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * failAndRetryOnce is annotated with maxRetries = 1 so it is expected to execute 2 times.
     */
    @Test
    public void testRetryEnabled() {
        // Always get a TestException
        Assert.assertThrows(TestException.class, () -> disableClient.failAndRetryOnce());
        // Should get two attempts if retry is enabled
        Assert.assertEquals(disableClient.getFailAndRetryOnceCounter(), 2, "Retry enabled - should be 2 exections");
    }

    /**
     * CircuitBreaker is enabled on the method so the policy should be applied
     */
    @Test
    public void testCircuitBreaker() {
        // Always get TestException on first execution
        Assert.assertThrows(TestException.class, () -> disableClient.failWithCircuitBreaker());
        // Should get CircuitBreakerOpenException on second execution because CircuitBreaker is enabled
        Assert.assertThrows(CircuitBreakerOpenException.class, () -> disableClient.failWithCircuitBreaker());
    }

    /**
     * Test Timeout is enabled, should fail with a timeout exception
     */
    @Test
    public void testTimeout() {
        // Expect TimeoutException because Timeout is enabled and method will time out
        Assert.assertThrows(TimeoutException.class, () -> disableClient.failWithTimeout());
    }

    /**
     * A test to check that asynchronous is enabled
     *
     * @throws InterruptedException
     *             interrupted
     * @throws ExecutionException
     *             task was aborted
     */
    @Test
    public void testAsync() throws InterruptedException, ExecutionException {
        Future<?> result = disableClient.asyncWaitThenReturn();
        try {
            Assert.assertFalse(result.isDone(), "Returned future.isDone() expected false because Async enabled");
        } finally {
            result.get(); // Success or failure, don't leave the future lying around
        }
    }

    /**
     * Test whether Bulkhead is enabled on {@code waitWithBulkhead()}
     */
    @Test
    public void testBulkhead() {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            // Start two executions at once
            BarrierTask<?> task1 = taskManager.runBarrierTask(disableClient::waitWithBulkhead);
            BarrierTask<?> task2 = taskManager.runBarrierTask(disableClient::waitWithBulkhead);
            task1.assertAwaits();
            task2.assertAwaits();

            // Try to start a third execution. This would throw a BulkheadException if Bulkhead is enabled.
            // Bulkhead is enabled on the method, so expect exception
            BarrierTask<?> task3 = taskManager.runBarrierTask(disableClient::waitWithBulkhead);
            task3.openBarrier();
            task3.assertThrows(BulkheadException.class);
        }
    }
}
