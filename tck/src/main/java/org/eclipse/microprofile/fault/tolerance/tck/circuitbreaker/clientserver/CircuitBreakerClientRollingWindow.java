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
package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver;

import java.io.Serializable;
import java.sql.Connection;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
/**
 * A client to exercise Circuit Breaker thresholds, with a SuccessThreshold of 2,
 * a requestVolumeThreshold of 4, failureRatio of 0.5 and a 1 millisecond delay
 *
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@RequestScoped
public class CircuitBreakerClientRollingWindow implements Serializable {
        
    private static final long serialVersionUID = 1L;
    private int counterForInvokingServiceA = 0;
        
    public int getCounterForInvokingServiceA() {
                return counterForInvokingServiceA;
        }

        public void setCounterForInvokingServiceA(int count) {
                this.counterForInvokingServiceA = count;
        }

    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio=0.5, delay = 1)
    public Connection serviceARollingWindowOpenAfter4() {
        Connection conn = null;
        counterForInvokingServiceA++;
        conn = connectionService1();
        
        return conn;
    }

    //simulate a backend service
    //Throw exception for the 2nd and 3rd request.
    private Connection connectionService1() {
        if((counterForInvokingServiceA ==2 ) || (counterForInvokingServiceA ==3)) {
                throw new RuntimeException("Connection failed");
        }
        return null;
    
    
    }
    //simulate a backend service
    //Throw exception for the 2nd and 5th request.
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio=0.5, delay = 1)
    public Connection serviceARollingWindowOpenAfter5() {
        Connection conn = null;
        counterForInvokingServiceA++;
        conn = connectionService2();
        
        return conn;
    }
    
    //simulate a backend service
    //Throw exception for the 2nd and 5th request.
    private Connection connectionService2() {
        if((counterForInvokingServiceA ==2 ) || (counterForInvokingServiceA ==5)) {
                throw new RuntimeException("Connection failed");
        }
        return null;
    
    
    }
    
}