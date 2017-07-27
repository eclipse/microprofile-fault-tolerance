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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.core.api.Asynchronousing.ExecutorService;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */
public class BulkheadFutureTest extends Arquillian {

    private static final int THREADPOOL_SIZE = 30;
    private ExecutorService xService = Executors.newFixedThreadPool(THREADPOOL_SIZE);

    @Inject
    private BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadTest.jar")
                .addPackage(BulkheadClassSemaphore10Bean.class.getPackage())
                .addClass(BulkheadTest.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * Tests that the method on the Future that is returned from a asynchronous
     * bulkhead work appropriately.
     */
    @Test()
    public void testBulkheadFuture() {

        Checker.setExpectedMaxWorkers(1);
        Checker.setExpectedInstances(1);
        Checker.setExpectedTasksScheduled(1);

        Checker sulker = new Checker(5000);

        Future result = bhBeanMethodAsynchronousDefault.test(sulker);
        try {
            result.get(1000, TimeUnit.MILLISECONDS);
        }
        catch (Throwable e) {
            Assert.assertTrue(e instanceof TimeoutException,
                    "result.get(1000) did not timeout for long running back-end, e is a " + e.toString()
                            + e.getMessage());
        }

        try {
            BulkheadTest.log("seeking answer");
            Object answer = result.get();
            BulkheadTest.log("answer was: " + answer);
        }
        catch (Throwable e) {
            BulkheadTest.log(e.toString());
        }

        Checker.reset();
        
        Future cancelResult = bhBeanMethodAsynchronousDefault.test(sulker);
        boolean mayInterruptIfRunning = false;
        boolean rc = result.cancel(mayInterruptIfRunning);
       
        Assert.assertFalse(rc, "we expected that the task is still running");
        Assert.assertFalse(result.isCancelled(), "we expected that the task is not cancelled");
        Assert.assertFalse(result.isDone(), "we expected that the task is not done, workers is " + Checker.getWorkers() );

        
        mayInterruptIfRunning = true;
        rc = result.cancel(mayInterruptIfRunning);
        Assert.assertTrue(rc, "after cancel, we expected that the task is still running");
        Assert.assertTrue(result.isCancelled(), "after cancel, we expected that the task is cancelled");
        Assert.assertTrue(result.isDone(), "after cancel, we expected that the task is done");


        // result.

    }

}