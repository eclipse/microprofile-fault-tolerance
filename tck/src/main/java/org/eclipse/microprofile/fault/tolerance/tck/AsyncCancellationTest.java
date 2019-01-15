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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncCancellationClient;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Test that calling {@code cancel()} on a {@code Future} returned from a method annotated with {@code Asynchronous} is handled correctly.
 * <p>
 * According to the documentation of {@code Future}:
 * <ul>
 * <li>If the task has not started running, it should not run</li>
 * <li>If the task has started running and {@code mayInterruptIfRunning} is {@code true}, the thread should be interrupted</li>
 * <li>When {@code cancel()} returns, calling {@code isDone()} should return {@code true}</li>
 * <li>If {@code cancel()} returns {@code true}, calling {@code isCancelled()} should return {@code true}</li>
 * </ul>
 */
public class AsyncCancellationTest extends Arquillian {
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsyncCancellation.jar")
                .addClasses(AsyncCancellationClient.class)
                .addPackage(AsyncBulkheadTask.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftAsyncCancellation.war")
                .addAsLibrary(testJar);
        return war;
    }


    @Inject private AsyncCancellationClient bean;
    
    private static List<AsyncBulkheadTask> tasks = new ArrayList<>();
    
    @AfterMethod
    public void cleanup() {
        for (AsyncBulkheadTask task : tasks) {
            task.complete();
        }
        tasks.clear();
    }
    
    private static AsyncBulkheadTask newTask() {
        AsyncBulkheadTask task = new AsyncBulkheadTask();
        tasks.add(task);
        return task;
    }
    
    @Test
    public void testCancel() throws InterruptedException {
        AsyncBulkheadTask task = newTask();
        
        Future result = bean.serviceAsync(task);
        
        task.assertStarting(result);
        
        result.cancel(true);
        
        task.assertInterrupting();
        
        assertTrue(result.isCancelled(), "Task is not cancelled");
        assertTrue(result.isDone(), "Task is not done");
        Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
        Exceptions.expect(CancellationException.class, () -> result.get());
        
        task.complete();
        
        // Assert result still gives correct values after the task is allowed to complete
        assertTrue(result.isCancelled(), "Task is not cancelled");
        assertTrue(result.isDone(), "Task is not done");
        Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
        Exceptions.expect(CancellationException.class, () -> result.get());
    }
    
    @Test
    public void testCancelWithoutInterrupt() throws InterruptedException {
        AsyncBulkheadTask task = newTask();
        
        Future result = bean.serviceAsync(task);
        
        task.assertStarting(result);
        
        result.cancel(false);
        
        task.assertNotInterrupting();
        
        assertTrue(result.isCancelled(), "Task is not cancelled");
        assertTrue(result.isDone(), "Task is not done");
        Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
        Exceptions.expect(CancellationException.class, () -> result.get());
        
        task.complete();
        
        // Assert result still gives correct values after the task is allowed to complete
        assertTrue(result.isCancelled(), "Task is not cancelled");
        assertTrue(result.isDone(), "Task is not done");
        Exceptions.expect(CancellationException.class, () -> result.get(2, TimeUnit.SECONDS));
        Exceptions.expect(CancellationException.class, () -> result.get());
    }
    
    @Test
    public void testCancelledButRemainsInBulkhead() throws InterruptedException {
        AsyncBulkheadTask task1 = newTask();
        Future result1 = bean.serviceAsyncBulkhead(task1);
        task1.assertStarting();
        
        AsyncBulkheadTask task2 = newTask();
        Future result2 = bean.serviceAsyncBulkhead(task2);
        task2.assertNotStarting();
        
        result1.cancel(false);
        
        // Task 2 does not start because task 1 is still running (it was not interrupted)
        task2.assertNotStarting();
        
        assertTrue(result1.isCancelled(), "Task is not cancelled");
        assertTrue(result1.isDone(), "Task is not done");
        Exceptions.expect(CancellationException.class, () -> result1.get(2, TimeUnit.SECONDS));
        Exceptions.expect(CancellationException.class, () -> result1.get());
    }
    
    @Test
    public void testCancelledWhileQueued() throws InterruptedException {
        AsyncBulkheadTask task1 = newTask();
        Future result1 = bean.serviceAsyncBulkhead(task1);
        task1.assertStarting();
        
        AsyncBulkheadTask task2 = newTask();
        Future result2 = bean.serviceAsyncBulkhead(task2);
        task2.assertNotStarting();
        
        result2.cancel(false);
        task1.complete();
        
        // Task 2 was cancelled while it was in the bulkhead queue, it should not start
        task2.assertNotStarting();
    }
    
    @Test
    public void testCancelledDoesNotRetry() throws InterruptedException {
        AsyncBulkheadTask task = newTask();
        Future result = bean.serviceAsyncRetry(task);
        task.assertStarting();
        
        result.cancel(true);
        
        Thread.sleep(500);
        
        assertEquals(bean.getServiceAsyncRetryAttempts(), 1, "Method should not have been retried - too many retry attempts");
    }
}
