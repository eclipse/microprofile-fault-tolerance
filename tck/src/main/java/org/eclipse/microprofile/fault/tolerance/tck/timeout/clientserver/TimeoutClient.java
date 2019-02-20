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
package org.eclipse.microprofile.fault.tolerance.tck.timeout.clientserver;

import java.sql.Connection;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;
/**
 * A client to test Timeouts
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
public class TimeoutClient {

    /**
     * serviceA uses the default Fault Tolerance timeout of 1 second.
     * @param timeToSleep How long should the execution take in millis
     * @return null or exception is raised
     */    
    @Timeout
    public Connection serviceA(long timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }

    /**
     * serviceB specifies a Timeout longer than the default, at 2 seconds
     * @param timeToSleep How long should the execution take in millis
     * @return null or exception is raised
     */
    @Timeout(2000)
    public Connection serviceB(long timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }

    /**
     * serviceC specifies a Timeout shorter than the default, at .5 seconds
     * @param timeToSleep How long should the execution take in millis
     * @return null or exception is raised
     */
    @Timeout(500)
    public Connection serviceC(long timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }
    
    /**
     * serviceD specifies a Timeout longer than the default, at 2 
     * seconds.<br>
     * Tests using this method will not have the timeout dynamically configured.
     * @param timeToSleepInMillis  How long should the execution take in millis
     * @return null or exception is raised
     */
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    public Connection serviceD(long timeToSleepInMillis) {
        try {
            Thread.sleep(timeToSleepInMillis);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }
}
