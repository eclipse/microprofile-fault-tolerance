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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;
import org.testng.Assert;

/**
 * A backend that is tailored to circuit breaker and bulkhead testing
 * @author Gordon Hutchison
 */
public class CBBulkheadBackend implements BackendTestDelegate {

    protected int millis = 1;
    private int instanceId;
    private boolean completed = false;
    private static boolean[] failSequence = null;

    protected static AtomicInteger workers = new AtomicInteger(0);
    protected static AtomicInteger maxSimultaneousWorkers = new AtomicInteger(0);
    protected static AtomicInteger instances = new AtomicInteger(0);
    protected static AtomicInteger tasksScheduled = new AtomicInteger(0);
    protected static int expectedInstances;
    protected static int expectedMaxSimultaneousWorkers;
    protected static int expectedTasksScheduled;
    private static boolean maxFill = true;
    private static int expectedTasksCompleted;
    private static AtomicInteger tasksCompleted = new AtomicInteger(0);

    public static int getTasksCompleted() {
        return tasksCompleted.get();
    }

    /**
     * @param sleepMillis
     * @param fails an array which is cycled round and used to stimulate failures
     */
    public CBBulkheadBackend(int sleepMillis, boolean[] fails) {
        failSequence = fails;
        this.millis = sleepMillis;
        instanceId = instances.incrementAndGet();
    }

    /*
     * Work this is the method that simulates the backend work inside the
     * Bulkhead.
     * 
     * @see org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.
     * BulkheadTestAction#perform()
     */
    @Override
    public Future<String> perform() throws InterruptedException {
        try {
            int taskId = tasksScheduled.incrementAndGet();
            int now = workers.incrementAndGet();
            int max = maxSimultaneousWorkers.get();

            while ((now > max) && !maxSimultaneousWorkers.compareAndSet(max, now)) {
                max = maxSimultaneousWorkers.get();
            }

            int index = taskId % failSequence.length;
            boolean fail = failSequence[index];
            if (fail) {
                RuntimeException e = new RuntimeException(
                        "Task: " + taskId + " fake error for CircuitBreaker testing");
                Utils.log("                       CircuitBreaker " + e.toString());
                throw e;
            }
            else {
                Utils.log("                                              CircuitBreaker Task " + taskId + " planing to succeed");
            }

            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Utils.log(e.toString());
        }
        finally {
            workers.decrementAndGet();
        }
        CompletableFuture<String> result = new CompletableFuture<>();
        completed = true;
        result.complete(this.toString());
        tasksCompleted.incrementAndGet();
        return result;
    }

    public String toString() {
        return "Instance " + instanceId + " completed: " + this.completed;
    }

    /**
     * Prepare the state for the next test
     */
    public static void reset() {
        instances.set(0);
        workers.set(0);
        maxSimultaneousWorkers.set(0);
        tasksScheduled.set(0);
        tasksCompleted.set(0);
        maxFill = true;
    }

    /**
     * Check the test ran successfully
     */
    public static void check() {
        Assert.assertEquals(workers.get(), 0, "Some workers still active. ");

        Assert.assertFalse(expectedInstances != 0 && instances.get() < expectedInstances,
                " Not all workers launched. " + instances.get() + "/" + expectedInstances);

        Assert.assertTrue(
                expectedMaxSimultaneousWorkers == 0 || maxSimultaneousWorkers.get() <= expectedMaxSimultaneousWorkers,
                " Bulkhead appears to have been breeched " + maxSimultaneousWorkers + " workers, expected "
                        + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedMaxSimultaneousWorkers > 1 && maxSimultaneousWorkers.get() == 1,
                " Workers are not in parrallel. ");
        Assert.assertTrue(
                !maxFill || expectedMaxSimultaneousWorkers == 0
                        || expectedMaxSimultaneousWorkers == maxSimultaneousWorkers.get(),
                " Work is not being done simultaneously enough, only " + maxSimultaneousWorkers + " "
                        + " workers at once. Expecting " + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedTasksScheduled != 0 && tasksScheduled.get() < expectedTasksScheduled,
                " Some tasks are missing, expected " + expectedTasksScheduled + " got " + tasksScheduled.get() + ". ");

        Assert.assertTrue(expectedTasksCompleted == 0 || expectedTasksCompleted == tasksCompleted.get(),
                " Expected work is not being completed " + tasksCompleted.get() + "/" + expectedTasksCompleted);

        Utils.log("Checks passed: " + "tasks: " + tasksScheduled + "/" + expectedTasksScheduled + ", bulkhead: "
                + maxSimultaneousWorkers + "/" + expectedMaxSimultaneousWorkers);
    }

    public static int getWorkers() {
        return workers.get();
    }

    public static void setExpectedTasksScheduled(int expected) {
        expectedTasksScheduled = expected;
    }

    public static void setExpectedInstances(int expectedInstances) {
        CBBulkheadBackend.expectedInstances = expectedInstances;
    }

    public static void setExpectedMaxWorkers(int expectedMaxWorkers) {
        CBBulkheadBackend.expectedMaxSimultaneousWorkers = expectedMaxWorkers;
    }

    public static void setExpectedMaxWorkers(int maxSimultaneousWorkers, boolean b) {
        setExpectedMaxWorkers(maxSimultaneousWorkers);
        maxFill = b;
    }

    public static void setExpectedTasksCompleted(int i) {
        CBBulkheadBackend.expectedTasksCompleted = i;
    }

    public static void setFailSequence(boolean[] bs) {
        CBBulkheadBackend.failSequence = bs;
    }

}
