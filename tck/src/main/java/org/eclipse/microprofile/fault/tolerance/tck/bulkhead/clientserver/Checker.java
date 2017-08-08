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

import javax.management.RuntimeErrorException;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;
import org.testng.Assert;

/**
 * A simple sleeping test backend worker. Having this backend as a delegate
 * means that we can perform more than one kind of test using a common
 * 
 * @Injected object that delegates to one of these that is passed in as a
 *           parameter to the business method.
 * 
 *           There are a number of tests that this backend can perform:
 *           <ul>
 *           <li>expected number of instances created
 *           <li>expected workers started via perform method
 *           <li>max simultaneous workers not exceeded
 *           </ul>
 * 
 * @author Gordon Hutchison
 */
public class Checker implements BackendTestDelegate {

    protected int millis = 1;
    private int fails = 0;
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

    /*
     * This string is used for varying substr's barcharts in the log, for
     * example for the number of concurrent workers.
     */
    static final String BAR = "**************************************************************************************+++";

    /**
     * Constructor
     * 
     * @param i
     *            how long to sleep for in milliseconds
     */
    public Checker(int sleepMillis) {
        millis = sleepMillis;
        instances.incrementAndGet();
    }

    public Checker(int sleepMillis, int fails) {
        this.fails = fails;
        this.millis = sleepMillis;
        instances.incrementAndGet();
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
            if (fails > 0) {
                Thread.sleep(millis/2);
                fails--;
                RuntimeErrorException e = new RuntimeErrorException(new Error("fake error for Retry Testing"));
                Utils.log(e.toString());
                throw e;
            }

            Utils.log("Task " + taskId + " sleeping for " + millis + " milliseconds. " + now
                    + " workers inside Bulkhead from " + instances + " instances " + BAR.substring(0, now));
            Thread.sleep(millis);

            Utils.log("Task " + taskId + " woke.");
        }
        catch (InterruptedException e) {
            Utils.log(e.toString());
        }
        finally {
            workers.decrementAndGet();
        }
        CompletableFuture<String> result = new CompletableFuture<>();
        result.complete("max workers was " + maxSimultaneousWorkers.get());
        return result;
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
                " Not all workers launched. " + instances.get() +"/"+expectedInstances );

        Assert.assertTrue(maxSimultaneousWorkers.get() <= expectedMaxSimultaneousWorkers,
                " Bulkhead appears to have been breeched " + maxSimultaneousWorkers + " workers, expected "
                        + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedMaxSimultaneousWorkers > 1 && maxSimultaneousWorkers.get() == 1,
                " Workers are not in parrallel. ");
        Assert.assertTrue(!maxFill || expectedMaxSimultaneousWorkers == maxSimultaneousWorkers.get(),
                " Work is not being done simultaneously enough, only " + maxSimultaneousWorkers + " "
                        + " workers at once. Expecting " + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedTasksScheduled != 0 && tasksScheduled.get() < expectedTasksScheduled,
                " Some tasks are missing, expected " + expectedTasksScheduled + " got " + tasksScheduled.get() + ". ");

        Assert.assertTrue(expectedTasksCompleted==0 || expectedTasksCompleted == tasksCompleted.get(),
                " Expected work is not being completed " + tasksCompleted.get() + "/" + expectedTasksCompleted );
  
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
        Checker.expectedInstances = expectedInstances;
    }

    public static void setExpectedMaxWorkers(int expectedMaxWorkers) {
        Checker.expectedMaxSimultaneousWorkers = expectedMaxWorkers;
    }

    public static void setExpectedMaxWorkers(int maxSimultaneousWorkers, boolean b) {
        setExpectedMaxWorkers(maxSimultaneousWorkers);
        maxFill = b;
    }

    public static void setExpectedTasksCompleted(int i) {
       Checker.expectedTasksCompleted = i;
    }

}
