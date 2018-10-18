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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

@ApplicationScoped
@Asynchronous
public class AsyncCaller {

    /**
     * Run a runnable asynchronously
     *
     * @param runnable task to execute
     * @return a completed future set to null
     */
    public Future<Void> run(Runnable runnable) {
        runnable.run();
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Run a callable asynchronously
     * 
     * @param callable the callable to run
     * @param <T> the type returned by {@code callable}
     * @return a future which can be used to get the result of running {@code callable}
     */
    public <T> Future<T> submit(Callable<T> callable) {
        try {
            return CompletableFuture.completedFuture(callable.call());
        }
        catch (Exception e) {
            CompletableFuture<T> result = new CompletableFuture<T>();
            result.completeExceptionally(e);
            return result;
        }
    }

}
