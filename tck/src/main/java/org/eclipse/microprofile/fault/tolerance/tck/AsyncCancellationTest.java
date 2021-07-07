/*
 *******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.fault.tolerance.tck;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncCancellationClient;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that calling {@code cancel()} on a {@code Future} returned from a method annotated with {@code Asynchronous} is
 * handled correctly.
 * <p>
 * According to the documentation of {@code Future}:
 * <ul>
 * <li>If the task has not started running, it should not run</li>
 * <li>If the task has started running and {@code mayInterruptIfRunning} is {@code true}, the thread should be
 * interrupted</li>
 * <li>When {@code cancel()} returns, calling {@code isDone()} should return {@code true}</li>
 * <li>If {@code cancel()} returns {@code true}, calling {@code isCancelled()} should return {@code true}</li>
 * </ul>
 */
public class AsyncCancellationTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsyncCancellation.jar")
                .addClasses(AsyncCancellationClient.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftAsyncCancellation.war")
                .addAsLibrary(testJar);
        return war;
    }

    @Inject
    private AsyncCancellationClient bean;

    @Test
    public void testCancel() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            Barrier barrier = taskManager.newBarrier();
            AtomicBoolean wasInterrupted = new AtomicBoolean(false);

            Future<?> result = bean.serviceAsync(barrier, wasInterrupted);
            barrier.assertAwaits();

            result.cancel(true);

            await("wasInterrupted").untilAtomic(wasInterrupted, is(true));

            assertTrue(result.isCancelled(), "Task is not cancelled");
            assertTrue(result.isDone(), "Task is not done");
            Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
            Exceptions.expect(CancellationException.class, () -> result.get());
        }
    }

    @Test
    public void testCancelWithoutInterrupt() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            Barrier barrier = taskManager.newBarrier();
            AtomicBoolean wasInterrupted = new AtomicBoolean(false);

            Future<?> result = bean.serviceAsync(barrier, wasInterrupted);
            barrier.assertAwaits();

            result.cancel(false);

            await("wasInterrupted").during(TCKConfig.getConfig().getTimeoutInDuration(500))
                    .untilAtomic(wasInterrupted, is(false));

            assertTrue(result.isCancelled(), "Task is not cancelled");
            assertTrue(result.isDone(), "Task is not done");
            Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
            Exceptions.expect(CancellationException.class, () -> result.get());

            // Allow method to complete
            barrier.open();

            // Assert future still gives cancellation exception after task is allowed to complete
            await("cancellationException").during(TCKConfig.getConfig().getTimeoutInDuration(500))
                    .untilAsserted(() -> Exceptions.expect(CancellationException.class,
                            () -> result.get(2, TimeUnit.SECONDS)));
            assertTrue(result.isCancelled(), "Task is not cancelled");
            assertTrue(result.isDone(), "Task is not done");
        }
    }

    @Test
    public void testCancelledButRemainsInBulkhead() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            Barrier barrier1 = taskManager.newBarrier();
            Future<?> result1 = bean.serviceAsyncBulkhead(barrier1);
            barrier1.assertAwaits();

            Barrier barrier2 = taskManager.newBarrier();
            Future<?> result2 = bean.serviceAsyncBulkhead(barrier2);
            barrier2.assertNotAwaiting();

            result1.cancel(false);

            // Task 2 does not start because task 1 is still running (it was not interrupted)
            barrier2.assertNotAwaiting();

            assertTrue(result1.isCancelled(), "Task is not cancelled");
            assertTrue(result1.isDone(), "Task is not done");
            Exceptions.expect(CancellationException.class, () -> result1.get(2, TimeUnit.SECONDS));
            Exceptions.expect(CancellationException.class, () -> result1.get());
        }
    }

    @Test
    public void testCancelledWhileQueued() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            Barrier barrier1 = taskManager.newBarrier();
            Future<?> result1 = bean.serviceAsyncBulkhead(barrier1);
            barrier1.assertAwaits();

            Barrier barrier2 = taskManager.newBarrier();
            Future<?> result2 = bean.serviceAsyncBulkhead(barrier2);
            barrier2.assertNotAwaiting();

            result2.cancel(false);

            barrier1.open();

            // Task 2 was cancelled while it was in the bulkhead queue, it should not start and await its barrier
            barrier2.assertNotAwaiting();
        }
    }

    @Test
    public void testCancelledDoesNotRetry() throws InterruptedException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            Barrier barrier = taskManager.newBarrier();
            Future<?> result = bean.serviceAsyncRetry(barrier);
            barrier.assertAwaits();

            result.cancel(true);

            Thread.sleep(TCKConfig.getConfig().getTimeoutInMillis(500));

            assertEquals(bean.getServiceAsyncRetryAttempts(), 1,
                    "Method should not have been retried - too many retry attempts");
        }
    }
}
