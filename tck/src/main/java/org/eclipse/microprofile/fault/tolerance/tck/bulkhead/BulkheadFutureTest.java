/*
 *******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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

import static org.awaitility.Awaitility.await;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadFutureClassBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadFutureMethodBean;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * This set of tests will test correct operation on the relevant methods of the Future object that is returned from the
 * business method of a Asynchronous Method or Class.
 *
 * @author Gordon Hutchison
 * @author carlosdlr
 * @author Andrew Rouse
 */
public class BulkheadFutureTest extends Arquillian {

    @Inject
    private BulkheadFutureMethodBean bhFutureMethodBean;

    @Inject
    private BulkheadFutureClassBean bhFutureClassBean;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadFutureTest.jar")
                .addClasses(BulkheadFutureMethodBean.class, BulkheadFutureClassBean.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap.create(WebArchive.class, "ftBulkheadFutureTest.war")
                .addAsLibrary(testJar);
    }

    /**
     * Tests that the Future that is returned from an asynchronous bulkhead method can be queried for Done OK before and
     * after a goodpath .get()
     */
    @Test
    public void testBulkheadMethodAsynchFutureDoneAfterGet()
            throws InterruptedException, ExecutionException, TimeoutException {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            TestFuture testFuture = new TestFuture();
            Barrier barrier = taskManager.newBarrier();
            Future<String> result = bhFutureMethodBean.test(testFuture, barrier);

            assertFalse(result.isDone(), "Future reporting Done when not");

            barrier.open();

            assertEquals(result.get(10, TimeUnit.SECONDS), "RESULT");
            assertEquals(result.get(), "RESULT");

            assertTrue(result.isDone(), "Future done not reporting true");
        }
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead method can be queried for Done OK even if the
     * user never calls get() to drive the backend (i.e. the method is called non-lazily)
     */
    @Test
    public void testBulkheadMethodAsynchFutureDoneWithoutGet() {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            TestFuture testFuture = new TestFuture();
            Barrier barrier = taskManager.newBarrier();

            Future<String> result = bhFutureMethodBean.test(testFuture, barrier);

            assertFalse(result.isDone(), "Future reporting Done when not");

            barrier.open();

            await("Future reports done").until(() -> result.isDone());
        }

    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can be queried for Done OK after a goodpath
     * get with timeout and also multiple gets can be called ok. This test is for the annotation at a Class level.
     */
    @Test
    public void testBulkheadClassAsynchFutureDoneAfterGet()
            throws InterruptedException, ExecutionException, TimeoutException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            TestFuture testFuture = new TestFuture();
            Barrier barrier = taskManager.newBarrier();
            Future<String> result = bhFutureClassBean.test(testFuture, barrier);

            assertFalse(result.isDone(), "Future reporting Done when not");

            barrier.open();

            assertEquals(result.get(10, TimeUnit.SECONDS), "RESULT");
            assertEquals(result.get(), "RESULT");

            assertTrue(result.isDone(), "Future done not reporting true");
        }
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can be queried for Done OK when get() is not
     * called. This test is for the annotation at a Class level.
     */
    @Test
    public void testBulkheadClassAsynchFutureDoneWithoutGet() {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            TestFuture testFuture = new TestFuture();
            Barrier barrier = taskManager.newBarrier();

            Future<String> result = bhFutureClassBean.test(testFuture, barrier);

            assertFalse(result.isDone(), "Future reporting Done when not");

            barrier.open();

            await("Future reports done").until(() -> result.isDone());
        }
    }

    public static final class TestFuture implements Future<String> {
        private boolean isCancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            isCancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public String get() {
            return "RESULT";
        }

        @Override
        public String get(long timeout, TimeUnit unit) {
            return "RESULT";
        }

    }
}
