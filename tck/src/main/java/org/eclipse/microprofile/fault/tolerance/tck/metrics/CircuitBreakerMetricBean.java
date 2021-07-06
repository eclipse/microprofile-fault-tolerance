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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CircuitBreakerMetricBean {

    public enum Result {
        PASS, PASS_EXCEPTION, SKIPPED_EXCEPTION, FAIL
    }

    @SuppressWarnings("serial")
    public static class SkippedException extends TestException {
        public SkippedException() {
            super("skipped");
        }
    }

    @CircuitBreaker(requestVolumeThreshold = 2, failureRatio = 1.0D, delay = 1000, successThreshold = 2, failOn = {
            TestException.class}, skipOn = SkippedException.class)
    public void doWork(Result result) {
        switch (result) {
            case PASS :
                return;
            case PASS_EXCEPTION :
                throw new RuntimeException();
            case FAIL :
                throw new TestException();
            case SKIPPED_EXCEPTION :
                throw new SkippedException();
            default :
                throw new IllegalArgumentException("Unknown result requested");
        }
    }
}
