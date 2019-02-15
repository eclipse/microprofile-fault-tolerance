/*
 *******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectBulkheadException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BackendTestDelegate;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55ClassAsynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55MethodAsynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55RapidRetry10ClassAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55RapidRetry10MethodAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryAbortOnAsyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryDelayAsyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRetryQueueAsyncBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * This collection of tests tests that failures, particularly Asynchronous
 * Bulkhead exception related failures will cause the Retry annotation logic to
 * work correctly.
 *
 * @author Gordon Hutchison
 *
 */
public class BulkheadAsynchRetryTest extends Arquillian {


    /*
     * As the FaultTolerance annotation only works on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */

    private static final int DONT_CHECK = 0;

    @Inject
    private Bulkhead55MethodAsynchronousRetryBean methodBean;
    @Inject
    private Bulkhead55ClassAsynchronousRetryBean classBean;

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
    
    private List<AsyncBulkheadTask> tasks = new ArrayList<>();
    
    private AsyncBulkheadTask newTask() {
        AsyncBulkheadTask task = new AsyncBulkheadTask();
        tasks.add(task);
        return task;
    }
    
    @AfterMethod
    public void cleanupTasks() {
        for (AsyncBulkheadTask task : tasks) {
            task.complete();
        }
    }

    /**
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     *
     * @return the test war "ftBulkheadAsynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadAsynchRetryTest.jar")
                .addPackage(Bulkhead55ClassAsynchronousRetryBean.class.getPackage()).addClass(Utils.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchRetryTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmethod: " + testContext.getName());
    }

    /**
     * Test no regression due to passive Retry. The Bulkhead is 5, but also has
     * a queue of 5, so the Retry should not come into effect for 10 tasks
     */
    @Test()
    public void testBulkheadClassAsynchronousPassiveRetry55() {
        int iterations = 10;
        int expectedTasksScheduled = iterations;
        TestData td = new TestData(new CountDownLatch(expectedTasksScheduled));
        Future[] results = Utils.loop(iterations, classBean, 5, expectedTasksScheduled, td);
        Utils.handleResults(iterations, results);
        td.check();
    }

    /**
     * Test that Retry can be used to prevent receiving Bulkhead exceptions from
     * a method level test. The retries are delayed by one second and will be
     * done 10 times so this gives 10 seconds of retrying, which is enough to
     * get the calls through the bulkhead.
     */
    @Test()
    public void testBulkheadMethodAsynchronousRetry55() {
        int iterations = 20;
        int expectedTasksScheduled = iterations;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData(new CountDownLatch(expectedTasksScheduled));
        Future[] results = Utils.loop(iterations, methodBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
        Utils.handleResults(iterations, results);
        td.check();
    }

    /**
     * Test that we still get BulkheadExceptions despite using Retry if the bulkhead
     * remains full while the retry is active.
     *
     * @throws InterruptedException when interrupted
     */
    @Test()
    public void testBulkheadMethodAsynchronousRetry55Trip() throws InterruptedException {
        List<AsyncBulkheadTask> tasks = new ArrayList<>();
        List<Future<?>> results = new ArrayList<>();
        
        // Start 5 tasks which should begin running
        for (int i = 0; i < 5; i++) {
            AsyncBulkheadTask task = newTask();
            tasks.add(task);
            Future<?> result = rrClassBean.test(task);
            results.add(result);
            task.assertStarting(result);
        }
        
        // Start another 5 tasks which should queue
        Collection<AsyncBulkheadTask> nonStartingTasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AsyncBulkheadTask task = newTask();
            tasks.add(task);
            nonStartingTasks.add(task);
            results.add(rrClassBean.test(task));
        }
        AsyncBulkheadTask.assertAllNotStarting(nonStartingTasks);
        
        // Start one more task, which should fail
        AsyncBulkheadTask testTask = newTask();
        Future<?> testResult = rrClassBean.test(testTask);
        
        testTask.assertNotStarting();
        expect(BulkheadException.class, testResult);
        
        // Cleanup
        for (AsyncBulkheadTask task : tasks) {
            task.complete();
        }
    }

    /**
     * Tests overloading the Retries by firing lots of work at a full Method
     * bulkhead
     *
     * @throws InterruptedException when interrupted
     */
    @Test()
    public void testBulkheadMethodAsynchronous55RetryOverload() throws InterruptedException {
        int iterations = 1000;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData();
        
        Future[] results = Utils.loop(iterations, rrMethodBean, maxSimultaneousWorkers, 0, td);
        
        int failures = 0;
        for (Future result : results) {
            try {
                result.get();
            }
            catch (ExecutionException e) {
                if (e.getCause() instanceof BulkheadException) {
                    failures++;
                }
                else {
                    Assert.fail("Unexpected non-bulkhead exception thrown", e);
                }
            }
        }
        
        MatcherAssert.assertThat("Failure count should be non-zero", failures, greaterThan(0));
    }

    /**
     *
     * Tests overloading the Retries by firing lots of work at a full Class
     * bulkhead
     * @throws InterruptedException when interrupted 
     */
    @Test()
    public void testBulkheadClassAsynchronous55RetryOverload() throws InterruptedException {
        int iterations = 1000;
        int expectedTasksScheduled = iterations;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData();
        
        Future[] results = Utils.loop(iterations, rrClassBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
        
        int failures = 0;
        for (Future result : results) {
            try {
                result.get();
            }
            catch (ExecutionException e) {
                if (e.getCause() instanceof BulkheadException) {
                    failures++;
                }
                else {
                    Assert.fail("Unexpected non-bulkhead exception thrown", e);
                }
            }
        }
        
        MatcherAssert.assertThat("Failure count should be non-zero", failures, greaterThan(0));
    }

    /**
     * Test no regression due to passive Retry. The Bulkhead is 5, but has a
     * queue of 5, so the Retry should not come into effect for 10 Tasks.
     */
    @Test()
    public void testBulkheadPassiveRetryMethodAsynchronous55() {
        int iterations = 10;
        int expectedTasksScheduled = iterations;
        TestData td = new TestData(new CountDownLatch(expectedTasksScheduled));
        Future[] results = Utils.loop(iterations, methodBean, 5, expectedTasksScheduled, td);
        Utils.handleResults(iterations, results);
        td.check();
    }

    /**
     * Test that Retry can be used to prevent receiving Bulkhead exceptions from
     * a method level test. There is enough retrying in the Bean to cover the
     * queue overflow.
     */
    @Test()
    public void testBulkheadRetryClassAsynchronous55() {
        int iterations = 20;
        int expectedTasksScheduled = iterations;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData(new CountDownLatch(expectedTasksScheduled));
        Future[] results = Utils.loop(iterations, classBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
        Utils.handleResults(iterations, results);
        td.check();
    }

    /**
     * Check we do not loose anything from the queue due to exceptions covered
     * by Retry at the class level. The Checker backends will throw an exception
     * the first time their 'perform' method is called but will run OK when
     * retried.
     * 
     * @throws InterruptedException if the test is interrupted
     */
    @Test()
    public void testBulkheadQueReplacesDueToClassRetryFailures() throws InterruptedException {
        int threads = 10;
        int maxSimultaneousWorkers = 5;
        Future[] results = new Future[threads];

        TestData td = new TestData(new CountDownLatch(threads));
        td.setExpectedInstances(threads);
        // As we are causing workers to get 'blown up' we cannot know that we
        // get
        // a full set at once, so we switch off the test that checks that the
        // bulkhead 'filled up'. We will still check we don't get more than the
        // bulkhead
        // at one time.
        td.setExpectedMaxSimultaneousWorkers(maxSimultaneousWorkers);
        td.setMaxFill(false);
        td.setExpectedTasksScheduled(DONT_CHECK);

        for (int i = 0; i < threads; i++) {
            Utils.log("Starting test " + i);
            BackendTestDelegate failOnce = new Checker(100, td, 1);
            results[i] = rrClassBean.test(failOnce);
        }

        Utils.handleResults(threads, results);
        td.check();
    }
    
    /**
     * Test that when an execution is retried, it doesn't hold onto its bulkhead slot.
     * <p>
     * This is particularly important if Retry is used with a long delay.
     * 
     * @throws InterruptedException if the test is interrupted
     * @throws TimeoutException if we time out waiting for a result
     * @throws ExecutionException if an asynchronous call threw an exception
     */
    @Test
    public void testRetriesReenterBulkhead() throws InterruptedException, ExecutionException, TimeoutException {
        // Start taskA
        AsyncBulkheadTask taskA = newTask();
        Future resultA = retryDelayAsyncBean.test(taskA);
        
        taskA.assertStarting(resultA);
        
        // Now, start taskB
        AsyncBulkheadTask taskB = newTask();
        Future resultB = retryDelayAsyncBean.test(taskB);
        
        // Cause taskA to fail, prompting a retry after 1 second
        taskA.completeExceptionally(new TestException());
        
        // Task B should now start because the bulkhead is empty while taskA waits to retry
        taskB.assertStarting(resultB);
        
        // Start taskC
        AsyncBulkheadTask taskC = newTask();
        Future resultC = retryDelayAsyncBean.test(taskC);
        
        // Task C should wait in the queue behind task B
        taskC.assertNotStarting();
        
        // Now, when taskA retries, it should complete with a BulkheadException because the bulkhead is full because
        // taskB is running and taskC is queued
        expectBulkheadException(resultA);
        
        // Now let taskB complete
        taskB.complete(CompletableFuture.completedFuture("OK"));
        Assert.assertEquals(resultB.get(1, TimeUnit.MINUTES), "OK", "taskB should be complete");
        
        // Now let taskC complete
        taskC.complete(CompletableFuture.completedFuture("OK"));
        Assert.assertEquals(resultC.get(1, TimeUnit.MINUTES), "OK", "taskC should be complete");
    }
    
    /**
     * Test that when an execution is retried, it goes to the back of the bulkhead queue.
     * 
     * @throws InterruptedException if the test is interrupted
     * @throws TimeoutException if we time out waiting for a result
     * @throws ExecutionException if an asynchronous call threw an exception
     */
    @Test
    public void testRetriesJoinBackOfQueue() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncBulkheadTask taskA = newTask();
        Future resultA = retryQueueAsyncBean.test(taskA);
        taskA.assertStarting(resultA);
        
        AsyncBulkheadTask taskB = newTask();
        Future resultB = retryQueueAsyncBean.test(taskB);
        taskB.assertNotStarting();
        
        AsyncBulkheadTask taskC = newTask();
        Future resultC = retryQueueAsyncBean.test(taskC);
        taskC.assertNotStarting();
        
        taskA.completeExceptionally(new TestException()); // fail A, causes instant retry which puts taskA on the back of the queue
        
        taskB.assertStarting(resultB);
        taskC.assertNotStarting();
        Assert.assertFalse(resultA.isDone(), "Result A should not be complete yet"); // Check A is not finished, it should be back on the queue
        
        taskB.complete(CompletableFuture.completedFuture("OK")); // Let B complete
        
        Assert.assertEquals(resultB.get(2, SECONDS), "OK", "ResultB should be complete");
        taskC.assertStarting(resultC);
        Assert.assertFalse(resultA.isDone(), "Result A should still not be complete");
        
        taskC.complete(CompletableFuture.completedFuture("OK")); // Let C complete
        Assert.assertEquals(resultC.get(2, SECONDS), "OK", "ResultC should be complete");
        
        // Now that there are no more tasks, A should quickly finish its retry and throw an exception
        expect(TestException.class, resultA);
    }
    
    /**
     * Test that retries do not occur when BulkheadException is not included in the retryOn attribute
     * 
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testNoRetriesWithoutRetryOn() throws InterruptedException {
        // Start taskA, which should run
        AsyncBulkheadTask taskA = newTask();
        retryDelayAsyncBean.test(taskA);
        taskA.assertStarting();
        
        // Start taskB, which should queue
        AsyncBulkheadTask taskB = newTask();
        retryDelayAsyncBean.test(taskB);
        taskB.assertNotStarting();
        
        // Now, start taskC, which should quickly fail because there are no retries
        long startTime = System.nanoTime();
        AsyncBulkheadTask taskC = newTask();
        Future<?> resultC = retryDelayAsyncBean.test(taskC);
        expectBulkheadException(resultC);
        long endTime = System.nanoTime();
        
        assertThat("Task took to long to return, may have done retries", Duration.ofNanos(endTime - startTime), lessThan(Duration.ofMillis(250)));
    }
    
    /**
     * Test that retries do not occur when BulkheadException is included in the abortOn attribute
     * 
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testNoRetriesWithAbortOn() throws InterruptedException {
        // Start taskA, which should run
        AsyncBulkheadTask taskA = newTask();
        retryAbortOnAsyncBean.test(taskA);
        taskA.assertStarting();
        
        // Start taskB, which should queue
        AsyncBulkheadTask taskB = newTask();
        retryAbortOnAsyncBean.test(taskB);
        taskB.assertNotStarting();
        
        // Now, start taskC, which should quickly fail because there are no retries
        long startTime = System.nanoTime();
        AsyncBulkheadTask taskC = newTask();
        Future<?> resultC = retryAbortOnAsyncBean.test(taskC);
        expectBulkheadException(resultC);
        long endTime = System.nanoTime();
        
        assertThat("Task took to long to return, may have done retries", Duration.ofNanos(endTime - startTime), lessThan(Duration.ofMillis(250)));
    }




}