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

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.ParrallelBulkheadTest;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.core.api.Asynchronousing.ExecutorService;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */
public class BulkheadSynchTest extends Arquillian {

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
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     * 
     * @return the test war "ftBulkheadSynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchTest.jar")
                .addPackage(BulkheadClassSemaphoreDefaultBean.class.getPackage()).addClass(Utils.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmathod: " + testContext.getName());
    }

    /**
     * Tests the class synchronous Bulkhead3. This test will check that 3 and no
     * more than 3 parallel synchronous calls are allowed into a method that is
     * a member of a @Bulkhead(3) Class.
     */
    @Test()
    public void testBulkheadClassSemaphore3() {
        TestData td = new TestData(new CountDownLatch(3));
        threads(20, bhBeanClassSemaphore3, 3, td);
        td.check();
    }

    /**
     * Tests the class synchronous Bulkhead10. This test will check that 10 and
     * no more than 10 parallel synchronous calls are allowed into a method that
     * is a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassSemaphore10() {
        TestData td = new TestData(new CountDownLatch(10));

        threads(20, bhBeanClassSemaphore10, 10, td);
        td.check();

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
        TestData td = new TestData(new CountDownLatch(10));

        threads(20, bhBeanMethodSemaphore10, 10, td);
        td.check();
    }

    /**
     * Tests the method synchronous Bulkhead3. This test will check that 3 and
     * no more than 3 parallel synchronous calls are allowed into a method that
     * has an individual Bulkhead(3) annotation
     */
    @Test()
    public void testBulkheadMethodSemaphore3() {
        TestData td = new TestData(new CountDownLatch(3));

        threads(20, bhBeanMethodSemaphore3, 3, td);
        td.check();
    }

    /**
     * Tests the basic class synchronous Bulkhead. This test will check that 10
     * and no more than 10 parallel synchronous calls are allowed into a method
     * that is a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassSemaphoreDefault() {
        TestData td = new TestData(new CountDownLatch(10));

        threads(20, bhBeanClassSemaphoreDefault, 10, td);
        td.check();

    }

    /**
     * Tests the basic method synchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but not more than 10 threads get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadMethodSemaphoreDefault() {
        TestData td = new TestData(new CountDownLatch(10));

        threads(20, bhBeanMethodSemaphoreDefault, 10, td);
        td.check();
    }

    /**
     * Run a number of Callable's in parallel
     * 
     * @param number
     * @param test
     * @param maxSimultaneousWorkers
     */
    private void threads(int number, BulkheadTestBackend test, int maxSimultaneousWorkers, TestData td) {

        td.setExpectedMaxSimultaneousWorkers(maxSimultaneousWorkers);
        td.setExpectedInstances(number);
        Future[] results = new Future[number];
        for (int i = 0; i < number; i++) {
            Utils.log("Starting test " + i);
            results[i] = xService.submit(new ParrallelBulkheadTest(test, td));
        }

        Utils.handleResults(number, results);
    }

}