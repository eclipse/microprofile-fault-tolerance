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
package org.eclipse.microprofile.fault.tolerance.tck.asynchronous.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AsyncFallbackClient {
    /**
     * Returns a Future which always completes successfully, so should NOT fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    public Future<String> service1() {
        return CompletableFuture.completedFuture("Success");
    }

    /**
     * Always throws an exception directly, so should fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    public Future<String> service2() throws IOException {
        throw new IOException("Simulated error");
    }

    /**
     * Returns a Future which always completes exceptionally, but the return type is Future, so should NOT fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    public Future<String> service3() {
        return CompletableFutureHelper.failedFuture(new IOException("Simulated error"));
    }

    public Future<String> fallback() {
        return completedFuture("Fallback");
    }

    /**
     * Returns a CompletionStage which always completes successfully, so should NOT fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallbackCS")
    public CompletionStage<String> serviceCS1() {
        return CompletableFuture.completedFuture("Success");
    }

    /**
     * Always throws an exception directly, so should fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallbackCS")
    public CompletionStage<String> serviceCS2() throws IOException {
        throw new IOException("Simulated error");
    }

    /**
     * Returns a CompletionStage which always completes exceptionally, so should fallback.
     */
    @Asynchronous
    @Fallback(fallbackMethod = "fallbackCS")
    public CompletionStage<String> serviceCS3() {
        return CompletableFutureHelper.failedFuture(new IOException("Simulated error"));
    }

    public CompletionStage<String> fallbackCS() {
        return completedFuture("Fallback");
    }
}
