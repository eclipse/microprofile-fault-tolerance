/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Bridge class that creates a thread and execute the service task operation in an asynchronous way
 * @author <a href="mailto:kusanagi12002@gmail.com">Carlos Andres De La Rosa</a>
 */
public class AsyncBridge {

    private final Task task;

    /**
     * Constructor that receives the task that will be executed
     * @param task a class that implements the interface task.
     */
    public AsyncBridge(Task task) {
        this.task = task;
    }

    /**
     * method that performs or executes the task
     * @param taskTime time that will take the task to be executed
     * @param expectedTaskResult the expected task result string after the process
     */
    public void perform(long taskTime, String expectedTaskResult) {
        CompletableFuture<Task> waitingFuture = new CompletableFuture<>();
        try {
            waitingFuture.get(taskTime, TimeUnit.MILLISECONDS);
            task.doTask(expectedTaskResult);
            waitingFuture.completedFuture(task);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            waitingFuture.completeExceptionally(e);
        }
    }
}
