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

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AbstractBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3MethodAsynchronousBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 * @author Andrew Rouse
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
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmethod: " + testContext.getName());
    }

    /**
     * Tests the class asynchronous Bulkhead(10) This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that is
     * a member of a {@code @Bulkhead(10)} Class.
     */
    @Test()
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
    @Test()
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
    @Test()
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
    @Test()
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
    @Test()
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
    @Test()
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
    @Test()
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
    @Test()
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
            AbstractBulkheadTask.assertAllNotStarting(queuingTasks);
            
            // Try to run one more (should get a bulkhead exception)
            AsyncBulkheadTask task = new AsyncBulkheadTask();
            tasks.add(task);
            Future<?> result = bhBeanClassAsynchronousDefault.test(task);
            task.assertNotStarting();
            
            assertTrue(result.isDone(), "When a task is rejected from the bulkhead, the returned future should report as done");
            Exceptions.expect(BulkheadException.class, result);
        }
        finally {
            for (AsyncBulkheadTask task : tasks) {
                task.complete();
            }
        }
    }
    
    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread.
     * Here we do not check that amount that were successfully through the
     * Bulkhead
     * 
     * @param loops
     * @param test
     * @param maxSimultaneousWorkers
     * @param td
     */
    private void loop(int loops, BulkheadTestBackend test, int maxSimultaneousWorkers, TestData td) {

        td.setExpectedMaxSimultaneousWorkers(maxSimultaneousWorkers);
        td.setExpectedInstances(loops);
        td.setExpectedTasksScheduled(loops);

        Future[] results = new Future[loops];
        for (int i = 0; i < loops; i++) {
            Utils.log("synchronous loop() starting test " + i);
            try {
                results[i] = test.test(new Checker(5 * 1000, td));
            }
            catch (InterruptedException e1) {
                Assert.fail("Unexpected interruption", e1);
            }

        }

        Utils.handleResults(loops, results);
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread
     *
     * @param number
     * @param test
     * @param maxSimultaneousWorkers
     * @param expectedTasksScheduled
     * @param td
     */
    private void loop(int number, BulkheadTestBackend test, int maxSimultaneousWorkers, int expectedTasksScheduled,
            TestData td) {
        td.setExpectedTasksScheduled(expectedTasksScheduled);
        loop(number, test, maxSimultaneousWorkers, td);
    }

}