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
package org.eclipse.microprofile.fault.tolerance.tck.interceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.interceptor.EarlyFtInterceptor.InterceptEarly;
import org.eclipse.microprofile.fault.tolerance.tck.interceptor.LateFtInterceptor.InterceptLate;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.testng.TestException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Component to show the CDI interceptor ordering with FT annotations
 *
 * @author carlosdlr
 */
@ApplicationScoped
public class InterceptorComponent {

    @Inject
    private OrderQueueProducer orderFactory;

    @InterceptEarly
    @InterceptLate
    @Retry(maxRetries = 1)
    public String serviceRetryA() {
        orderFactory.getOrderQueue().add("serviceRetryA");
        throw new TestException("retryGetString failed");
    }

    @InterceptEarly
    @InterceptLate
    @Asynchronous
    public Future<String> asyncGetString() {
        orderFactory.getOrderQueue().add("asyncGetString");
        return CompletableFuture.completedFuture("OK");
    }
}
