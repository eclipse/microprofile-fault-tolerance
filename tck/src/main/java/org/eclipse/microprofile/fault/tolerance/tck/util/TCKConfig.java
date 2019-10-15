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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;


public class TCKConfig {

    private static final int DEFAULT_TIMEOUT = 1000;
    private static final TCKConfig INSTANCE = new TCKConfig();

    public static TCKConfig getConfig() {
        return INSTANCE;
    }

    private double baseMultiplier;

    private TCKConfig() {
        try {
            final Config config = ConfigProvider.getConfig();
            baseMultiplier = config
                .getOptionalValue("org.eclipse.microprofile.fault.tolerance.tck.timeout.multiplier", Double.class)
                .orElse(1.0D);
        }
        catch (Exception e) {
            System.out.println("Could not use microprofile config. Falling back to system properties. Problem:" + e.getMessage());
            baseMultiplier = Double.valueOf(
                System.getProperty("org.eclipse.microprofile.fault.tolerance.tck.timeout.multiplier", "1.0"));
        }
        if (baseMultiplier <= 0) {
            throw new IllegalArgumentException("baseMultiplier must be a positive number. Was set to: " + baseMultiplier);
        }
    }

    /**
     * Should be the Timeout default
     *
     * @return
     */
    public String getTimeoutInStr() {
        return String.valueOf(getTimeoutInMillis(DEFAULT_TIMEOUT));
    }

    public String getTimeoutInStr(final long originalInMillis) {
        return String.valueOf(getTimeoutInMillis(originalInMillis));
    }

    public long getTimeoutInMillis(final long originalInMillis) {
        final double offset = Double.valueOf(originalInMillis) / DEFAULT_TIMEOUT;
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
        return Math.round(DEFAULT_TIMEOUT * baseMultiplier);
    }

}
