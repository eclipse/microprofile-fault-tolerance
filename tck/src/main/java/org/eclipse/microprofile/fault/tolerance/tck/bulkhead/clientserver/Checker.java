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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.management.RuntimeErrorException;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;

/**
 * A simple sleeping test backend worker. Having this backend as a delegate
 * means that we can perform more than one kind of test using a common 
 * {@code @Inject}-ed object that delegates to one of these that is passed in as a
 * parameter to the business method.
 * 
 * @author Gordon Hutchison
 */
public class Checker implements BackendTestDelegate {

    private int millis = 1;
    private int fails = 0;
    private TestData td = null;
    /*
     * This string is used for varying substr's barcharts in the log, for
     * example for the number of concurrent workers.
     */
    static final String BAR = "**************************************************************************************+++";

    /**
     * Constructor
     * 
     * @param sleepMillis  how long to sleep for in milliseconds
     * @param td test data
     */
    public Checker(int sleepMillis, TestData td) {
        this.millis = sleepMillis;
        this.td = td;
        this.td.getInstances().incrementAndGet();
    }

    public Checker(int sleepMillis, TestData td, int fails) {
        this.fails = fails;
        this.millis = sleepMillis;
        this.td = td;
        this.td.getInstances().incrementAndGet();
    }

    /*
     * This is the method that simulates the backend work inside the
     * Bulkhead.
     * 
     * @see org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.
     * BulkheadTestAction#perform()
     */
    @Override
    public Future<String> perform() throws InterruptedException {
        try {
            int taskId = td.getTasksScheduled().incrementAndGet();
            int now = td.getWorkers().incrementAndGet();
            int max = td.getMaxSimultaneousWorkers().get();

            while ((now > max) && !td.getMaxSimultaneousWorkers().compareAndSet(max, now)) {
                max = td.getMaxSimultaneousWorkers().get();
            }
            if (fails > 0) {
                Thread.sleep(millis / 2);
                fails--;
                RuntimeErrorException e = new RuntimeErrorException(new Error("fake error for Retry Testing"));
                Utils.log(e.toString());

                // We will countDown the latch in the finally block

                throw e;
            }

            Utils.log("Task " + taskId + " sleeping for " + millis + " milliseconds. " + now
                    + " workers inside Bulkhead from " + td.getInstances() + " instances " + BAR.substring(0, now));

            Thread.sleep(millis);

            Utils.log("Task " + taskId + " woke.");

            // We will countDown the latch in the finally block

        }
        catch (InterruptedException e) {
            Utils.log(e.toString());
        }
        finally {

            // We want to decrement this before the latch
            td.getWorkers().decrementAndGet();

            CountDownLatch latch = td.getLatch();
            if (latch != null ) {
                latch.countDown();
            }

        }
        CompletableFuture<String> result = new CompletableFuture<>();
        result.complete("max workers was " + td.getMaxSimultaneousWorkers().get());
        return result;
    }

    /**
     * @return how long do we sleep for
     */
    public int getSleep(){
        return millis;
    }
}