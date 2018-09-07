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
package org.eclipse.microprofile.fault.tolerance.tck.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *
 * @author Ondrej Mihalyi
 */
public class CompletableFutureHelper {

    private CompletableFutureHelper() { // this is a util class only for static methods
    }

    /**
     * Creates a future completed with a supplied exception.
     * Equivalent to {@link CompletableFuture}{@code .failedFuture} available since Java 9 but not in Java 8.
     * 
     * @param <U> The type of the future result
     * @param ex The exception to finish the result with
     * @return A future completed with the a supplied exception {@code ex}
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * Returns a future that is completed when the stage is completed and has the same value or exception
     * as the completed stage. It's supposed to be equivalent to calling 
     * {@link CompletionStage#toCompletableFuture()} but works with any CompletionStage
     * and doesn't throw {@link java.lang.UnsupportedOperationException}.
     * 
     * @param <U> The type of the future result
     * @param stage Stage to convert to a future
     * @return Future converted from stage
     */
    public static <U> CompletableFuture<U> toCompletableFuture(CompletionStage<U> stage) {
        CompletableFuture<U> future = new CompletableFuture<>();
        stage.whenComplete((v, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            }
            else {
                future.complete(v);
            }
        });
        return future;
    }
}