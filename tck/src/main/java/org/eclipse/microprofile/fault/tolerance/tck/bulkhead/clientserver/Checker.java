/**********************************************************************
* Copyright (c) 2017 Contributors to the Eclipse Foundation 
*
* See the NOTICES file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* All rights reserved. This program and the accompanying materials 
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

    private int millis = 1;
    private static AtomicInteger workers = new AtomicInteger(0);
    private static AtomicInteger maxSimultaneousWorkers = new AtomicInteger(0);
    private static AtomicInteger instances = new AtomicInteger(0);
    private static AtomicInteger tasksScheduled = new AtomicInteger(0);
    private static int expectedInstances;
    private static int expectedMaxSimultaneousWorkers;
    private static int expectedTasksScheduled;


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

    /*
     * Work this is the method that simulates the backend work inside the
     * Bulkhead.
     * 
     * @see org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.
     * BulkheadTestAction#perform()
     */
    @Override
    public Future<String> perform() {
        try {
            int taskId = tasksScheduled.incrementAndGet();
            int now = workers.incrementAndGet();
            int max = maxSimultaneousWorkers.get();

            while ((now > max) && !maxSimultaneousWorkers.compareAndSet(max, now)) {
                max = maxSimultaneousWorkers.get();
            }

            Utils.log("Task " + taskId + " sleeping for " + millis + " milliseconds. " + now + " workers from "
                    + instances + " instances " + BAR.substring(0, now));
            Thread.sleep(millis);

            Utils.log("woke");
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
    }

    /**
     * Check the test ran successfully
     */
    public static void check() {
        Assert.assertEquals(workers.get(), 0, "Some workers still active. ");
        Assert.assertEquals(instances.get(), expectedInstances, " Not all workers launched. ");
        Assert.assertTrue(maxSimultaneousWorkers.get() <= expectedMaxSimultaneousWorkers,
                " Bulkhead appears to have been breeched " + maxSimultaneousWorkers.get() + " workers, expected "
                        + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedMaxSimultaneousWorkers > 1 && maxSimultaneousWorkers.get() == 1,
                " Workers are not in parrallel. ");
        Assert.assertTrue(expectedMaxSimultaneousWorkers == maxSimultaneousWorkers.get(),
                " Work is not being done simultaneously enough, only " + maxSimultaneousWorkers + ". "
                        + " workers are once. Expecting " + expectedMaxSimultaneousWorkers + ". ");
        Assert.assertFalse(expectedTasksScheduled != 0 && tasksScheduled.get() < expectedTasksScheduled,
                " Some tasks are missing, expected " + expectedTasksScheduled + " got " + tasksScheduled.get() + ". ");

        Utils.log("Checks passed");
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
}
