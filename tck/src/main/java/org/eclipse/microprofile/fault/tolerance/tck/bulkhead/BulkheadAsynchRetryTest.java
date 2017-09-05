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

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55ClassAsynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead55MethodAsynchronousRetryBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55ClassAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadRapidRetry55MethodAsynchBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
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
 */

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
    private BulkheadRapidRetry55ClassAsynchBean rrClassBean;
    @Inject
    private BulkheadRapidRetry55MethodAsynchBean rrMethodBean;

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
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadAsynchRetryTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmathod: " + testContext.getName());
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
        Utils.loop(iterations, classBean, 5, expectedTasksScheduled, td);
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
        Utils.loop(iterations, methodBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
        td.check();
    }

    /**
     * Test that Retry can be used to prevent receiving Bulkhead exceptions from
     * a method level test. The bean here does not delay its retries so there is
     * not enough time for the first generation of workers and queued workers to
     * progress.
     * 
     * @throws InterruptedException
     */
    @Test()
    public void testBulkheadMethodAsynchronousRetry55Trip() throws InterruptedException {
        int iterations = 11;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData();

        boolean tripped;
        try {
            Utils.loop(iterations, rrMethodBean, maxSimultaneousWorkers, 0, td);
            tripped = false;
        }
        catch (BulkheadException bhe) {
            tripped = true;
            Utils.log("Class Bulkhead queue not long enough as expected " + bhe.getMessage());
        }
        Assert.assertTrue(tripped,
                "Asynchronous class Bulkead Retry not throwing Bulkhead exception when retry limit just exceeded");

        td.setExpectedTasksScheduled(DONT_CHECK);
        td.setExpectedInstances(DONT_CHECK);
        td.check();
    }

    /**
     * Tests overloading the Retries by firing lots of work at a full Method
     * bulkhead
     * 
     * @throws InterruptedException
     */
    @Test()
    public void testBulkheadMethodAsynchronous55RetryOverload() throws InterruptedException {
        int iterations = 1000;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData();
        boolean blown;
        try {
            Utils.loop(iterations, rrMethodBean, maxSimultaneousWorkers, 0, td);
            blown = false;
        }
        catch (BulkheadException bhe) {
            blown = true;
            Utils.log("Method Bulkhead blown as expected " + bhe.getMessage());
        }
        Assert.assertTrue(blown,
                "Asynchronous method Bulkead Retry not throwing Bulkhead exception when retry limit exceeded");
    }

    /**
     * 
     * Tests overloading the Retries by firing lots of work at a full Class
     * bulkhead
     */
    @Test()
    public void testBulkheadClassAsynchronous55RetryOverload() {
        int iterations = 1000;
        int expectedTasksScheduled = iterations;
        int maxSimultaneousWorkers = 5;
        TestData td = new TestData();

        boolean blown;
        try {
            Utils.loop(iterations, rrClassBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
            blown = false;
        }
        catch (BulkheadException bhe) {
            blown = true;
            Utils.log("Class Bulkhead blown as expected " + bhe.getMessage());
        }
        Assert.assertTrue(blown,
                "Asynchronous class Bulkead Retry not throwing Bulkhead exception when retry limit exceeded");
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
        Utils.loop(iterations, methodBean, 5, expectedTasksScheduled, td);
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
        Utils.loop(iterations, classBean, maxSimultaneousWorkers, expectedTasksScheduled, td);
        td.check();
    }

}