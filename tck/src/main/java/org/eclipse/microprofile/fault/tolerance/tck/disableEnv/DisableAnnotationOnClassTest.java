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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
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
 * Test the impact of policies disabling through config.
 *
 * The test assumes that the container supports both the MicroProfile Configuration API and the MicroProfile Fault
 * Tolerance API. All Fault tolerance policies are disabled through configuration on the DisabledClient.
 *
 * @author <a href="mailto:antoine@sabot-durand.net">Antoine Sabot-Durand</a>
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 */
public class DisableAnnotationOnClassTest extends Arquillian {

    @Inject
    private DisableAnnotationClient disableClient;

    @Deployment
    public static WebArchive deploy() {
        Asset config = new DisableConfigAsset()
                .disable(DisableAnnotationClient.class, Retry.class)
                .disable(DisableAnnotationClient.class, CircuitBreaker.class)
                .disable(DisableAnnotationClient.class, Timeout.class)
                .disable(DisableAnnotationClient.class, Asynchronous.class)
                .disable(DisableAnnotationClient.class, Fallback.class)
                .disable(DisableAnnotationClient.class, Bulkhead.class);

        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftDisableClass.jar")
                .addClasses(DisableAnnotationClient.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftDisableClass.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * failAndRetryOnce is annotated with maxRetries = 1 so it is expected to execute 2 times but as Retry is disabled,
     * then no retries should be attempted.
     */
    @Test
    public void testRetryDisabled() {
        Assert.assertThrows(TestException.class, () -> disableClient.failAndRetryOnce());
        Assert.assertEquals(disableClient.getFailAndRetryOnceCounter(), 1, "Retry disabled - should be 1 exection");
    }

    /**
     * Test that a Fallback service is ignored when service fails.
     *
     * failRetryOnceThenFallback is annotated with maxRetries = 1 so serviceB is expected to execute 2 times but as
     * Retry is disabled then no retries should be attempted .
     */
    @Test
    public void testFallbackDisabled() {
        // Throw TestException because Fallback is disabled
        Assert.assertThrows(TestException.class, () -> disableClient.failRetryOnceThenFallback());
        // One execution because Retry is disabled
        Assert.assertEquals(disableClient.getFailRetryOnceThenFallbackCounter(), 1,
                "Retry disabled - should be 1 execution");
    }

    /**
     * CircuitBreaker policy being disabled the policy shouldn't be applied
     */
    @Test
    public void testCircuitClosedThenOpen() {
        // Always get TestException on first execution
        Assert.assertThrows(TestException.class, () -> disableClient.failWithCircuitBreaker());
        // Should get TestException on second execution because CircuitBreaker is disabled
        Assert.assertThrows(TestException.class, () -> disableClient.failWithCircuitBreaker());
    }

    /**
     * Test Timeout is disabled, should wait two seconds and then get a TestException
     */
    @Test
    public void testTimeout() {
        // Expect TestException because Timeout is disabled and will not fire
        Assert.assertThrows(TestException.class, () -> disableClient.failWithTimeout());
    }

    /**
     * A test to check that asynchronous is disabled
     *
     * In normal operation, asyncClient.asyncWaitThenReturn() is launched asynchronously. As Asynchronous operation was
     * disabled via config, test is expecting a synchronous operation.
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
            Assert.assertTrue(result.isDone(), "Returned future.isDone() expected true because Async disabled");
        } finally {
            result.get(); // Success or failure, don't leave the future lying around
        }
    }

    /**
     * Test whether Bulkhead is enabled on {@code waitWithBulkhead()}
     *
     * @throws InterruptedException
     *             interrupted
     * @throws ExecutionException
     *             task was aborted
     */
    @Test
    public void testBulkhead() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Start two executions at once
        CompletableFuture<Void> waitingFuture = new CompletableFuture<>();
        Future<?> result1 = executor.submit(() -> disableClient.waitWithBulkhead(waitingFuture));
        Future<?> result2 = executor.submit(() -> disableClient.waitWithBulkhead(waitingFuture));

        try {
            disableClient.waitForBulkheadExecutions(2);

            // Try to start a third execution. This would throw a BulkheadException if Bulkhead is enabled.
            // Bulkhead is disabled on the class so no exception expected
            disableClient.waitWithBulkhead(CompletableFuture.completedFuture(null));
        } finally {
            // Clean up executor and first two executions
            executor.shutdown();

            waitingFuture.complete(null);
            result1.get();
            result2.get();
        }
    }
}
