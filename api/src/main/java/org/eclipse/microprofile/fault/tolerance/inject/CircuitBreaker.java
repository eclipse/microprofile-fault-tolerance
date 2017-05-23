/*
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
 */
package org.eclipse.microprofile.fault.tolerance.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Define the Circuit Breaker policy
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD })
public @interface CircuitBreaker {

    /**
     * Define the failure criteria
     * @return the failure exception
     */
    Class<? extends Throwable>[] failOn() default Throwable.class;

    /**
     *
     * @return The delay time after the circuit is open
     */
    long delay() default 5000;

    /**
     *
     * @return The delay unit after the circuit is open
     */

    ChronoUnit delayUnit() default ChronoUnit.MILLIS;
    

    /**
     * The number of consecutive requests in a rolling window 
     * that will trip the circuit.
     * @return the number of the consecutive requests in a rolling window
     *
     */
    long requestVolumeThreshold() default 20;
    /**
     * The failure threshold to trigger the circuit to open.
     * e.g. if the requestVolumeThreshold is 20 and failureRation is .50, 
     * more than 10 failures in 20 consecutive requests will trigger
     * the circuit to open.
     * @return The failure threshold to open the circuit
     */
    double failureRatio() default .50;

    /**
     * For an open circuit, after the delay period is reached, once the successThreshold
     * is reached, the circuit is back to close again.
     * @return The success threshold to fully close the circuit
     */
    long successThreshold() default 1;

}
