/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithAsyncBulkhead;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithAsyncBulkheadNoFail;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithSyncBulkhead;
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

public class CircuitBreakerBulkheadTest extends Arquillian {

    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerBulkhead.jar")
                        .addClasses(CircuitBreakerClientWithAsyncBulkhead.class,
                                    CircuitBreakerClientWithSyncBulkhead.class,
                                    CircuitBreakerClientWithAsyncBulkheadNoFail.class)
                        .addPackage(BulkheadTask.class.getPackage())
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
     * A test to ensure that the CircuitBreaker is checked before entering the
     * Bulkhead and that BulkheadExceptions count as failures for the
     * CircuitBreaker.
     * 
     * Uses an asynchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0,
     * delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour
     * ========= =========
     * 1 Fill Bulkhead
     * 2 Fill Bulkhead
     * 3 BulkheadException
     * 4 BulkheadException
     * 5 BulkheadException
     * 6 CircuitBreakerOpenException
     * 7 CircuitBreakerOpenException
     * 
     * @throws InterruptedException if the test is interrupted 
     * @throws TimeoutException if waiting for a result takes too long
     * @throws ExecutionException if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreakerAroundBulkheadAsync() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncBulkheadTask task1 = new AsyncBulkheadTask();
        Future result1 = asyncBulkheadClient.test(task1);
        task1.assertStarting(result1);
        
        AsyncBulkheadTask task2 = new AsyncBulkheadTask();
        Future result2 = asyncBulkheadClient.test(task2);
        task2.assertNotStarting();
        
        // While circuit closed, we get a BulkheadException
        for (int i = 3; i < 6; i++) {
            AsyncBulkheadTask taski = new AsyncBulkheadTask();
            Future resulti = asyncBulkheadClient.test(taski);
            expect(BulkheadException.class, resulti);
        }
        
        // After circuit opens, we get CircuitBreakerOpenException
        for (int i = 6; i < 8; i++) {
            AsyncBulkheadTask taski = new AsyncBulkheadTask();
            Future resulti = asyncBulkheadClient.test(taski);
            expect(CircuitBreakerOpenException.class, resulti);
        }
        
        // Tidy Up, complete task1 and 2, check task 2 starts
        task1.complete(CompletableFuture.completedFuture("OK"));
        task2.complete(CompletableFuture.completedFuture("OK"));
        task2.assertStarting(result2);
        
        // Check both tasks return results
        assertEquals(result1.get(2, SECONDS), "OK");
        assertEquals(result2.get(2, SECONDS), "OK");
    }
    
    /**
     * A test to ensure that the CircuitBreaker is checked before entering the
     * Bulkhead and that BulkheadExceptions count as failures for the
     * CircuitBreaker.
     * 
     * Uses a synchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0,
     * delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour
     * ========= =========
     * 1 Fill Bulkhead
     * 2 BulkheadException
     * 3 BulkheadException
     * 4 BulkheadException
     * 5 CircuitBreakerOpenException
     * 6 CircuitBreakerOpenException
     * 
     * @throws InterruptedException if the test is interrupted
     * @throws TimeoutException if waiting for a result takes too long
     * @throws ExecutionException if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreakerAroundBulkheadSync() throws InterruptedException, ExecutionException, TimeoutException {
        BulkheadTaskManager manager = new BulkheadTaskManager();
        
        try {
            BulkheadTask task1 = manager.startTask(syncBulkheadClient);
            task1.assertStarting();
            
            // While circuit closed, we get a BulkheadException
            for (int i = 2; i < 5; i++) {
                BulkheadTask taski = manager.startTask(syncBulkheadClient);
                taski.assertFinishing();
                expect(BulkheadException.class, taski.getResultFuture());
            }
            
            // After circuit opens, we get CircuitBreakerOpenException
            for (int i = 5; i < 7; i++) {
                BulkheadTask taski = manager.startTask(syncBulkheadClient);
                taski.assertFinishing();
                expect(CircuitBreakerOpenException.class, taski.getResultFuture());
            }
            
            // Tidy up, complete task1 and check result
            task1.complete(CompletableFuture.completedFuture("OK"));
            task1.assertFinishing();
            assertEquals(task1.getResult().get(2, TimeUnit.MINUTES), "OK");
        }
        finally {
            manager.cleanup();
        }
    }
    
    /**
     * A test to ensure that the CircuitBreaker does not open in response to a
     * BulkheadException if {@code failOn} does not include BulkheadException
     * 
     * Uses an asynchronous bulkhead
     * 
     * With requestVolumeThreshold = 3, failureRatio = 1.0,
     * delay = 50000, failOn=TestException the expected behaviour is,
     * 
     * Execution Behaviour
     * ========= =========
     * 1 Fill Bulkhead
     * 2 Fill Bulkhead
     * 3 BulkheadException
     * 4 BulkheadException
     * 5 BulkheadException
     * 6 BulkheadException
     * 7 BulkheadException
     * 
     * @throws InterruptedException if the test is interrupted
     * @throws TimeoutException     if waiting for a result takes too long
     * @throws ExecutionException   if an async method throws an unexpected exception
     */
    @Test
    public void testCircuitBreaker() throws InterruptedException, ExecutionException, TimeoutException {
        List<AsyncBulkheadTask> tasks = new ArrayList<>();
        try {
            AsyncBulkheadTask task1 = new AsyncBulkheadTask();
            tasks.add(task1);
            Future result1 = asyncBulkheadNoFailClient.test(task1);
            task1.assertStarting(result1);
            
            AsyncBulkheadTask task2 = new AsyncBulkheadTask();
            tasks.add(task2);
            Future result2 = asyncBulkheadNoFailClient.test(task2);
            task2.assertNotStarting();
            
            // While circuit closed, we get a BulkheadException
            // Circuit should not open because failOn does not include BulkheadException
            for (int i = 3; i < 8; i++) {
                AsyncBulkheadTask taski = new AsyncBulkheadTask();
                tasks.add(taski);
                Future resulti = asyncBulkheadNoFailClient.test(taski);
                expect(BulkheadException.class, resulti);
            }
            
            // Tidy Up, complete task1 and 2, check task 2 starts
            task1.complete(CompletableFuture.completedFuture("OK"));
            task2.complete(CompletableFuture.completedFuture("OK"));
            task2.assertStarting(result2);
            
            // Check both tasks return results
            assertEquals(result1.get(2, SECONDS), "OK");
            assertEquals(result2.get(2, SECONDS), "OK");
        }
        finally {
            for (AsyncBulkheadTask task : tasks) {
                task.complete();
            }
        }
    }

}
