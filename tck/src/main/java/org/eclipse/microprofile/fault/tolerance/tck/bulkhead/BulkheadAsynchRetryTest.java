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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead33RetryManyAsyncClassBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead33RetryManyAsyncMethodBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55RapidRetry10ClassAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55RapidRetry10MethodAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryAbortOnAsyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryDelayAsyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryQueueAsyncBean;
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
 * This collection of tests tests that failures, particularly Asynchronous Bulkhead exception related failures will
 * cause the Retry annotation logic to work correctly.
 *
 * @author Gordon Hutchison
 * @author carlosdlr
 * @author Andrew Rouse
 *
 */
public class BulkheadAsynchRetryTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only works on business methods of injected objects we need to inject a variety
     * of these for use by the tests below. The naming convention indicates if the annotation is on a class or method,
     * asynchronous or semaphore based, the size/value of the @Bulkhead and whether we have queuing or not.
     */

    @Inject
    private Bulkhead33RetryManyAsyncClassBean retryManyClassBean;

    @Inject
    private Bulkhead33RetryManyAsyncMethodBean retryManyMethodBean;

    @Inject
    private Bulkhead55RapidRetry10ClassAsynchBean rrClassBean;

    @Inject
    private Bulkhead55RapidRetry10MethodAsynchBean rrMethodBean;

    @Inject
    private BulkheadRetryDelayAsyncBean retryDelayAsyncBean;

    @Inject
    private BulkheadRetryQueueAsyncBean retryQueueAsyncBean;

    @Inject
    private BulkheadRetryAbortOnAsyncBean retryAbortOnAsyncBean;

    /**
     * This is the Arquillian deploy method that controls the contents of the war that contains all the tests.
     *
     * @return the test war "ftBulkheadAsynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {

        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .autoscaleClass(Bulkhead33RetryManyAsyncClassBean.class)
                .autoscaleMethod(Bulkhead33RetryManyAsyncMethodBean.class, "test")
                .autoscaleClass(BulkheadRetryDelayAsyncBean.class)
                .autoscaleClass(BulkheadRetryQueueAsyncBean.class)
                .autoscaleClass(BulkheadRetryAbortOnAsyncBean.class);

        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadAsynchRetryTest.jar")
                .addClass(BulkheadAsynchTest.class)
                .addPackage(Bulkhead33RetryManyAsyncClassBean.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        return ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchRetryTest.war")
                .addAsLibrary(testJar);
    }

    /**
     * Test that we still get BulkheadExceptions despite using Retry if the bulkhead remains full while the retry is
     * active.
     */
    @Test
    public void testBulkheadExceptionThrownClassAsync() {
        // Since the bulkhead remains full while retries occur and the bean is configured
        // to retry very quickly, we can do the same test as for a bulkhead without retry
        BulkheadAsynchTest.testBulkhead(5, 5, rrClassBean::test);
    }

    /**
     * Test that we still get BulkheadExceptions despite using Retry if the bulkhead remains full while the retry is
     * active.
     */
    @Test
    public void testBulkheadExceptionThrownMethodAsync() {
        // Since the bulkhead remains full while retries occur and the bean is configured
        // to retry very quickly, we can do the same test as for a bulkhead without retry
        BulkheadAsynchTest.testBulkhead(5, 5, rrMethodBean::test);
    }

    /**
     * Test that bulkhead exceptions are retried with annotations on method
     */
    @Test
    public void testBulkheadExceptionRetriedMethodAsync() {
        testBulkheadExceptionRetried(3, 3, retryManyMethodBean::test);
    }

    /**
     * Test that bulkhead exceptions are retried with annotations on class
     */
    @Test
    public void testBulkheadExceptionRetriedClassAsync() {
        testBulkheadExceptionRetried(3, 3, retryManyClassBean::test);
    }

    /**
     * Test that bulkhead exceptions are retried
     * <p>
     * This test fills the bulkhead and the queue, tries to run an additional task, empties the bulkhead and the queue
     * and checks that the additional task runs.
     * 
     * @param maxRunning
     *            bulkhead size
     * @param maxQueued
     *            bulkhead queue size
     * @param bulkheadMethod
     *            reference to the method annotated with Bulkhead
     */
    private static void testBulkheadExceptionRetried(int maxRunning, int maxQueued,
            Function<Barrier, Future<?>> bulkheadMethod) {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {

            // Fill the bulkhead
            List<BarrierTask<?>> runningTasks = new ArrayList<>();
            for (int i = 0; i < maxRunning; i++) {
                BarrierTask<?> task = taskManager.runAsyncBarrierTask(bulkheadMethod);
                runningTasks.add(task);
            }

            // Check tasks start and await on the barrier
            for (int i = 0; i < maxRunning; i++) {
                runningTasks.get(i).assertAwaits();
            }

            // Fill the queue
            List<BarrierTask<?>> queuedTasks = new ArrayList<>();
            for (int i = 0; i < maxQueued; i++) {
                BarrierTask<?> task = taskManager.runAsyncBarrierTask(bulkheadMethod);
                queuedTasks.add(task);
            }

            // Check queued tasks do not start and await on the barrier
            AsyncTaskManager.assertAllNotAwaiting(queuedTasks);

            // Next task would overflow the queue and fail with BulkheadException but should be retried
            BarrierTask<?> overflowTask = taskManager.runAsyncBarrierTask(bulkheadMethod);
            overflowTask.assertNotAwaiting();

            // Release all running and queued tasks and check that the overflow task starts
            runningTasks.forEach(BarrierTask::openBarrier);
            queuedTasks.forEach(BarrierTask::openBarrier);
            overflowTask.assertAwaits();

            overflowTask.openBarrier();
            overflowTask.assertCompletes();
        }
    }

    /**
     * Test that when an execution is retried, it doesn't hold onto its bulkhead slot.
     * <p>
     * This is particularly important if Retry is used with a long delay.
     */
    @Test
    public void testRetriesReenterBulkhead() {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> taskA =
                    taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, new TestException()));
            taskA.assertAwaits();

            BarrierTask<?> taskB = taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, null));
            taskB.assertNotAwaiting();

            taskA.openBarrier();
            taskB.assertAwaits();

            BarrierTask<?> taskC = taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, null));
            taskC.assertNotAwaiting();

            taskA.assertThrows(BulkheadException.class);

            taskB.openBarrier();
            taskB.assertSuccess();

            taskC.openBarrier();
            taskC.assertSuccess();
        }
    }

    /**
     * Test that when an execution is retried, it goes to the back of the bulkhead queue.
     */
    @Test
    public void testRetriesJoinBackOfQueue() {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> taskA =
                    taskManager.runAsyncBarrierTask(barrier -> retryQueueAsyncBean.test(barrier, new TestException()));
            taskA.assertAwaits();

            BarrierTask<?> taskB = taskManager.runAsyncBarrierTask(barrier -> retryQueueAsyncBean.test(barrier, null));
            taskB.assertNotAwaiting();

            BarrierTask<?> taskC = taskManager.runAsyncBarrierTask(barrier -> retryQueueAsyncBean.test(barrier, null));
            taskC.assertNotAwaiting();

            taskA.openBarrier(); // fail A, causes instant retry which puts taskA on the back of the queue

            taskB.assertAwaits();
            taskC.assertNotAwaiting();
            taskA.assertNotCompleting(); // Check A is not finished, it should be back on the queue

            taskB.openBarrier();
            taskB.assertSuccess(); // B should complete
            taskC.assertAwaits(); // C should run
            taskA.assertNotCompleting(); // A should still be in the queue

            taskC.openBarrier();
            taskC.assertSuccess(); // C should now finish
            taskA.assertThrows(TestException.class); // A should run and quickly finish since it has already been
                                                     // released
        }
    }

    /**
     * Test that retries do not occur when BulkheadException is not included in the retryOn attribute
     *
     * @throws InterruptedException
     *             if the test is interrupted
     */
    @Test
    public void testNoRetriesWithoutRetryOn() throws InterruptedException {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> taskA = taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, null));
            taskA.assertAwaits();

            BarrierTask<?> taskB = taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, null));
            taskB.assertNotAwaiting();

            long startTime = System.nanoTime();
            BarrierTask<?> taskC = taskManager.runAsyncBarrierTask(barrier -> retryDelayAsyncBean.test(barrier, null));
            taskC.assertThrows(BulkheadException.class);
            long endTime = System.nanoTime();

            assertThat("Task took to long to return, may have done retries",
                    Duration.ofNanos(endTime - startTime), lessThan(TCKConfig.getConfig().getTimeoutInDuration(800)));
        }
    }

    /**
     * Test that retries do not occur when BulkheadException is included in the abortOn attribute
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     */
    @Test
    public void testNoRetriesWithAbortOn() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> taskA = taskManager.runAsyncBarrierTask(retryAbortOnAsyncBean::test);
            taskA.assertAwaits();

            BarrierTask<?> taskB = taskManager.runAsyncBarrierTask(retryAbortOnAsyncBean::test);
            taskB.assertNotAwaiting();

            long startTime = System.nanoTime();
            BarrierTask<?> taskC = taskManager.runAsyncBarrierTask(retryAbortOnAsyncBean::test);
            taskC.assertThrows(BulkheadException.class);
            long endTime = System.nanoTime();

            assertThat("Task took to long to return, may have done retries",
                    Duration.ofNanos(endTime - startTime), lessThan(TCKConfig.getConfig().getTimeoutInDuration(800)));
        }
    }
}
