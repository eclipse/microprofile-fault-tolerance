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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;
import org.testng.Assert;

/**
 * A class to hold a set of data that is has the scope of a test.
 * 
 * @author Gordon Hutchison
 */
public class TestData {

    private AtomicInteger workers = new AtomicInteger(0);
    private AtomicInteger maxSimultaneousWorkers = new AtomicInteger(0);
    private AtomicInteger instances = new AtomicInteger(0);
    private AtomicInteger tasksScheduled = new AtomicInteger(0);
    private int expectedInstances;
    private int expectedMaxSimultaneousWorkers;
    private int expectedTasksScheduled;
    private boolean maxFill = true;
    private CountDownLatch latch = null;

    public TestData(CountDownLatch countDownLatch) {
        latch = countDownLatch;
    }

    public TestData() {
    }

    public AtomicInteger getWorkers() {
        return workers;
    }

    public void setWorkers(AtomicInteger workers) {
        this.workers = workers;
    }

    public AtomicInteger getMaxSimultaneousWorkers() {
        return maxSimultaneousWorkers;
    }

    public void setMaxSimultaneousWorkers(AtomicInteger maxSimultaneousWorkers) {
        this.maxSimultaneousWorkers = maxSimultaneousWorkers;
    }

    public AtomicInteger getInstances() {
        return instances;
    }

    public void setInstances(AtomicInteger instances) {
        this.instances = instances;
    }

    public AtomicInteger getTasksScheduled() {
        return tasksScheduled;
    }

    public void setTasksScheduled(AtomicInteger tasksScheduled) {
        this.tasksScheduled = tasksScheduled;
    }

    public int getExpectedInstances() {
        return expectedInstances;
    }

    public void setExpectedInstances(int expectedInstances) {
        this.expectedInstances = expectedInstances;
    }

    public int getExpectedMaxSimultaneousWorkers() {
        return expectedMaxSimultaneousWorkers;
    }

    public void setExpectedMaxSimultaneousWorkers(int expectedMaxSimultaneousWorkers) {
        this.expectedMaxSimultaneousWorkers = expectedMaxSimultaneousWorkers;
    }

    public int getExpectedTasksScheduled() {
        return expectedTasksScheduled;
    }

    public boolean isMaxFill() {
        return maxFill;
    }

    public void setMaxFill(boolean maxFill) {
        this.maxFill = maxFill;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void setExpectedTasksScheduled(int expected) {
        if (tasksScheduled.get() != 0 && // test being set up
                expectedTasksScheduled != tasksScheduled.get()) {
            Utils.log("WARNING: expectedTasksScheduled being set to " + expected + " while tasksScheduled was "
                    + tasksScheduled.get() + " this will make this check likely to FAIL.");
        }
        this.expectedTasksScheduled = expected;
    }

    /**
     * Check the test ran successfully.
     * Note: It is not safe to have checks of actual results here as they are RETURNED
     * from the workers 'perform' methods and these decrement the latch used below
     * just BEFORE returning.
     */
    public void check() {
        
        if( latch != null ) {
            try {
                latch.await(50000, TimeUnit.MILLISECONDS);
                Assert.assertEquals(getWorkers().get(), 0, "Some workers still active. ");
            }
            catch (InterruptedException e) {
                Utils.log(e.getLocalizedMessage());
            }
        }
        

        Assert.assertFalse(getExpectedInstances() != 0 && getInstances().get() < getExpectedInstances(),
                " Not all workers launched. " + getInstances().get() + "/" + getExpectedInstances());

        Assert.assertTrue(getMaxSimultaneousWorkers().get() <= getExpectedMaxSimultaneousWorkers(),
                " Bulkhead appears to have been breeched " + getMaxSimultaneousWorkers() + " workers, expected "
                        + getExpectedMaxSimultaneousWorkers() + ". ");
        Assert.assertFalse(isMaxFill() && getExpectedMaxSimultaneousWorkers() > 1 && getMaxSimultaneousWorkers().get() == 1,
                " Workers are not in parrallel. ");
        Assert.assertTrue(
                !isMaxFill() || getExpectedMaxSimultaneousWorkers() == getMaxSimultaneousWorkers().get(),
                " Work is not being done simultaneously enough, only " + getMaxSimultaneousWorkers() + " "
                        + " workers at once. Expecting " + getExpectedMaxSimultaneousWorkers() + ". ");
        Assert.assertFalse(
                getExpectedTasksScheduled() != 0 && getTasksScheduled().get() < getExpectedTasksScheduled(),
                " Some tasks are missing, expected " + getExpectedTasksScheduled() + " got "
                        + getTasksScheduled().get() + ". ");

        Utils.log("Checks passed: " + "tasks: " + getTasksScheduled() + "/" + getExpectedTasksScheduled()
                + ", bulkhead: " + getMaxSimultaneousWorkers() + "/" + getExpectedMaxSimultaneousWorkers());
    }

}
