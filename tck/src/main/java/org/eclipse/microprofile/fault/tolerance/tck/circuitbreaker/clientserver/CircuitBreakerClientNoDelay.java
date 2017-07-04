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
 * a requestVolumeThreshold of 4, failureRatio of 0.75 and a 1 millisecond delay
 *
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 */
@RequestScoped
public class CircuitBreakerClientNoDelay implements Serializable {
        private int counterForInvokingServiceA = 0;
        
    public int getCounterForInvokingServiceA() {
                return counterForInvokingServiceA;
        }

        public void setCounterForInvokingServiceA(int count) {
                this.counterForInvokingServiceA = count;
        }

        @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio=0.75, delay = 1)
    public Connection serviceA() {
        Connection conn = null;
        counterForInvokingServiceA++;
        conn = connectionService();
        
        return conn;
    }

    //simulate a backend service
    private Connection connectionService() {
        if(counterForInvokingServiceA < 5) {
                throw new RuntimeException("Connection failed");
        }
        return null;
    }
}