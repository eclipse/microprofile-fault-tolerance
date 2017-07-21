/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver;

import java.io.Serializable;
import java.sql.Connection;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
/**
 * A client to exercise Circuit Breaker thresholds using Retries. Annotations are
 * specified at both the Class and Method level.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
@CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
@Retry(retryOn = {RuntimeException.class}, maxRetries = 7)
public class CircuitBreakerClassLevelClientWithRetry implements Serializable {
    private int counterForInvokingServiceA = 0;
    private int counterForInvokingServiceB = 0;

    public int getCounterForInvokingServiceA() {
        return counterForInvokingServiceA;
    }
    
    public int getCounterForInvokingServiceB() {
        return counterForInvokingServiceB;
    }
    
    public Connection serviceA() {
        Connection conn = null;
        counterForInvokingServiceA++;
        conn = connectionService();
        return conn;
    }

    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
    @Retry(retryOn = {RuntimeException.class}, maxRetries = 2)
    public Connection serviceB() {
        Connection conn = null;
        counterForInvokingServiceB++;
        conn = connectionService();
        return conn;
    }
    
    //simulate a backend service
    private Connection connectionService() {
        throw new RuntimeException("Connection failed");
    }
}