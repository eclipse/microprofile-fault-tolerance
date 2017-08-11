/*
 *******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BackendTestDelegate;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55ClassSynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55MethodSynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry550MethodSynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55ClassSynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55MethodSynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.ParrallelBulkheadTest;
import org.jboss.arquillian.container.test.api.Deployment;
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

/**
 * This collection of tests tests that failures, particularly Synchronous
 * Bulkhead related failures will cause the Retry annotation logic to work
 * correctly.
 *
 * @author Gordon Hutchison
 *
 */
public class BulkheadSynchRetryTest extends Arquillian {

    /*
     * We use an executer service to simulate the parallelism of multiple*
     * simultaneous requests
     */
    private static final int THREADPOOL_SIZE = 30;
    private ExecutorService xService = Executors.newFixedThreadPool(THREADPOOL_SIZE);

    /*
     * As the FaultTolerance annotation only works on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */
    @Inject
    private Bulkhead55MethodSynchronousRetryBean methodBean;
    @Inject
    private Bulkhead55ClassSynchronousRetryBean classBean;

    @Inject
    private BulkheadRapidRetry55ClassSynchBean rrClassBean;
    @Inject
    private BulkheadRapidRetry55MethodSynchBean rrMethodBean;

    @Inject
    private BulkheadRapidRetry550MethodSynchBean zeroRetryBean;

    /**
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     * 
     * @return the test war "ftBulkheadSynchRetryTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchRetryTest.jar")
                .addPackage(Bulkhead55ClassSynchronousRetryBean.class.getPackage()).addClass(Utils.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchRetryTest.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * This method is called prior to every test. It waits for all workers to
     * finish and resets the Checker's state.
     */
    @BeforeTest
    public void beforeTest() {
        Utils.quiesce();
        Checker.reset();
    }

    /**
     * Test no regression due to passive Retry. The Bulkhead is 5, but also has
     * a queue of 5, so the Retry should not come into effect for 10 tasks
     */
    @Test()
    public void testBulkheadClassSynchronousPassiveRetry55() {
        int threads = 10;
        int maxSimultaneousWorkers = 5;
        threads(threads, classBean, maxSimultaneousWorkers);
        Utils.check();
    }

    /**
     * Check we do not loose anything from the queue due to exceptions covered
     * by Retry at the class level. The Checker backends will throw an exception
     * the first time their 'perform' method is called but will run OK when retried.
     */
    @Test()
    public void testBulkheadQueReplacesDueToClassRetryFailures() {
        int threads = 10;
        Checker.setExpectedInstances(threads);
        int maxSimultaneousWorkers = 5;
        Future[] results = new Future[threads];
        
        // As we are causing workers to get 'blown up' we cannot know that we get
        // a full set at once, so we switch off the test that checks that the
        // bulkhead 'filled up'. We will still check we don't get more than the bulkhead
        // at one time.
        Checker.setExpectedMaxWorkers(maxSimultaneousWorkers, false);
        

        for (int i = 0; i < threads; i++) {
            Utils.log("Starting test " + i);
            BackendTestDelegate failOnce = new Checker(1, 1);
            results[i] = xService.submit(new ParrallelBulkheadTest(rrClassBean, failOnce));
        }

        Utils.handleResults(threads, results);
        Utils.check();
    }

    /**
     * Check we do not loose anything from the queue due to exceptions covered
     * by Retry at the method level. The Checker backends will throw an exception
     * the first time their 'perform' method is called but will run OK when retried.
     */
    @Test()
    public void testBulkheadQueReplacesDueToMethodRetryFailures() {
        int threads = 10;
        int maxSimultaneousWorkers = 5;
        Checker.setExpectedInstances(threads);
        Checker.setExpectedMaxWorkers(0);
        Future[] results = new Future[threads];
        Checker.setExpectedMaxWorkers(maxSimultaneousWorkers, false);

        // As we are causing workers to get 'blown up' we cannot know that we get
        // a full set at once, so we switch off the test that checks that the
        // bulkhead 'filled up'. We still check we don't get more than the bulkhead
        // at one time.

        for (int i = 0; i < threads; i++) {
            Utils.log("Starting test " + i);
            BackendTestDelegate failOnce = new Checker(1, 1);
            results[i] = xService.submit(new ParrallelBulkheadTest(rrMethodBean, failOnce));
        }

        Utils.handleResults(threads, results);
        Utils.check();
    }

    /**
     * Test that Retry can be used to prevent receiving Bulkhead exceptions from
     * a method level test. The retries are delayed by one second and will be
     * done 10 times so this gives 10 seconds of retrying, which is enough to
     * get the calls through the bulkhead.
     */
    @Test()
    public void testBulkheadMethodSynchronousRetry55() {
        int threads = 20;
        int maxSimultaneousWorkers = 5;
        threads(threads, methodBean, maxSimultaneousWorkers);
        Utils.check();
    }

    /**
     * Test no regression due to passive Retry. The Bulkhead is 5, but has a
     * queue of 5, so the Retry should not come into effect for 10 Tasks.
     */
    @Test()
    public void testBulkheadPassiveRetryMethodSynchronous55() {
        int threads = 10;
        int maxSimultaneousWorkers = 5;
        threads(threads, methodBean, maxSimultaneousWorkers);
        Utils.check();
    }

    /**
     * Test that Retry can be used to prevent receiving Bulkhead exceptions from
     * a method level test. There is enough retrying in the Bean to cover the
     * queue overflow to allow only ONE extra generation so we should loose
     * 5 calls. 
     */
    @Test()
    public void testBulkheadRetryClassSynchronous55() {
        int threads = 20;
        int expectedTasks = 15; // We Retry just long enough for the first
                                // generation to finish.
        int maxSimultaneousWorkers = 5;
        threads(threads, classBean, maxSimultaneousWorkers, expectedTasks);
        Utils.check();
    }

    /**
     * Test that without retries that bulkhead exceptions are raised and that no
     * more than the expected number of tests go through.
     */
    @Test()
    public void testNoRetriesBulkhead() {
        int threads = 30;
        int maxSimultaneousWorkers = 5;
        int expectedTasks = 10;
        threads(threads, zeroRetryBean, maxSimultaneousWorkers, expectedTasks);
        Utils.check();
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
            Utils.log("Starting test " + i);
            results[i] = xService.submit(new ParrallelBulkheadTest(test));
        }

        Utils.handleResults(number, results);
    }

    /**
     * Front end to threads(x,y,z) method above that allows to set explicitly the number
     * of tasks we expect to get through to the backend.
     * 
     * @param threads
     * @param bean
     * @param maxSimultaneousWorkers
     * @param expectedTasks
     */
    private void threads(int threads, BulkheadTestBackend bean, int maxSimultaneousWorkers, int expectedTasks) {
        Checker.setExpectedTasksScheduled(expectedTasks);
        threads(threads, bean, maxSimultaneousWorkers);
    }

}