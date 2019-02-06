package org.eclipse.microprofile.fault.tolerance.tck.util;/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class TCKConfig {

    /**
     * The base timeout serves as the base multiplier for all timeouts and is expressed in milliseconds.
     * Must not be less than 10ms.
     */
    @Inject
    @ConfigProperty(name = "org.eclipse.microprofile.fault.tolerance.basetimeout", defaultValue = "100")
    private long baseTimeout;

    @Inject
    @ConfigProperty(name = "org.eclipse.microprofile.fault.tolerance.basemultiplier", defaultValue = "10")
    private int baseMultiplier;

    public long getBaseTimeout() {
        return baseTimeout;
    }

    public long getTimeoutInMillis(final int originalInMillis) {
        if (originalInMillis < baseTimeout) {
            throw new IllegalArgumentException("Timeout must be bigger than " + baseTimeout + "ms, the baseTimeout.");
        }
        final float offset = Float.valueOf(originalInMillis) / 1000;
        return Math.round(getTimeoutInMillis() * offset);
    }

    public Duration getTimeoutInDuration(final int originalInMillis) {
        return Duration.ofMillis(getTimeoutInMillis(originalInMillis));
    }

    /**
     * Reference value is 1000ms
     *
     * @return
     */
    public long getTimeoutInMillis() {
        return baseTimeout * baseMultiplier;
    }


}
