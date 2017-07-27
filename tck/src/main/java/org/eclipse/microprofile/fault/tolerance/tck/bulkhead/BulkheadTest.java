/**********************************************************************
* Copyright (c) 2017 Contributors to the Eclipse Foundation 
*
* See the NOTICES file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* All rights reserved. This program and the accompanying materials 
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphoreDefaultBean;
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

    /*
     * We use an executer service to simulate the parallelism of multiple
     * simultaneous requests
     */
    private static final int THREADPOOL_SIZE = 30;
    private ExecutorService xService = Executors.newFixedThreadPool(THREADPOOL_SIZE);

    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */
    @Inject
    private BulkheadClassSemaphoreDefaultBean bhBeanClassSemaphoreDefault;
    @Inject
    private BulkheadMethodSemaphoreDefaultBean bhBeanMethodSemaphoreDefault;
    @Inject
    private BulkheadClassSemaphore3Bean bhBeanClassSemaphore3;
    @Inject
    private BulkheadMethodSemaphore3Bean bhBeanMethodSemaphore3;
    @Inject
    private BulkheadClassSemaphore10Bean bhBeanClassSemaphore10;
    @Inject
    private BulkheadMethodSemaphore10Bean bhBeanMethodSemaphore10;

    /**
     * This is the Arquillian deploy method that controls the contents of the war
     * that contains all the tests.
     * 
     * @return the test war "ftBulkheadTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadTest.jar")
                .addPackage(BulkheadClassSemaphore10Bean.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
        return war;
    }

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
     * A simple local logger. Messages are logged with a prefix of the threadId
     * and the current time.
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

    /**
     * This method is called prior to every test. It waits for all workers to
     * finish and resets the Checker's state.
     */
    @BeforeTest
    public void beforeTest() {
        quiesce();
        Checker.reset();
    }

    /**
     * Wait for the tests to finish and check the results
     */
    public void check() {
        quiesce();
        Checker.check();
    }

    /**
     * Wait for the tests to finish. We avoid using 'isDone' that passes through
     * the wrapping Future as we want to test this independently.
     */
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