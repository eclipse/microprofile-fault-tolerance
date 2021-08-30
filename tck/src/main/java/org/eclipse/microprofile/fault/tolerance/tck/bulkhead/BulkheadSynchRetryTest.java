/*
 *******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;
import java.util.function.Consumer;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead1Retry0MethodSyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead1Retry1SyncClassBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead1Retry1SyncMethodBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead1RetryManySyncClassBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead1RetryManySyncMethodBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryAbortOnSyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * This collection of tests tests that failures, particularly Synchronous Bulkhead related failures will cause the Retry
 * annotation logic to work correctly.
 *
 * @author Gordon Hutchison
 * @author Andrew Rouse
 *
 */
public class BulkheadSynchRetryTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only works on business methods of injected objects we need to inject a variety
     * of these for use by the tests below. The naming convention indicates if the annotation is on a class or method,
     * asynchronous or semaphore based, the size/value of the @Bulkhead and whether we have queueing or not.
     */
    @Inject
    private Bulkhead1RetryManySyncMethodBean longRetryMethodBean;

    @Inject
    private Bulkhead1RetryManySyncClassBean longRetryClassBean;

    @Inject
    private Bulkhead1Retry1SyncMethodBean retry1DelayMethodBean;

    @Inject
    private Bulkhead1Retry1SyncClassBean retry1DelayClassBean;

    @Inject
    private BulkheadRetryAbortOnSyncBean retryAbortOnBean;

    @Inject
    private Bulkhead1Retry0MethodSyncBean zeroRetryBean;

    /**
     * This is the Arquillian deploy method that controls the contents of the war that contains all the tests.
     *
     * @return the test war "ftBulkheadSynchRetryTest.war"
     */
    @Deployment
    public static WebArchive deploy() {

        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .autoscaleMethod(Bulkhead1RetryManySyncMethodBean.class, "test")
                .autoscaleClass(Bulkhead1RetryManySyncClassBean.class)
                .autoscaleMethod(Bulkhead1Retry1SyncMethodBean.class, "test")
                .autoscaleClass(Bulkhead1Retry1SyncClassBean.class)
                .autoscaleClass(BulkheadRetryAbortOnSyncBean.class)
                .autoscaleMethod(Bulkhead1Retry0MethodSyncBean.class, "test");

        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchRetryTest.jar")
                .addPackage(Bulkhead1RetryManySyncMethodBean.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        return ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchRetryTest.war").addAsLibrary(testJar);
    }

    /**
     * Test that Bulkhead exceptions are retried with annotations on method
     */
    public void testRetryBulkheadExceptionMethod() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<Void> taskA = taskManager.runBarrierTask(longRetryMethodBean::test);
            taskA.assertAwaits();

            BarrierTask<Void> taskB = taskManager.runBarrierTask(longRetryMethodBean::test);

            // taskB should immediately fail and be retried because the bulkhead is full
            taskB.assertNotAwaiting();

            // Release taskA, allowing taskB to start when it retries
            taskA.openBarrier();
            taskA.assertSuccess();

            // Assert taskB runs and completes
            taskB.assertAwaits();
            taskB.openBarrier();
            taskB.assertSuccess();
        }
    }

    /**
     * Test that Bulkhead exceptions are retried with annotations on class
     */
    public void testRetryBulkheadExceptionClass() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<Void> taskA = taskManager.runBarrierTask(longRetryClassBean::test);
            taskA.assertAwaits();

            BarrierTask<Void> taskB = taskManager.runBarrierTask(longRetryClassBean::test);

            // taskB should immediately fail and be retried because the bulkhead is full
            taskB.assertNotAwaiting();

            // Release taskA, allowing taskB to start when it retries
            taskA.openBarrier();
            taskA.assertSuccess();

            // Assert taskB runs and completes
            taskB.assertAwaits();
            taskB.openBarrier();
            taskB.assertSuccess();
        }
    }

    /**
     * Test Bulkhead + Retry when the method throws a business exception with annotations on class
     * <p>
     * Test that:
     * <ul>
     * <li>the execution is retried
     * <li>when the execution is retried, it doesn't hold onto its bulkhead slot
     * </ul>
     * <p>
     * This second point is particularly important if Retry is used with a long delay.
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     */
    @Test
    public void testRetryTestExceptionClass() throws InterruptedException {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            // Start taskA
            BarrierTask<Void> taskA =
                    taskManager.runBarrierTask(barrier -> retry1DelayClassBean.test(barrier, new TestException()));
            taskA.assertAwaits();

            // Cause it to fail, prompting a retry after 1 second
            taskA.openBarrier();

            // Wait a short while for taskA to clear the bulkhead
            Thread.sleep(TCKConfig.getConfig().getTimeoutInMillis(100));

            // Now, start taskB
            BarrierTask<Void> taskB = taskManager.runBarrierTask(barrier -> retry1DelayClassBean.test(barrier, null));
            // Task B should start because the bulkhead is empty while taskA waits to retry
            taskB.assertAwaits();

            // Now, when taskA retries, it should complete with a BulkheadException because the bulkhead is full because
            // taskB is running
            taskA.assertThrows(BulkheadException.class);

            // Now let taskB complete
            taskB.openBarrier();
            taskB.assertSuccess();
        }
    }

    /**
     * Test Bulkhead + Retry when the method throws a business exception with annotations on method
     * <p>
     * Test that:
     * <ul>
     * <li>the execution is retried
     * <li>when the execution is retried, it doesn't hold onto its bulkhead slot
     * </ul>
     * <p>
     * This second point is particularly important if Retry is used with a long delay.
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     */
    @Test
    public void testRetryTestExceptionMethod() throws InterruptedException {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            // Start taskA
            BarrierTask<Void> taskA =
                    taskManager.runBarrierTask(barrier -> retry1DelayMethodBean.test(barrier, new TestException()));
            taskA.assertAwaits();

            // Cause it to fail, prompting a retry after 1 second
            taskA.openBarrier();

            // Wait a short while for taskA to clear the bulkhead
            Thread.sleep(TCKConfig.getConfig().getTimeoutInMillis(100));

            // Now, start taskB
            BarrierTask<Void> taskB = taskManager.runBarrierTask(barrier -> retry1DelayMethodBean.test(barrier, null));
            // Task B should start because the bulkhead is empty while taskA waits to retry
            taskB.assertAwaits();

            // Now, when taskA retries, it should complete with a BulkheadException because the bulkhead is full because
            // taskB is running
            taskA.assertThrows(BulkheadException.class);

            // Now let taskB complete
            taskB.openBarrier();
            taskB.assertSuccess();
        }
    }

    /**
     * Test that retries do not occur when BulkheadException is not included in the retryOn attribute
     */
    @Test
    public void testNoRetriesWithoutRetryOn() {
        testNoRetries(barrier -> retry1DelayClassBean.test(barrier, null));
    }

    /**
     * Test that retries do not occur when BulkheadException is included in the abortOn attribute
     */
    @Test
    public void testNoRetriesWithAbortOn() {
        testNoRetries(retryAbortOnBean::test);
    }

    /**
     * Test that with maxRetries = 0, bulkhead exceptions are not retried
     */
    @Test
    public void testNoRetriesWithMaxRetriesZero() {
        testNoRetries(zeroRetryBean::test);
    }

    /**
     * Tests that a Bulkhead + Retry configuration does not retry on BulkheadException
     * <p>
     * The test method should be configured with {@code Bulkhead(1)} and a 1 second delay on Retry.
     * 
     * @param testMethod
     *            the test method
     */
    private static void testNoRetries(Consumer<Barrier> testMethod) {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            // Start a task to fill the bulkhead
            BarrierTask<Void> taskA = taskManager.runBarrierTask(testMethod);
            taskA.assertAwaits();

            // Run a second task which should fail with a BulkheadException
            // Measure how long it takes to tell whether it retried
            long startTime = System.nanoTime();
            BarrierTask<Void> taskB = taskManager.runBarrierTask(testMethod);
            taskB.assertThrows(BulkheadException.class);
            long endTime = System.nanoTime();

            assertThat("Task took to long to return, may have done retries",
                    Duration.ofNanos(endTime - startTime), lessThan(TCKConfig.getConfig().getTimeoutInDuration(800)));
        }
    }

}
