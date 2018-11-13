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
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphoreDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.ParrallelBulkheadTest;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */
public class BulkheadSynchConfigTest extends Arquillian {

    /**
     * This is the Arquillian deploy method that controls the contents of the
     * war that contains all the tests.
     *
     * @return the test war "ftBulkheadSynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchTest.jar")
            .addPackage(BulkheadClassSemaphoreDefaultBean.class.getPackage())
            .addClass(Utils.class)
            .addPackage(Packages.UTILS)
            .addAsManifestResource(new StringAsset(
                "Bulkhead/value=5"), "microprofile-config.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmethod: " + testContext.getName());
    }

    /**
     * Tests taking Bulkhead3 waiting for 3 and no more workers and change its configuration by propoerty
     * to switch to 5 workers. Test wouldn't pass without property config working
     */
    @Test()
    public void testBulkheadClassSemaphore3() {
        TestData td = new TestData(new CountDownLatch(3));
        threads(20, bhBeanClassSemaphore3, 5, td);
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

    /*
     * We use an AsyncCaller bean to simulate the parallelism of multiple
     * simultaneous requests
     */
    @Inject
    private AsyncCaller xService;

    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the {@code @Bulkhead} and whether we have queueing or not.
     */
    @Inject
    private Bulkhead3ClassSemaphoreBean bhBeanClassSemaphore3;

}
