/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class Barrier {

    /**
     * Time limit for tasks waiting on the barrier.
     */
    public static final long BARRIER_WAIT_TIME_MS = TCKConfig.getConfig().getTimeoutInMillis(30_000);

    /**
     * Time to wait for something which is expected not to happen (e.g. assert that a task does not complete)
     */
    public static final long EXPECTED_FAIL_TIME_MS = TCKConfig.getConfig().getTimeoutInMillis(500);

    /**
     * Time to wait for something which is expected to happen (e.g. assert that a task completes)
     */
    public static final long WAIT_TIME_MS = TCKConfig.getConfig().getTimeoutInMillis(3000);

    private final CompletableFuture<Void> future = new CompletableFuture<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final CompletableFuture<Void> isWaitingFuture = new CompletableFuture<>();

    public void await() {
        try {
            awaitInterruptably();
        } catch (InterruptedException e) {
            fail("Interrupted while awaiting barrier", e);
        }
    }

    public void awaitInterruptably() throws InterruptedException {
        counter.incrementAndGet();
        isWaitingFuture.complete(null);
        try {
            future.get(BARRIER_WAIT_TIME_MS, MILLISECONDS);
        } catch (ExecutionException e) {
            fail("Unexpected exception while awaiting barrier", e);
        } catch (TimeoutException e) {
            fail("Timed out while awaiting barrier", e);
        }
    }

    public void open() {
        future.complete(null);
        counter.set(0);
    }

    public int countWaiting() {
        return counter.get();
    }

    /**
     * Assert that at least one task awaits on this barrier within WAIT_TIME_MS
     */
    public void assertAwaits() {
        try {
            isWaitingFuture.get(WAIT_TIME_MS, MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Interrupted while checking task is awaiting");
        } catch (ExecutionException e) {
            fail("Unexpected exception while checking task is awaiting", e);
        } catch (TimeoutException e) {
            fail("Timed out while checking task is awaiting");
        }
    }

    /**
     * Assert that no task awaits this barrier within EXPECTED_FAIL_TIME_MS
     */
    public void assertNotAwaiting() {
        try {
            isWaitingFuture.get(EXPECTED_FAIL_TIME_MS, MILLISECONDS);
            fail("Task is awaiting");
        } catch (InterruptedException e) {
            fail("Interrupted while checking task is not awaiting");
        } catch (ExecutionException e) {
            fail("Unexpected exception while checking task is awaiting", e);
        } catch (TimeoutException e) {
            // Expected
        }
    }

    /**
     * Assert that no task waits on any of a set of barriers within EXPECTED_FAIL_TIME_MS
     * 
     * @param barriers
     *            the barriers to check
     */
    public static void assertAllNotAwaiting(Collection<? extends Barrier> barriers) {
        try {
            Thread.sleep(EXPECTED_FAIL_TIME_MS);
        } catch (InterruptedException e) {
            fail("Interrupted while checking tasks are not awaiting", e);
        }

        for (Barrier barrier : barriers) {
            assertFalse(barrier.isWaitingFuture.isDone(), "Task is waiting");
        }
    }

}
