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

import static org.awaitility.Awaitility.await;
import static org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper.toCompletableFuture;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectBulkheadException;
import static org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig.getConfig;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadCompletionStageBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
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
 * @author Gordon Hutchison
 * @author Andrew Rouse
 * @author carlosdlr
 */

public class BulkheadAsynchTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only work on business methods of injected objects we need to inject a variety of
     * these for use by the tests below. The naming convention indicates if the annotation is on a class or method,
     * asynchronous or semaphore based, the size/value of the {@code @Bulkhead} and whether we have queueing or not.
     */

    @Inject
    private BulkheadClassAsynchronousDefaultBean bhBeanClassAsynchronousDefault;
    @Inject
    private BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;
    @Inject
    private Bulkhead3ClassAsynchronousBean bhBeanClassAsynchronous3;
    @Inject
    private Bulkhead3MethodAsynchronousBean bhBeanMethodAsynchronous3;
    @Inject
    private Bulkhead10ClassAsynchronousBean bhBeanClassAsynchronous10;
    @Inject
    private Bulkhead10MethodAsynchronousBean bhBeanMethodAsynchronous10;
    @Inject
    private BulkheadClassAsynchronousQueueingBean bhBeanClassAsynchronousQueueing;
    @Inject
    private BulkheadMethodAsynchronousQueueingBean bhBeanMethodAsynchronousQueueing;
    @Inject
    private BulkheadCompletionStageBean bhBeanCompletionStage;

    /**
     * This is the Arquillian deploy method that controls the contents of the war that contains all the tests.
     *
     * @return the test war "ftBulkheadAsynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadAsynchTest.jar")
                .addPackage(BulkheadClassAsynchronousDefaultBean.class.getPackage())
                .addClass(CompletableFutureHelper.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        return ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchTest.war").addAsLibrary(testJar);
    }

    /**
     * Tests the class asynchronous Bulkhead(10) This test will check that 10 and no more than 10 asynchronous calls are
     * allowed into a method that is a member of a {@code @Bulkhead(10)} Class.
     */
    @Test
    public void testBulkheadClassAsynchronous10() {
        testBulkhead(10, 10, bhBeanClassAsynchronous10::test);
    }

    /**
     * Tests the method asynchronous Bulkhead(10). This test will check that 10 and no more than 10 asynchronous calls
     * are allowed into a method that has an individual {@code @Bulkhead(10)} annotation
     */
    @Test
    public void testBulkheadMethodAsynchronous10() {
        testBulkhead(10, 10, bhBeanMethodAsynchronous10::test);
    }

    /**
     * Tests the class asynchronous Bulkhead(3) This test will check that 3 and no more than 3 asynchronous calls are
     * allowed into a method that is a member of a {@code @Bulkhead(3)} Class.
     */
    @Test
    public void testBulkheadClassAsynchronous3() {
        testBulkhead(3, 10, bhBeanClassAsynchronous3::test);
    }

    /**
     * Tests the method asynchronous Bulkhead(3). This test will check that 3 and no more than 3 asynchronous calls are
     * allowed into a method that has an individual Bulkhead(3) annotation
     */
    @Test
    public void testBulkheadMethodAsynchronous3() {
        testBulkhead(3, 10, bhBeanMethodAsynchronous3::test);
    }

    /**
     * Tests the basic class asynchronous Bulkhead with defaulting value parameter. This will check that exactly 10
     * calls can be in the bulkhead at once.
     */
    @Test
    public void testBulkheadClassAsynchronousDefault() {
        testBulkhead(10, 10, bhBeanClassAsynchronousDefault::test);
    }

    /**
     * Tests the basic method asynchronous Bulkhead with defaulting value parameter. This will check that more than 1
     * but less than 10 calls get into the bulkhead at once.
     */
    @Test
    public void testBulkheadMethodAsynchronousDefault() {
        testBulkhead(10, 10, bhBeanMethodAsynchronousDefault::test);
    }

    /**
     * Tests the queueing class asynchronous Bulkhead with value parameter 10. This will check that more than 1 but less
     * than 5 calls get into the bulkhead at once but that 5 threads can queue to get into the bulkhead
     */
    @Test
    public void testBulkheadClassAsynchronousQueueing5() {
        testBulkhead(5, 5, bhBeanClassAsynchronousQueueing::test);
    }

    /**
     * Tests the queueing method asynchronous Bulkhead with value parameter 10. This will check that more than 1 but
     * less than 5 calls get into the bulkhead at once but that 5 threads can queue to get into the bulkhead
     */
    @Test
    public void testBulkheadMethodAsynchronousQueueing5() {
        testBulkhead(5, 5, bhBeanMethodAsynchronousQueueing::test);
    }

    /**
     * Test that an asynchronous method which returns an incomplete CompletionStage still reserves a slot in the
     * bulkhead
     */
    @Test
    public void testBulkheadCompletionStage() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            Future<Void> future1 = toCompletableFuture(bhBeanCompletionStage.serviceCS(result));
            Future<Void> future2 = toCompletableFuture(bhBeanCompletionStage.serviceCS(result));
            Thread.sleep(getConfig().getTimeoutInMillis(200)); // Give tasks a chance to start and run
            Future<Void> future3 = toCompletableFuture(bhBeanCompletionStage.serviceCS(result));
            Future<Void> future4 = toCompletableFuture(bhBeanCompletionStage.serviceCS(result));
            Thread.sleep(getConfig().getTimeoutInMillis(200)); // Give tasks a chance to start and run
            Future<Void> future5 = toCompletableFuture(bhBeanCompletionStage.serviceCS(result));

            // Although futures 1 & 2 have had time to run, they shouldn't be finished because the completion stage
            // returned is not complete
            assertFalse(future1.isDone(), "Future1 reported done");
            assertFalse(future2.isDone(), "Future2 reported done");

            // Because futures 1 & 2 are still not "complete" (even though the method call may have returned), future 5
            // should be rejected
            expectBulkheadException(future5);

            // Complete the CompletionStage which was returned
            result.complete(null);

            // After the CompletionStage completes, future 1 & 2 should complete
            future1.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
            future2.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);

            // Tasks 3 & 4 should then be removed from the queue, started and complete almost immediately
            future3.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
            future4.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
        } finally {
            result.complete(null);
        }
    }

    /**
     * Conducts a standard test to ensure that an asynchronous bulkhead with no other annotations works correctly. It
     * asserts that the correct number of tasks are allowed to run and to queue and that when a task in the bulkhead
     * completes a new task can be run.
     * <p>
     * The {@code bulkheadMethod} should be a reference to a method annotated with {@link Bulkhead} and
     * {@link Asynchronous} which accepts a {@code Barrier} and calls {@link Barrier#await()}.
     * 
     * @param maxRunning
     *            expected number of tasks permitted to run
     * @param maxQueued
     *            expected number of tasks permitted to queue
     * @param bulkheadMethod
     *            a reference to the annotated method
     */
    public static void testBulkhead(int maxRunning, int maxQueued, Function<Barrier, Future<?>> bulkheadMethod) {

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

            // Check next task is rejected
            BarrierTask<?> overflowTask = taskManager.runAsyncBarrierTask(bulkheadMethod);
            overflowTask.assertThrows(BulkheadException.class);

            // Release one running task
            BarrierTask<?> releasedTask = runningTasks.get(7 % maxRunning); // Pick one out of the middle
            releasedTask.openBarrier();
            releasedTask.assertSuccess();

            // Check that one of the queued tasks now starts
            await().until(() -> queuedTasks.stream()
                    .filter(task -> task.isAwaiting())
                    .count() == 1);

            // Now check that another task can be submitted and queues
            BarrierTask<?> extraTask = taskManager.runAsyncBarrierTask(bulkheadMethod);
            extraTask.assertNotAwaiting();

            // Now check that next task is rejected
            BarrierTask<?> overflowTask2 = taskManager.runAsyncBarrierTask(bulkheadMethod);
            overflowTask2.assertThrows(BulkheadException.class);
        }
    }
}
