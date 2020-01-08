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

import static org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils.log;
import static org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper.toCompletableFuture;
import static org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils.handleResults;
import static org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AbstractBulkheadTask.assertAllNotStarting;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadCompletionStageBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectBulkheadException;
import static org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig.getConfig;
import static org.testng.Assert.fail;

/**
 * @author Gordon Hutchison
 * @author Andrew Rouse
 * @author carlosdlr
 */

public class BulkheadAsynchTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the {@code @Bulkhead} and whether we have queueing or not.
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
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     *
     * @return the test war "ftBulkheadAsynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadAsynchTest.jar")
                .addPackage(BulkheadClassAsynchronousDefaultBean.class.getPackage())
                .addClass(Utils.class)
                .addClass(CompletableFutureHelper.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        return ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchTest.war").addAsLibrary(testJar);
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        log("Testmethod: " + testContext.getName());
    }

    /**
     * Tests the class asynchronous Bulkhead(10) This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that is
     * a member of a {@code @Bulkhead(10)} Class.
     */
    @Test
    public void testBulkheadClassAsynchronous10() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanClassAsynchronous10, 10, td);
        td.check();
    }

    /**
     * Tests the method asynchronous Bulkhead(10). This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that has
     * an individual {@code @Bulkhead(10)} annotation
     */
    @Test
    public void testBulkheadMethodAsynchronous10() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanMethodAsynchronous10, 10, td);
        td.check();
    }

    /**
     * Tests the class asynchronous Bulkhead(3) This test will check that 3 and
     * no more than 3 asynchronous calls are allowed into a method that is a
     * member of a {@code @Bulkhead(3)} Class.
     */
    @Test
    public void testBulkheadClassAsynchronous3() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanClassAsynchronous3, 3, td);
        td.check();
    }

    /**
     * Tests the method asynchronous Bulkhead(3). This test will check that 3
     * and no more than 3 asynchronous calls are allowed into a method that has
     * an individual Bulkhead(3) annotation
     */
    @Test
    public void testBulkheadMethodAsynchronous3() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanMethodAsynchronous3, 3, td);
        td.check();
    }

    /**
     * Tests the basic class asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test
    public void testBulkheadClassAsynchronousDefault() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanClassAsynchronousDefault, 10, td);
        td.check();
    }

    /**
     * Tests the basic method asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test
    public void testBulkheadMethodAsynchronousDefault() {
        TestData td = new TestData(new CountDownLatch(10));
        loop(10, bhBeanMethodAsynchronousDefault, 10, td);
        td.check();
    }

    /**
     * Tests the queueing class asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test
    public void testBulkheadClassAsynchronousQueueing10() {
        TestData td = new TestData(new CountDownLatch(20));
        loop(20, bhBeanClassAsynchronousQueueing, 10, 20, td);
        td.check();
    }

    /**
     * Tests the queueing method asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test
    public void testBulkheadMethodAsynchronousQueueing10() {
        TestData td = new TestData(new CountDownLatch(20));
        loop(20, bhBeanMethodAsynchronousQueueing, 10, 20, td);
        td.check();
    }

    /**
     * Test that when the bulkhead is full, a BulkheadException is thrown
     *
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testBulkheadExceptionThrownWhenQueueFullAsync() throws InterruptedException {
        List<AsyncBulkheadTask> tasks = new ArrayList<>();

        try {
            // Fill the bulkhead
            for (int i = 0; i < 10; i++) {
                AsyncBulkheadTask task = new AsyncBulkheadTask();
                tasks.add(task);
                Future<?> result = bhBeanClassAsynchronousDefault.test(task);
                task.assertStarting(result);
            }

            // Fill the queue
            List<AsyncBulkheadTask> queuingTasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                AsyncBulkheadTask task = new AsyncBulkheadTask();
                tasks.add(task);
                queuingTasks.add(task);
                bhBeanClassAsynchronousDefault.test(task);
            }
            // Queued tasks should not start
            assertAllNotStarting(queuingTasks);

            // Try to run one more (should get a bulkhead exception)
            AsyncBulkheadTask task = new AsyncBulkheadTask();
            tasks.add(task);
            Future<?> result = bhBeanClassAsynchronousDefault.test(task);
            task.assertNotStarting();

            assertTrue(result.isDone(), "When a task is rejected from the bulkhead, the returned future should report as done");
            expect(BulkheadException.class, result);
        }
        finally {
            for (AsyncBulkheadTask task : tasks) {
                task.complete();
            }
        }
    }

    /**
     * Test that an asynchronous method which returns an incomplete CompletionStage still reserves a slot in the bulkhead
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
            
            // Although futures 1 & 2 have had time to run, they shouldn't be finished because the completion stage returned is not complete
            assertFalse(future1.isDone(), "Future1 reported done");
            assertFalse(future2.isDone(), "Future2 reported done");
            
            // Because futures 1 & 2 are still not "complete" (even though the method call may have returned), future 5 should be rejected
            expectBulkheadException(future5);
            
            // Complete the CompletionStage which was returned
            result.complete(null);
            
            // After the CompletionStage completes, future 1 & 2 should complete
            future1.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
            future2.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
            
            // Tasks 3 & 4 should then be removed from the queue, started and complete almost immediately
            future3.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
            future4.get(getConfig().getTimeoutInMillis(1000), TimeUnit.MILLISECONDS);
        }
        finally {
            result.complete(null);
        }
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread.
     * Here we do not check that amount that were successfully through the Bulkhead
     * @param loops number of loops to simulate
     * @param test bulkhead component to test
     * @param maxSimultaneousWorkers max number of simultaneous workers
     * @param td testData component to simulate the execution
     */
    private void loop(int loops, BulkheadTestBackend test, int maxSimultaneousWorkers, TestData td) {
        td.setExpectedMaxSimultaneousWorkers(maxSimultaneousWorkers);
        td.setExpectedInstances(loops);
        td.setExpectedTasksScheduled(loops);

        Future[] results = new Future[loops];
        for (int i = 0; i < loops; i++) {
            log("synchronous loop() starting test " + i);
            try {
                results[i] = test.test(new Checker(5 * 1000, td));
            }
            catch (InterruptedException e1) {
                fail("Unexpected interruption", e1);
            }
        }
        handleResults(loops, results);
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread
     * @param number expected instances
     * @param test bulkhead component to test
     * @param maxSimultaneousWorkers max number of simultaneous workers
     * @param expectedTasksScheduled number of expected tasks
     * @param td testData component to simulate the execution
     */
    private void loop(int number, BulkheadTestBackend test, int maxSimultaneousWorkers, int expectedTasksScheduled,
            TestData td) {
        td.setExpectedTasksScheduled(expectedTasksScheduled);
        loop(number, test, maxSimultaneousWorkers, td);
    }
}
