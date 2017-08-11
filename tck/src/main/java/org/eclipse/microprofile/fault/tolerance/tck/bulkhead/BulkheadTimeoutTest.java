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

import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsynchCallableCaller;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousTimeoutBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSynchronousTimeoutBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousTimeoutBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSynchronousTimeoutBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.ParrallelBulkheadTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */
public class BulkheadTimeoutTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */
    @Inject
    private BulkheadClassSynchronousTimeoutBean bhCSTB;
    @Inject
    private BulkheadClassAsynchronousTimeoutBean bhCATB;
    @Inject
    private BulkheadMethodAsynchronousTimeoutBean bhMATB;
    @Inject
    private BulkheadMethodSynchronousTimeoutBean bhMSTB;

    @Inject
    private AsynchCallableCaller caller;

    /**
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     * 
     * @return the test war "ftBulkheadSynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadTimeoutTest.jar")
                .addPackage(BulkheadClassSynchronousTimeoutBean.class.getPackage()).addClass(Utils.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTimeoutTest.war").addAsLibrary(testJar);
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
        Checker.setExpectedInstances(2);
        Checker.setExpectedMaxWorkers(5);
        Checker.setExpectedTasksScheduled(10);
        Checker.setExpectedTasksCompleted(5);
    }

    /**
     * Tests that tasks which timeout are replaced by those
     * queueing to get into the bulkhead
     * for synchronous class bulkheads
     */
    @Test(enabled = true)
    public void testBulkheadClassSynchTimeoutQueueing() {
        asynchs(10, bhCSTB, 5);
        Utils.check();
    }

    /**
     * Tests that tasks which timeout are replaced by those
     * queueing to get into the bulkhead
     * for synchronous method bulkheads
     */
    @Test(enabled = false)
    public void testBulkheadMethodSynchTimeoutQueueing() {

        asynchs(10, bhMSTB, 5);
        Utils.check();
    }

    /**
     * Tests that tasks which timeout are replaced by those
     * queueing to get into the bulkhead
     * for asynchronous class bulkheads
     */
    @Test(enabled = false)
    public void testBulkheadClassAsynchTimeoutQueueing() {
        loop(bhCATB);
        Utils.check();
    }

    /**
     * Tests that tasks which timeout are replaced by those
     * queueing to get into the bulkhead
     * for asynchronous method bulkheads
     */
    @Test(enabled = false)
    public void testBulkheadMethodAsynchTimeoutQueueing() {
        loop(bhMATB);
        Checker.setExpectedInstances(2);
        Checker.setExpectedMaxWorkers(5);
        Checker.setExpectedTasksScheduled(10);
        Checker.setExpectedTasksCompleted(5);
        Utils.check();
    }

    /**
     * Loop round putting a number of calls into a
     * (usually) asynch bulkhead.
     * @param test
     */
    public void loop(BulkheadTestBackend test) {
        Checker slow = new Checker(30 * 1000);
        Checker fast = new Checker(100);

        Checker.setExpectedInstances(2);
        for (int i = 0; i < 10; i++) {
            try {
                if (i < 5) {
                    test.test(slow); // These will timeout
                }
                else {
                    test.test(fast); // These will queue then complete
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Run a number of Callable's in parallel. To do this it uses an Injected
     * bean with a call(Callable c) method that runs asynchronously. 
     * This method will run the first 5 calls with slow backends (that are
     * going to timeout and be replaced by the queueing tasks all being well). 
     * 
     * @param number how many to run
     * @param test what to run
     * @param maxSimultaneousWorkers the limit on the bulkhead we are checking
     */
    private void asynchs(int number, BulkheadTestBackend test, int maxSimultaneousWorkers) {

        Checker slow = new Checker(30 * 1000);
        Checker fast = new Checker(100);

        Checker.setExpectedMaxWorkers(maxSimultaneousWorkers, false);
        Checker.setExpectedInstances(2);
        Future[] results = new Future[number];
        for (int i = 0; i < number; i++) {
            Utils.log("Starting test " + i);
            if (i < 5) {
                // These will timeout
                try {
                    results[i] = caller.call(new ParrallelBulkheadTest(test, slow));
                }
                catch (Exception e) {
                    Utils.log(e.toString());
                    Assert.fail("Unexpected " + e.toString());
                }
            }
            else {
                // These will queue then complete if the bulkhead queue is big enough in
                // this test.
                try {
                    results[i] = caller.call(new ParrallelBulkheadTest(test, fast));
                }
                catch (Exception e) {
                    Utils.log(e.toString());
                    Assert.fail("Unexpected " + e.toString());
                }
            }
        }

        Utils.handleResults(number, results);
    }

}