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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronous10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronous3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphoreDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronous10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronous3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSemaphore10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSemaphore3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSemaphoreDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.ParrallelBulkheadTest;
import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.core.api.Asynchronousing.ExecutorService;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */
public class BulkheadTest extends Arquillian {

    private static final int THREADPOOL_SIZE = 30;
    private ExecutorService xService = Executors.newFixedThreadPool(THREADPOOL_SIZE);

    @Inject
    private BulkheadClassSemaphoreDefaultBean bhBeanClassSemaphoreDefault;
    @Inject
    private BulkheadMethodSemaphoreDefaultBean bhBeanMethodSemaphoreDefault;
    @Inject
    private BulkheadClassAsynchronousDefaultBean bhBeanClassAsynchronousDefault;
    @Inject
    private BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;

    @Inject
    private BulkheadClassSemaphore3Bean bhBeanClassSemaphore3;
    @Inject
    private BulkheadMethodSemaphore3Bean bhBeanMethodSemaphore3;
    @Inject
    private BulkheadClassAsynchronous3Bean bhBeanClassAsynchronous3;
    @Inject
    private BulkheadMethodAsynchronous3Bean bhBeanMethodAsynchronous3;

    @Inject
    private BulkheadClassSemaphore10Bean bhBeanClassSemaphore10;
    @Inject
    private BulkheadMethodSemaphore10Bean bhBeanMethodSemaphore10;
    @Inject
    private BulkheadClassAsynchronous10Bean bhBeanClassAsynchronous10;
    @Inject
    private BulkheadMethodAsynchronous10Bean bhBeanMethodAsynchronous10;

    @Inject
    private BulkheadClassAsynchronousQueueingBean bhBeanClassAsynchronousQueueing;
    @Inject
    private BulkheadMethodAsynchronousQueueingBean bhBeanMethodAsynchronousQueueing;

    /**
     * Tests the class synchronous Bulkhead3. This test will check that 3 and no
     * more than 3 parallel synchronous calls are allowed into a method that is
     * a member of a @Bulkhead(3) Class.
     */
    @Test()
    public void testBulkheadClassSemaphore3() {
        threads(20, bhBeanClassSemaphore3, 3);
        check();
    }

    /**
     * Tests the class synchronous Bulkhead10. This test will check that 10 and
     * no more than 10 parallel synchronous calls are allowed into a method that
     * is a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassSemaphore10() {
        threads(20, bhBeanClassSemaphore10, 10);
        check();

    }

    /**
     * Tests the method synchronous Bulkhead10. This test will check that 10 and
     * no more than 10 parallel synchronous calls are allowed into a method that
     * has an individual
     * 
     * @Bulkhead(10) annotation
     */
    @Test()
    public void testBulkheadMethodSemaphore10() {
        threads(20, bhBeanMethodSemaphore10, 10);
        check();
    }

    /**
     * Tests the class asynchronous Bulkhead(10) This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that is
     * a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassAsynchronous10() {
        loop(10, bhBeanClassAsynchronous10, 10);
        check();
    }

    /**
     * Tests the method asynchronous Bulkhead(10). This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that has
     * an individual
     * 
     * @Bulkhead(10) annotation
     */
    @Test()
    public void testBulkheadMethodAsynchronous10() {
        loop(10, bhBeanMethodAsynchronous10, 10);
        check();
    }

    /**
     * Tests the method synchronous Bulkhead3. This test will check that 3 and
     * no more than 3 parallel synchronous calls are allowed into a method that
     * has an individual Bulkhead(3) annotation
     */
    @Test()
    public void testBulkheadMethodSemaphore3() {
        threads(20, bhBeanMethodSemaphore3, 3);
        check();
    }

    /**
     * Tests the class asynchronous Bulkhead(3) This test will check that 3 and
     * no more than 3 asynchronous calls are allowed into a method that is a
     * member of a @Bulkhead(3) Class.
     */
    @Test()
    public void testBulkheadClassAsynchronous3() {
        loop(10, bhBeanClassAsynchronous3, 3);
        check();
    }

    /**
     * Tests the method asynchronous Bulkhead(3). This test will check that 3
     * and no more than 3 asynchronous calls are allowed into a method that has
     * an individual Bulkhead(3) annotation
     */
    @Test()
    public void testBulkheadMethodAsynchronous3() {
        loop(10, bhBeanMethodAsynchronous3, 3);
        check();
    }

    /**
     * Tests the basic class synchronous Bulkhead. This test will check that 10
     * and no more than 10 parallel synchronous calls are allowed into a method
     * that is a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassSemaphoreDefault() {
        threads(20, bhBeanClassSemaphoreDefault, 10);
        check();

    }

    /**
     * Tests the basic method synchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 threads get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadMethodSemaphoreDefault() {
        threads(20, bhBeanMethodSemaphoreDefault, 10);
        check();
    }

    /**
     * Tests the basic class asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadClassAsynchronousDefault() {
        loop(10, bhBeanClassAsynchronousDefault, 10);
        check();
    }

    /**
     * Tests the basic method asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadMethodAsynchronousDefault() {
        loop(10, bhBeanMethodAsynchronousDefault, 10);
        check();
    }

    /**
     * Tests the queueing class asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test()
    public void testBulkheadClassAsynchronousQueueing10() {
        loop(20, bhBeanClassAsynchronousQueueing, 10, 20);
        check();
    }

    /**
     * Tests the queueing method asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test()
    public void testBulkheadMethodAsynchronousQueueing10() {
        loop(20, bhBeanMethodAsynchronousQueueing, 10, 20);
        check();
    }

    /**
     * Run a number of Callable's in parallel
     * 
     * @param number
     * @param test
     * @param maxSimultaneousWorkers
     */
    private void threads(int number, BulkheadTestBackend test, int maxSimultaneousWorkers) {

        Checker.setExpectedMaxWorkers(maxSimultaneousWorkers);
        Checker.setExpectedInstances(number);
        Future[] results = new Future[number];
        for (int i = 0; i < number; i++) {
            log("Starting test " + i);
            results[i] = xService.submit(new ParrallelBulkheadTest(test));
        }

        handleResults(number, results);
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread
     * 
     * @param number
     * @param test
     * @param maxSimultaneousWorkers
     * @param expectedTasksScheduled
     */
    private void loop(int number, BulkheadTestBackend test, int maxSimultaneousWorkers, int expectedTasksScheduled) {

        Checker.setExpectedTasksScheduled(expectedTasksScheduled);
        loop(number, test, maxSimultaneousWorkers);
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread.
     * Here we do not check that amount that were successfully through the
     * Bulkhead
     * 
     * @param number
     * @param test
     * @param maxSimultaneousWorkers
     */
    private void loop(int number, BulkheadTestBackend test, int maxSimultaneousWorkers) {

        Checker.setExpectedMaxWorkers(maxSimultaneousWorkers);
        Checker.setExpectedInstances(number);
        Checker.setExpectedTasksScheduled(number);

        Future[] results = new Future[number];
        for (int i = 0; i < number; i++) {
            log("Starting test " + i);
            results[i] = test.test(new Checker(5 * 1000));
        }

        handleResults(number, results);
    }

    /**
     * Common function to check the returned results of the tests
     * 
     * @param number
     * @param results
     */
    private void handleResults(int number, Future[] results) {
        try {
            boolean done = false;
            // Wait for all the backends to finish
            while (!done) {
                done = true;
                for (int i = 0; i < number; i++) {
                    boolean thisDone = (results[i] == null || results[i].get() == null || ((Future) results[i]).isDone()
                            || results[i].get() instanceof Future && ((Future) results[i].get()).isDone());
                    done = done && thisDone;
                    log("Result for " + i + (thisDone ? " (Done)" : " (NotDone)") + " is "
                            + (((Future) results[i]).get() instanceof Future
                                    ? ((Future) ((Future) results[i]).get()).get() : ((Future) results[i]).get()));

                }
                Thread.sleep(1000);
            }
        }
        catch (Throwable e) {
            log(e.toString());
        }
    }

    /**
     * A simple local logger
     * 
     * @param s
     *            message
     */
    public static void log(String s) {
        System.out.println(tid() + " " + hms() + ": " + s);
    }

    /**
     * Get the time in simple format
     * 
     * @return
     */
    private static String hms() {
        return DateTimeFormatter.ofPattern("HH:mm:ss:SS").format(LocalDateTime.now());
    }

    /**
     * Get the Thread ID
     * 
     * @return
     */
    private static long tid() {
        return Thread.currentThread().getId();
    }

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadTest.jar")
                .addPackage(BulkheadClassSemaphore10Bean.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void reset() {
        quiesce();
        Checker.reset();
    }

    public void check() {
        quiesce();
        Checker.check();
    }

    private void quiesce() {
        try {
            int waits = 0;
            while (Checker.getWorkers() > 0) {
                BulkheadTest.log("Waiting for " + Checker.getWorkers() + " workers to finish");
                Thread.sleep(100);
                if (waits++ > 100) {
                    break;
                }

            }
        }
        catch (InterruptedException e) {
            BulkheadTest.log(e.toString());
        }
    }

}