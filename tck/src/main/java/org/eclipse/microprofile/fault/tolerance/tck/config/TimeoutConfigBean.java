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

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Suite of methods for testing the various parameters of Timeout
 */
@ApplicationScoped
public class TimeoutConfigBean {
    @Timeout(value = 1, unit = ChronoUnit.MILLIS)
    public void serviceValue() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }

    @Timeout(value = 1000, unit = ChronoUnit.MICROS)
    public void serviceUnit() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }

    @Timeout(value = 10, unit = ChronoUnit.MICROS)
    @Asynchronous
    public CompletionStage<Void> serviceBoth() throws InterruptedException {
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        return CompletableFuture.completedFuture(null);
    }
}
