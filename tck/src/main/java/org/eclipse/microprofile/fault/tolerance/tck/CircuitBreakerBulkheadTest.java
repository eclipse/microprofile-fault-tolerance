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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithAsyncBulkhead;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithAsyncBulkheadNoFail;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithSyncBulkhead;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class CircuitBreakerBulkheadTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerBulkhead.jar")
                .addClasses(CircuitBreakerClientWithAsyncBulkhead.class,
                        CircuitBreakerClientWithSyncBulkhead.class,
                        CircuitBreakerClientWithAsyncBulkheadNoFail.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftCircuitBreakerBulkhead.war")
                .addAsLibrary(testJar);
        return war;
    }

    @Inject
    private CircuitBreakerClientWithAsyncBulkhead asyncBulkheadClient;

    @Inject
    private CircuitBreakerClientWithSyncBulkhead syncBulkheadClient;

    @Inject
    private CircuitBreakerClientWithAsyncBulkheadNoFail asyncBulkheadNoFailClient;

    /**
     * A test to ensure that the CircuitBreaker is checked before entering the Bulkhead and that BulkheadExceptions
     * count as failures for the CircuitBreaker.
     * 
     * Uses an asynchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0, delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour ========= ========= 1 Fill Bulkhead 2 Fill Bulkhead 3 BulkheadException 4 BulkheadException 5
     * BulkheadException 6 CircuitBreakerOpenException 7 CircuitBreakerOpenException
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     * @throws TimeoutException
     *             if waiting for a result takes too long
     * @throws ExecutionException
     *             if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreakerAroundBulkheadAsync()
            throws InterruptedException, ExecutionException, TimeoutException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> task1 = taskManager.runAsyncBarrierTask(asyncBulkheadClient::test);
            task1.assertAwaits();

            BarrierTask<?> task2 = taskManager.runAsyncBarrierTask(asyncBulkheadClient::test);
            task2.assertNotAwaiting();

            // While circuit closed, we get a BulkheadException
            for (int i = 3; i < 6; i++) {
                BarrierTask<?> taski = taskManager.runAsyncBarrierTask(asyncBulkheadClient::test);
                taski.assertThrows(BulkheadException.class);
            }

            // While circuit closed, we get a BulkheadException
            for (int i = 6; i < 8; i++) {
                BarrierTask<?> taski = taskManager.runAsyncBarrierTask(asyncBulkheadClient::test);
                taski.assertThrows(CircuitBreakerOpenException.class);
            }

            task1.openBarrier();
            task1.assertSuccess();
            task2.openBarrier();
            task2.assertSuccess();
        }
    }

    /**
     * A test to ensure that the CircuitBreaker is checked before entering the Bulkhead and that BulkheadExceptions
     * count as failures for the CircuitBreaker.
     * 
     * Uses a synchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0, delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour ========= ========= 1 Fill Bulkhead 2 BulkheadException 3 BulkheadException 4
     * BulkheadException 5 CircuitBreakerOpenException 6 CircuitBreakerOpenException
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     * @throws TimeoutException
     *             if waiting for a result takes too long
     * @throws ExecutionException
     *             if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreakerAroundBulkheadSync()
            throws InterruptedException, ExecutionException, TimeoutException {
        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> task1 = taskManager.runBarrierTask(syncBulkheadClient::test);
            task1.assertAwaits();

            // While circuit closed, we get a BulkheadException
            for (int i = 2; i < 5; i++) {
                BarrierTask<?> taski = taskManager.runBarrierTask(syncBulkheadClient::test);
                taski.assertThrows(BulkheadException.class);
            }

            // After circuit opens, we get CircuitBreakerOpenException
            for (int i = 5; i < 7; i++) {
                BarrierTask<?> taski = taskManager.runBarrierTask(syncBulkheadClient::test);
                taski.assertThrows(CircuitBreakerOpenException.class);
            }

            task1.openBarrier();
            task1.assertSuccess();
        }
    }

    /**
     * A test to ensure that the CircuitBreaker does not open in response to a BulkheadException if {@code failOn} does
     * not include BulkheadException
     * 
     * Uses an asynchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0, delay = 50000, failOn=TestException the expected behaviour
     * is,
     * 
     * Execution Behaviour ========= ========= 1 Fill Bulkhead 2 Fill Bulkhead 3 BulkheadException 4 BulkheadException 5
     * BulkheadException 6 BulkheadException 7 BulkheadException
     * 
     * @throws InterruptedException
     *             if the test is interrupted
     * @throws TimeoutException
     *             if waiting for a result takes too long
     * @throws ExecutionException
     *             if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreaker() throws InterruptedException, ExecutionException, TimeoutException {

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<?> task1 = taskManager.runAsyncBarrierTask(asyncBulkheadNoFailClient::test);
            task1.assertAwaits();

            BarrierTask<?> task2 = taskManager.runAsyncBarrierTask(asyncBulkheadNoFailClient::test);
            task2.assertNotAwaiting();

            // While circuit closed, we get a BulkheadException
            // Circuit should not open because failOn does not include BulkheadException
            for (int i = 3; i < 8; i++) {
                BarrierTask<?> taski = taskManager.runAsyncBarrierTask(asyncBulkheadNoFailClient::test);
                taski.assertThrows(BulkheadException.class);
            }

            task1.openBarrier();
            task1.assertSuccess();
            task2.openBarrier();
            task2.assertSuccess();
        }
    }

}
