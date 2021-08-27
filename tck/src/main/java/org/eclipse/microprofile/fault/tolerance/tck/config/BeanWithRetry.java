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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.Dependent;

/**
 * @author Antoine Sabot-Durand
 */
@Dependent
@Retry
public class BeanWithRetry {

    private int retry = 0;

    @Retry
    public void triggerException() {
        retry++;
        throw new IllegalStateException("Exception");
    }

    public int getRetry() {
        return retry;
    }
}
