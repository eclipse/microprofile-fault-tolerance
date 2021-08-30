package org.eclipse.microprofile.fault.tolerance.tck.asyncretry.clientserver;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCallerExecutor;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * A client to demonstrate the combination of the @Retry and @Asynchronous annotations.
 *
 * @author <a href="mailto:bbaptista@tomitribe.com">Bruno Baptista</a>
 */
@RequestScoped
public class AsyncRetryClient {

    private int countInvocationsServA = 0;
    private int countInvocationsServBFailException = 0;
    private int countInvocationsServBFailExceptionally = 0;
    private int countInvocationsServC = 0;
    private int countInvocationsServD = 0;
    private int countInvocationsServE = 0;
    private int countInvocationsServF = 0;
    private int countInvocationsServG = 0;
    private int countInvocationsServH = 0;
    private TCKConfig config = TCKConfig.getConfig();

    @Inject
    private AsyncCallerExecutor executor;

    /**
     * Service will retry a method returning a CompletionStage and configured to always completeExceptionally.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceA() {
        countInvocationsServA++;
        // always fail
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("Simulated error"));
        return future;
    }

    /**
     * Service will retry a method returning a CompletionStage and configured to always completeExceptionally.
     *
     * @return a {@link CompletionStage}
     */
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceBFailExceptionally(final CompletionStage future) {
        countInvocationsServBFailExceptionally++;
        // always fail
        future.toCompletableFuture().completeExceptionally(new IOException("Simulated error"));
        return future;
    }

    /**
     * Service will retry a method returning a CompletionStage and configured to always completeExceptionally.
     *
     * @return a {@link CompletionStage}
     */
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceBFailException(final CompletionStage future) {
        countInvocationsServBFailException++;
        // always fail
        throw new RuntimeException("Simulated error");
    }

    /**
     * Service will retry a method returning a CompletionStage and configured to completeExceptionally twice.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 3)
    public CompletionStage<String> serviceC() {
        countInvocationsServC++;

        CompletableFuture<String> future = new CompletableFuture<>();

        if (countInvocationsServC < 3) {
            // fail 2 first invocations
            future.completeExceptionally(new IOException("Simulated error"));
        } else {
            future.complete("Success");
        }
        return future;
    }

    /**
     * Service will retry a method returning a chained, running sequentially, CompletionStage configured to
     * completeExceptionally twice.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 3)
    public CompletionStage<String> serviceD() {
        countInvocationsServD++;

        if (countInvocationsServD < 3) {
            // fail 2 first invocations
            return CompletableFuture.supplyAsync(doTask(null), executor)
                    .thenCompose(s -> CompletableFuture.supplyAsync(doTask("Simulated error"), executor));
        } else {
            return CompletableFuture.supplyAsync(doTask(null), executor)
                    .thenCompose(s -> CompletableFuture.supplyAsync(doTask(null), executor));
        }
    }

    /**
     * Service will retry a method returning a chained, running sequentially, CompletionStage configured to
     * completeExceptionally on all calls.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceE() {
        countInvocationsServE++;

        // always fail
        return CompletableFuture.supplyAsync(doTask(null), executor)
                .thenCompose(s -> CompletableFuture.supplyAsync(doTask("Simulated error"), executor));
    }

    /**
     * Service will retry a method returning a parallel execution of 2 CompletionStages. One of them configured to
     * always fail.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 3)
    public CompletionStage<String> serviceF() {
        countInvocationsServF++;

        if (countInvocationsServF < 3) {
            // fail 2 first invocations
            return CompletableFuture.supplyAsync(doTask(null), executor)
                    .thenCombine(CompletableFuture.supplyAsync(doTask("Simulated error"), executor),
                            (s, s2) -> s + " then " + s2);
        } else {
            return CompletableFuture.supplyAsync(doTask(null), executor)
                    .thenCombine(CompletableFuture.supplyAsync(doTask(null), executor),
                            (s, s2) -> s + " then " + s2);
        }

    }

    /**
     * Service will retry a method returning a parallel execution of 2 CompletionStages. One of them configured to fail
     * twice.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceG() {
        countInvocationsServG++;
        // always fail
        return CompletableFuture.supplyAsync(doTask(null), executor)
                .thenCombine(CompletableFuture.supplyAsync(doTask("Simulated error"), executor),
                        (s, s2) -> s + " then " + s2);

    }

    /**
     * Service will retry a method returning CompletionStages but throwing an exception. fail twice.
     *
     * @return a {@link CompletionStage}
     */
    @Asynchronous
    @Retry(maxRetries = 2)
    public CompletionStage<String> serviceH() {
        countInvocationsServH++;
        // fails twice
        if (countInvocationsServH < 3) {
            throw new RuntimeException("Simulated error");
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("Success");
        return future;
    }

    public int getCountInvocationsServA() {
        return countInvocationsServA;
    }

    public int getCountInvocationsServBFailException() {
        return countInvocationsServBFailException;
    }

    public int getCountInvocationsServBFailExceptionally() {
        return countInvocationsServBFailExceptionally;
    }

    public int getCountInvocationsServC() {
        return countInvocationsServC;
    }

    public int getCountInvocationsServD() {
        return countInvocationsServD;
    }

    public int getCountInvocationsServE() {
        return countInvocationsServE;
    }

    public int getCountInvocationsServF() {
        return countInvocationsServF;
    }

    public int getCountInvocationsServG() {
        return countInvocationsServG;
    }

    public int getCountInvocationsServH() {
        return countInvocationsServH;
    }

    private Supplier<String> doTask(final String errorMessage) {
        return () -> {
            try {
                // simulate some processing.
                TimeUnit.MILLISECONDS.sleep(config.getTimeoutInMillis(50));
            } catch (InterruptedException e) {
                throw new RuntimeException("Unplanned error: " + e);
            }
            if (nonNull(errorMessage)) {
                throw new RuntimeException(errorMessage);
            } else {
                return "Success";
            }
        };
    }
}
