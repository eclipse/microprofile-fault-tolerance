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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10ClassSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead10MethodSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3MethodSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3TaskQueueSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphoreDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodSemaphoreDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * @author Gordon Hutchison
 * @author Andrew Rouse
 */
public class BulkheadSynchTest extends Arquillian {

    /*
     * As the FaultTolerance annotation only work on business methods of injected objects we need to inject a variety of
     * these for use by the tests below. The naming convention indicates if the annotation is on a class or method,
     * asynchronous or semaphore based, the size/value of the {@code @Bulkhead} and whether we have queueing or not.
     */
    @Inject
    private BulkheadClassSemaphoreDefaultBean bhBeanClassSemaphoreDefault;
    @Inject
    private BulkheadMethodSemaphoreDefaultBean bhBeanMethodSemaphoreDefault;
    @Inject
    private Bulkhead3ClassSemaphoreBean bhBeanClassSemaphore3;
    @Inject
    private Bulkhead3MethodSemaphoreBean bhBeanMethodSemaphore3;
    @Inject
    private Bulkhead10ClassSemaphoreBean bhBeanClassSemaphore10;
    @Inject
    private Bulkhead10MethodSemaphoreBean bhBeanMethodSemaphore10;
    @Inject
    private Bulkhead3TaskQueueSemaphoreBean bhBeanTaskQueueSemaphore3;

    /**
     * This is the Arquillian deploy method that controls the contents of the war that contains all the tests.
     * 
     * @return the test war "ftBulkheadSynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchTest.jar")
                .addPackage(BulkheadClassSemaphoreDefaultBean.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        return ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchTest.war").addAsLibrary(testJar);
    }

    /**
     * Tests the class synchronous Bulkhead3. This test will check that 3 and no more than 3 parallel synchronous calls
     * are allowed into a method that is a member of a {@code @Bulkhead(3)} Class.
     */
    @Test
    public void testBulkheadClassSemaphore3() {
        testBulkhead(3, bhBeanClassSemaphore3::test);
    }

    /**
     * Tests the method synchronous Bulkhead3. This test will check that 3 and no more than 3 parallel synchronous calls
     * are allowed into a method that has an individual Bulkhead(3) annotation
     */
    @Test
    public void testBulkheadMethodSemaphore3() {
        testBulkhead(3, bhBeanMethodSemaphore3::test);
    }

    /**
     * Tests the class synchronous Bulkhead10. This test will check that 10 and no more than 10 parallel synchronous
     * calls are allowed into a method that is a member of a {@code @Bulkhead(10)} Class.
     */
    @Test
    public void testBulkheadClassSemaphore10() {
        testBulkhead(10, bhBeanClassSemaphore10::test);
    }

    /**
     * Tests the method synchronous Bulkhead10. This test will check that 10 and no more than 10 parallel synchronous
     * calls are allowed into a method that has an individual
     *
     * {@code @Bulkhead(10)} annotation
     */
    @Test
    public void testBulkheadMethodSemaphore10() {
        testBulkhead(10, bhBeanMethodSemaphore10::test);
    }

    /**
     * Tests the basic class synchronous Bulkhead. This test will check that 10 and no more than 10 parallel synchronous
     * calls are allowed into a method that is a member of a {@code @Bulkhead(10)} Class.
     */
    @Test
    public void testBulkheadClassSemaphoreDefault() {
        testBulkhead(10, bhBeanClassSemaphoreDefault::test);
    }

    /**
     * Tests the basic method synchronous Bulkhead with defaulting value parameter. This will check that more than 1 but
     * not more than 10 threads get into the bulkhead at once.
     */
    @Test
    public void testBulkheadMethodSemaphoreDefault() {
        testBulkhead(10, bhBeanMethodSemaphoreDefault::test);
    }

    /**
     * Test that the {@code waitingTaskQueue} parameter is ignored when {@code Bulkhead} is used without
     * {@code Asynchronous}.
     */
    public void testSemaphoreWaitingTaskQueueIgnored() {
        testBulkhead(3, bhBeanTaskQueueSemaphore3::test);
    }

    /**
     * Conducts a standard test to ensure that a synchronous bulkhead with no other annotations works correctly. It
     * asserts that the correct number of tasks are allowed to run and to queue and that when a task in the bulkhead
     * completes a new task can be run.
     * <p>
     * The {@code bulkheadMethod} should be a reference to a method annotated with {@link Bulkhead} which accepts a
     * {@code Barrier} and calls {@link Barrier#await()}.
     * 
     * @param maxRunning
     *            expected number of tasks permitted to run
     * @param bulkheadMethod
     *            a reference to the annotated method
     */
    public static void testBulkhead(int maxRunning, Consumer<Barrier> bulkheadMethod) {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {

            // Fill the bulkhead
            List<BarrierTask<?>> runningTasks = new ArrayList<>();
            for (int i = 0; i < maxRunning; i++) {
                BarrierTask<?> task = taskManager.runBarrierTask(bulkheadMethod);
                runningTasks.add(task);
            }

            // Check tasks start and await on the barrier
            for (int i = 0; i < maxRunning; i++) {
                runningTasks.get(i).assertAwaits();
            }

            // Check next task is rejected
            BarrierTask<?> overflowTask = taskManager.runBarrierTask(bulkheadMethod);
            overflowTask.assertThrows(BulkheadException.class);

            // Release one running task
            BarrierTask<?> releasedTask = runningTasks.get(7 % maxRunning); // Pick one out of the middle
            releasedTask.openBarrier();
            releasedTask.assertSuccess();

            // Now check that another task can be submitted and runs
            BarrierTask<?> extraTask = taskManager.runBarrierTask(bulkheadMethod);
            extraTask.assertAwaits();

            // Now check that next task is rejected
            BarrierTask<?> overflowTask2 = taskManager.runBarrierTask(bulkheadMethod);
            overflowTask2.assertThrows(BulkheadException.class);
        }
    }

}
