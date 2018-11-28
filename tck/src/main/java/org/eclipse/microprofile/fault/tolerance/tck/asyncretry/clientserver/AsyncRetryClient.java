package org.eclipse.microprofile.fault.tolerance.tck.asyncretry.clientserver;
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

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.context.RequestScoped;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

/**
 * A client to demonstrate the combination of the @Retry and @Asynchronous annotations.
 *
 * @author <a href="mailto:bbaptista@tomitribe.com">Bruno Baptista</a>
 */
@RequestScoped
public class AsyncRetryClient {

    private int countServiceAInvocations = 0;

    /**
     * Service A will retry a method returning a CompletionStage and configured to always completeExceptionally.
     *
     * @return a {@link CompletionStage}
     * @throws IOException
     */
    @Asynchronous
    @Retry(maxRetries = 2)
    public Future<String> serviceA() {
        countServiceAInvocations++;

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("Simulated error"));
        return future;
    }

    public int getCountServiceAInvocations() {
        return countServiceAInvocations;
    }
}
