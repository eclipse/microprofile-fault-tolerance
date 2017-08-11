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
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousCircuitBreakerBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSynchronousCircuitBreakerBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousCircuitBreakerBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSynchronousCircuitBreakerBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.CBBulkheadBackend;
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
public class BulkheadCircuitBreakerTest extends Arquillian {

    // We loop round this, and fail if true. 3 OK, 3 fails, 3 OK (half-open), 1
    // OK(open)
    private static final boolean[] FAIL_SEQUENCE =
    { false, false, false, true, true, true, false, false, false, false };
    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */
    @Inject
    private BulkheadClassSynchronousCircuitBreakerBean bhCSCBB;
    @Inject
    private BulkheadClassAsynchronousCircuitBreakerBean bhCACBB;
    @Inject
    private BulkheadMethodAsynchronousCircuitBreakerBean bhMACBB;
    @Inject
    private BulkheadMethodSynchronousCircuitBreakerBean bhMSCBB;

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
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadCircuitBreakerTest.jar")
                .addPackage(BulkheadClassSynchronousCircuitBreakerBean.class.getPackage()).addClass(Utils.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadCircuitBreakerTest.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * This method is called prior to every test. It waits for all workers to
     * finish and resets the CBBulkheadBackend's state.
     */
    @BeforeTest
    public void beforeTest() {
        Utils.quiesce();
        CBBulkheadBackend.reset();
        CBBulkheadBackend.setFailSequence(FAIL_SEQUENCE);
    }

    /**
     * Tests that tasks which CircuitBreaker are replaced by those queueing to
     * get into the bulkhead for synchronous class bulkheads
     */
    @Test()
    public void testBulkheadClassSynchCircuitBreakerQueueing() {
        asynchs(10, bhCSCBB, 5);
        Assert.assertEquals(CBBulkheadBackend.getTasksCompleted(), 3 );
        Utils.sleep(5000);
        Utils.log("Complete: " + CBBulkheadBackend.getTasksCompleted());
    }

    /**
     * Tests that tasks which CircuitBreaker are replaced by those queueing to
     * get into the bulkhead for synchronous method bulkheads
     */
    @Test()
    public void testBulkheadMethodSynchCircuitBreakerQueueing() {
        asynchs(10, bhMSCBB, 5);
        Utils.sleep(5000);
        Utils.log("Complete: " + CBBulkheadBackend.getTasksCompleted());
        Assert.assertEquals(CBBulkheadBackend.getTasksCompleted(), 3 );

    }

    /**
     * Tests that tasks which CircuitBreaker are replaced by those queueing to
     * get into the bulkhead for asynchronous class bulkheads
     */
    @Test()
    public void testBulkheadClassAsynchCircuitBreakerQueueing() {
        loop(bhCACBB);
        Utils.sleep(5000);  
        Assert.assertEquals(CBBulkheadBackend.getTasksCompleted(), 3 );
        Utils.log("Complete: " + CBBulkheadBackend.getTasksCompleted());

    }

    /**
     * Tests that tasks which CircuitBreaker are replaced by those queueing to
     * get into the bulkhead for asynchronous method bulkheads
     */
    @Test()
    public void testBulkheadMethodAsynchCircuitBreakerQueueing() {
        loop(bhMACBB);
        Utils.sleep(5000);
        Assert.assertEquals(CBBulkheadBackend.getTasksCompleted(), 3 );
        Utils.log("Complete: " + CBBulkheadBackend.getTasksCompleted());
    }

    /**
     * Loop round putting a number of calls into a (usually) asynch bulkhead.
     * 
     * @param test
     */
    public void loop(BulkheadTestBackend test) {
        for (int i = 0; i < 10; i++) {
            CBBulkheadBackend backEnd = new CBBulkheadBackend(1, FAIL_SEQUENCE);
            try {
                test.test(backEnd);
            }
            catch (Exception e) {
                Utils.log(e.toString());
                Utils.log("Call threw exception " + e.toString());
            }
        }
    }

    /**
     * Run a number of Callable's in parallel. To do this it uses an Injected
     * bean with a call(Callable c) method that runs asynchronously.
     * We create the backends with an array of booleans that is looped round,
     * where there is a true the back-end will throw a RuntimeException.
     * 
     * @param number
     *            how many to run
     * @param test
     *            what to run
     * @param maxSimultaneousWorkers
     *            the limit on the bulkhead we are checking
     */
    private void asynchs(int number, BulkheadTestBackend test, int maxSimultaneousWorkers) {

        Future[] results = new Future[number];

        for (int i = 0; i < number; i++) {
            Utils.log("Starting test " + i);
            CBBulkheadBackend backEnd = new CBBulkheadBackend(1, FAIL_SEQUENCE);
            try {
                results[i] = caller.call(new ParrallelBulkheadTest(test, backEnd));
            }
            catch (Exception e) {
                Utils.log(e.toString());
                Utils.log("Call threw exception " + e.toString());
            }
        }

        Utils.handleResults(number, results);

    }
}