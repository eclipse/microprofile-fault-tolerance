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

/**
 * A client to exercise Circuit Breaker thresholds, with a SuccessThreshold of 2,
 * a requestVolumeThreshold of 4, failureRatio of 0.75 and a 50 second delay, so
 * that, once opened, the Circuit Breaker remains open for the duration of the test.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
@CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
public class CircuitBreakerClassLevelClientWithDelay implements Serializable {
    private int counterForInvokingService = 0;

    public int getCounterForInvokingService() {
        return counterForInvokingService;
    }

    public void setCounterForInvokingServiceA(int count) {
        this.counterForInvokingService = count;
    }

    public Connection serviceA() {
        Connection conn = null;

        counterForInvokingService++;
        conn = connectionService();

        return conn;
    }

    public Connection serviceB() {
        Connection conn = null;

        counterForInvokingService++;
        conn = connectionService();

        return conn;
    }

    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 2, failureRatio = 1, delay = 50000)
    public Connection serviceC() {
        Connection conn = null;

        counterForInvokingService++;
        conn = connectionService();

        return conn;
    }

    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 1)
    public Connection serviceD() {
        Connection conn = null;

        counterForInvokingService++;
        conn = connectionService();

        return conn;
    }

    //simulate a backend service
    private Connection connectionService() {
        if (counterForInvokingService < 5) {
            throw new RuntimeException("Connection failed");
        }
        return null;
    }
}