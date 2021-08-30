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
import static java.util.stream.Collectors.toList;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Barrier.EXPECTED_FAIL_TIME_MS;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Barrier.WAIT_TIME_MS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.hamcrest.Matcher;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Utility for running concurrent tasks which need to wait
 * <p>
 * This class takes care of creating {@link Barrier Barriers} and {@link BarrierTask BarrierTasks} and ensuring that
 * they get cleaned up.
 * <p>
 * This class implements {@link AutoCloseable} so that it can be used in a try-with-resources block to make it easy to
 * ensure everything from your test is cleaned up.
 * <p>
 * Example:
 * 
 * <pre>
 * <code>
 * try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
 *     BarrierTask&lt;Void&gt; task = taskManager.runBarrierTask(bean::testMethod);
 *     task.assertNotAwaiting();
 *     task.release();
 *     task.assertSuccess();
 * }
 * </code>
 * </pre>
 * <p>
 * Example test method:
 * 
 * <pre>
 * <code>
 * public void testMethod(Barrier barrier) {
 *     barrier.await();
 *     return "OK"
 * }
 * </code>
 * </pre>
 */
public class AsyncTaskManager implements AutoCloseable {

    private List<BarrierTask<?>> startedTasks = Collections.synchronizedList(new ArrayList<>());
    private List<Barrier> createdNonTaskBarriers = Collections.synchronizedList(new ArrayList<>());

    /**
     * Run a task which awaits on a barrier
     * <p>
     * Since the task is assumed to run synchronously and expected to await the barrier, {@link AsyncCaller} is used to
     * call the task asynchronously.
     * <p>
     * The returned {@link BarrierTask} can be used to release the barrier and assert the result of the task.
     * 
     * @param task
     *            the task to run
     * @return the BarrierTask
     */
    public BarrierTask<Void> runBarrierTask(Consumer<Barrier> task) {
        Barrier barrier = new Barrier();
        Future<Void> future = getExecutor().run(() -> {
            task.accept(barrier);
        });

        BarrierTask<Void> taskImpl = new BarrierTask<>(future, barrier);
        startedTasks.add(taskImpl);
        return taskImpl;
    }

    /**
     * Run an asynchronous task which awaits a barrier
     * <p>
     * The task is assumed to run asynchronously and return a {@link Future} with its result, either by being a method
     * annotated with {@link Asynchronous} or by some other means.
     * <p>
     * The returned {@link BarrierTask} can be used to release the barrier and assert the result of the task.
     * 
     * @param <T>
     *            the return type of the task
     * @param task
     *            the task to run
     * @return the BarrierTask
     */
    public <T> BarrierTask<T> runAsyncBarrierTask(Function<Barrier, Future<? extends T>> task) {
        Barrier barrier = new Barrier();
        Future<? extends T> result = task.apply(barrier);

        BarrierTask<T> taskImpl = new BarrierTask<>(result, barrier);
        startedTasks.add(taskImpl);
        return taskImpl;
    }

    /**
     * Run an asynchronous task which awaits a barrier
     * <p>
     * The task is assumed to run asynchronously and return a {@link CompletionStage} with its result, either by being a
     * method annotated with {@link Asynchronous} or by some other means.
     * <p>
     * The returned {@link BarrierTask} can be used to release the barrier and assert the result of the task.
     * 
     * @param <T>
     *            the return type of the task
     * @param task
     *            the task to run
     * @return the BarrierTask
     */
    public <T> BarrierTask<T> runAsyncCsBarrierTask(Function<Barrier, CompletionStage<? extends T>> task) {
        Barrier barrier = new Barrier();
        Future<? extends T> result = CompletableFutureHelper.toCompletableFuture(task.apply(barrier));

        BarrierTask<T> taskImpl = new BarrierTask<>(result, barrier);
        startedTasks.add(taskImpl);
        return taskImpl;
    }

    /**
     * Create a {@link Barrier} not associated with any task
     * <p>
     * The AsyncTaskManager will ensure that {@link Barrier#open()} is called when {@link AsyncTaskManager#close()} is
     * called.
     * 
     * @return the newly created {@code Barrier}
     */
    public Barrier newBarrier() {
        Barrier barrier = new Barrier();
        createdNonTaskBarriers.add(barrier);
        return barrier;
    }

    /**
     * Close the AsyncTaskManager, opening all barriers and ensuring that all tasks complete
     */
    public void close() {
        for (BarrierTask<?> task : startedTasks) {
            task.openBarrier();
        }

        for (Barrier barrier : createdNonTaskBarriers) {
            barrier.open();
        }

        try {
            for (BarrierTask<?> task : startedTasks) {
                task.assertCompletes();
            }
        } finally {
            startedTasks.clear();
            createdNonTaskBarriers.clear();
        }
    }

    /**
     * Assert that multiple tasks do not wait on their barriers
     * <p>
     * This method always takes EXPECTED_FAIL_TIME_MS ms.
     * <p>
     * This method is quicker than calling {@link BarrierTask#assertNotAwaiting()} if you need to assert multiple tasks
     * at the same time.
     */
    public static void assertAllNotAwaiting(Collection<? extends BarrierTask<?>> tasks) {
        Collection<Barrier> barriers = tasks.stream()
                .map(t -> (BarrierTask<?>) t)
                .map(t -> t.barrier)
                .collect(toList());

        Barrier.assertAllNotAwaiting(barriers);
    }

    /**
     * A task which runs using a barrier
     * <p>
     * Use this interface to check that the task waits on the barrier and completes as expected
     * 
     * @param <T>
     *            the return type of the task
     */
    public static class BarrierTask<T> {

        private final Future<? extends T> result;
        private final Barrier barrier;

        public BarrierTask(Future<? extends T> result, Barrier barrier) {
            this.result = result;
            this.barrier = barrier;
        }

        /**
         * Open the barrier used by the task
         */
        public void openBarrier() {
            barrier.open();
        }

        /**
         * Assert that the task waits on its barrier within WAIT_TIME_MS
         */
        public void assertAwaits() {
            barrier.assertAwaits();
        }

        /**
         * Assert that the task does not wait on its barrier
         * <p>
         * This method always takes EXPECTED_FAIL_TIME_MS ms.
         * <p>
         * If you need to check multiple tasks, use {@link AsyncTaskManager#assertAllNotAwaiting(Collection)} instead.
         */
        public void assertNotAwaiting() {
            barrier.assertNotAwaiting();
        }

        /**
         * Assert that the task completes within WAIT_TIME_MS
         */
        public void assertCompletes() {
            try {
                getResult();
                // Completed successfully
            } catch (ExecutionException e) {
                // Completed exceptionally

                // Check for completion with an error
                // Mostly to catch cases where an assertion error is thrown by the task
                if (e.getCause() instanceof Error) {
                    fail("Task completed but with an error", e.getCause());
                }
            }
        }

        /**
         * Assert that the task does not complete within EXPECTED_FAIL_TIME_MS
         */
        public void assertNotCompleting() {
            try {
                T r = result.get(EXPECTED_FAIL_TIME_MS, MILLISECONDS);
                fail("Task completed with result: " + r);
            } catch (ExecutionException e) {
                fail("Task completed with exception", e);
            } catch (InterruptedException e) {
                fail("Interrupted while checking task does not complete", e);
            } catch (TimeoutException e) {
                // Expected
            }
        }

        /**
         * Assert that the task completes without exception within WAIT_TIME_MS
         */
        public void assertSuccess() {
            assertResult(is(anything()));
        }

        /**
         * Assert that the task completes, throwing an exception of the specified class within WAIT_TIME_MS
         * 
         * @param exceptionClass
         *            the class of the expected exception
         */
        public void assertThrows(Class<? extends Throwable> exceptionClass) {
            try {
                getResult();
                fail("Task did not throw an exception");
            } catch (ExecutionException e) {
                assertThat(e.getCause(), instanceOf(exceptionClass));
            }
        }

        /**
         * Assert that the task completes without exception within WAIT_TIME_MS and that the value returned is matched
         * by the provided matcher
         * 
         * @param matcher
         *            the {@link Matcher} used to assert the returned value
         */
        public void assertResult(Matcher<? super T> matcher) {
            try {
                T result = getResult();
                assertThat(result, matcher);
            } catch (ExecutionException e) {
                fail("Task threw exception", e);
            }
        }

        /**
         * Get the result of the task
         * <p>
         * Asserts that the task completes within WAIT_TIME_MS.
         * 
         * @return the task result
         * @throws ExecutionException
         *             if the task threw an exception instead of returning
         */
        public T getResult() throws ExecutionException {
            return getResult(WAIT_TIME_MS, MILLISECONDS);
        }

        /**
         * Get the result of the task
         * <p>
         * Asserts that the task completes within the given time
         * 
         * @param time
         *            the time to wait for the task to complete
         * @param unit
         *            the unit for {@code time}
         * @return the task result
         * @throws ExecutionException
         *             if the task threw an exception instead of returning
         */
        public T getResult(long time, TimeUnit unit) throws ExecutionException {
            try {
                return result.get(time, unit);
            } catch (InterruptedException e) {
                throw new AssertionError("Interrupted while waiting for result", e);
            } catch (TimeoutException e) {
                throw new AssertionError("Timed out while waiting for result", e);
            }
        }

        /**
         * Whether this task is awaiting its barrier right now
         * <p>
         * This method returns immediately.
         * 
         * @return {@code true} if this task is currently waiting on its barrier, otherwise {@code false}
         */
        public boolean isAwaiting() {
            return barrier.countWaiting() != 0;
        }

    }

    private AsyncCaller getExecutor() {
        return CDI.current().select(AsyncCaller.class).get();
    }

}
