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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
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

/**
 * Test that the parameters of Bulkhead can be configured
 */
public class BulkheadConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .set(BulkheadConfigBean.class, "serviceValue", Bulkhead.class, "value", "1")
                .set(BulkheadConfigBean.class, "serviceWaitingTaskQueue", Bulkhead.class, "waitingTaskQueue", "1");
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadConfig.jar")
                                    .addClass(BulkheadConfigBean.class)
                                    .addPackage(Packages.UTILS)
                                    .addAsManifestResource(config, "microprofile-config.properties")
                                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadConfig.war")
                                   .addAsLibrary(jar);
        
        return war;
    }
    
    @Inject
    private BulkheadConfigBean bean;
    
    @Inject
    private AsyncCaller executor;
    
    @Test
    public void testConfigValue() throws Exception {
        // In annotation: value = 5
        // In config:     value = 1
        
        CompletableFuture<Void> waitingFuture = new CompletableFuture<>();
        Future<?> f1 = executor.submit(() -> {
            bean.serviceValue(waitingFuture);
            return null;
        });
        
        try {
            
            await("Task 1 starting").until(() -> bean.getTasksRunning(), is(1));
            
            Future<?> f2 = executor.submit(() -> {
                bean.serviceValue(waitingFuture);
                return null;
            });
            
            await("Task 2 starting").until(() -> f2.isDone() || bean.getTasksRunning() == 2);
            
            // Now that task 2 is either running or has given us an exception, we can release
            // all waiting tasks and check the result
            waitingFuture.complete(null);
            
            Exceptions.expect(BulkheadException.class, f2);
        }
        finally {
            waitingFuture.complete(null);
            f1.get(1, MINUTES);
        }
    }
    
    @Test
    public void testWaitingTaskQueue() throws Exception {
        // In annotation: waitingTaskQueue = 5
        //                value = 1
        // In config:     waitingTaskQueue = 1
        
        List<Future<?>> futures = new ArrayList<>();
        
        CompletableFuture<Void> waitingFuture = new CompletableFuture<>();
        Future<?> f1 = bean.serviceWaitingTaskQueue(waitingFuture);
        futures.add(f1);
        try {
            await("Task 1 starting").until(() -> bean.getTasksRunning(), is(1));
            
            Future<?> f2 = bean.serviceWaitingTaskQueue(waitingFuture);
            futures.add(f2);
            
            Future<?> f3 = bean.serviceWaitingTaskQueue(waitingFuture);
            
            // Check f3 is not started or queued
            await().until(() -> f3.isDone());
            Exceptions.expect(BulkheadException.class, f3);
        }
        finally {
            waitingFuture.complete(null);
            for (Future<?> future : futures) {
                future.get(1, MINUTES);
            }
        }
    }
}
