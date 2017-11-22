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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.TestData;
import org.testng.Assert;

public class Utils {

    /**
     * Get the Thread ID
     * 
     * @return thread id
     */
    private static long tid() {
        return Thread.currentThread().getId();
    }

    /**
     * Common function to check the returned results of the tests
     * 
     * @param number number of futures in results
     * @param results futures of background processes
     */
    static void handleResults(int number, Future[] results) {
        try {
            boolean done = false;
            // Wait for all the backends to finish
            while (!done) {
                done = true;
                for (int i = 0; i < number; i++) {
                    boolean thisDone = (results[i] == null || results[i].get() == null || ((Future) results[i]).isDone()
                            || results[i].get() instanceof Future && ((Future) results[i].get()).isDone());
                    done = done && thisDone;
                    
                    log("Result for " + i + (thisDone ? " (Done)" : " (NotDone)") + " is "
                            + ((results[i] == null || results[i].get() == null)?"null":""+(((Future) results[i]).get() instanceof Future
                                    ? ((Future) ((Future) results[i]).get()).get() : ((Future) results[i]).get())));

                }
                Thread.sleep(1000);
            }
        }
        catch (Throwable e) {
            log(e.toString());
        }
    }

    /**
     * Run a number of Callable's (usually Asynch's) in a loop on one thread.
     * Here we do not check that amount that were successfully through the
     * Bulkhead
     * 
     * @param iterations number of iterations
     * @param test test instance
     * @param maxSimultaneousWorkers simultaneous workers
     * @param expectedTasksScheduled expected tasks scheduled
     * @param td - used to hold expected results
     */
    public static void loop(int iterations, BulkheadTestBackend test, int maxSimultaneousWorkers, int expectedTasksScheduled, TestData td ) {
        Future[] results = new Future[iterations];
        try {
            td.setExpectedMaxSimultaneousWorkers(maxSimultaneousWorkers);
            td.setExpectedInstances(iterations);
            td.setExpectedTasksScheduled(expectedTasksScheduled);

            try {
                for (int i = 0; i < iterations; i++) {
                    Utils.log("Starting test " + i);
                    results[i] = test.test(new Checker(1 * 1000, td));
                }
            }
            catch (InterruptedException e1) {
                Assert.fail("Unexpected interruption", e1);
            }
        }
        finally {
            Utils.handleResults(iterations, results);
        }
    }

    /**
     * A simple local logger. Messages are logged with a prefix of the threadId
     * and the current time.
     * 
     * @param s - message
     */
    public static void log(String s) {
        System.out.println(tid() + " " + hms() + ": " + s);
    }

    /**
     * Get the time in simple format
     * 
     * @return
     */
    private static String hms() {
        return DateTimeFormatter.ofPattern("HH:mm:ss:SS").format(LocalDateTime.now());
    }

    /**
     * @param millis how log to sleep
     */
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            log("woken" + e.getMessage());
        }
    }

    /**
     * Prevent instances being constructed
     */
    private Utils() {
    }

}
