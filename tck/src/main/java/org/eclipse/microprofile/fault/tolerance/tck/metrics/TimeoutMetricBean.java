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

import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class TimeoutMetricBean {

    @Timeout(value = 500)
    public void counterTestWorkForMillis(long millis) {
        doWork(millis);
    }

    @Timeout(value = 2000)
    public void histogramTestWorkForMillis(long millis) {
        doWork(millis);
    }

    private void doWork(long millis) {
        try {
            Thread.sleep(millis);// timeout config must be done in the caller.
        } catch (InterruptedException ex) {
            throw new RuntimeException("Test was interrupted", ex);
        }
    }

}
