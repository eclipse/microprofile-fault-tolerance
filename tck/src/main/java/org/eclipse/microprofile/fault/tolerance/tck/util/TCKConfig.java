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

    private static final TCKConfig INSTANCE = new TCKConfig();

    public static TCKConfig getConfig() {
        return INSTANCE;
    }

    private long baseTimeout;

    private int baseMultiplier;

    private TCKConfig() {
        try {
            final Config config = ConfigProvider.getConfig();
            baseTimeout = config
                .getOptionalValue("org.eclipse.microprofile.fault.tolerance.basetimeout", Long.class)
                .orElse(100L);
            baseMultiplier = config
                .getOptionalValue("org.eclipse.microprofile.fault.tolerance.basemultiplier", Integer.class)
                .orElse(10);
        }
        catch (Exception e) {
            System.out.println("Could not use microprofile config. Falling back to system properties. Problem:" + e.getMessage());
            baseTimeout = Long.valueOf(
                System.getProperty("org.eclipse.microprofile.fault.tolerance.basetimeout", "100"));
            baseMultiplier = Integer.valueOf(
                System.getProperty("org.eclipse.microprofile.fault.tolerance.basemultiplier", "10"));
        }
    }

    /**
     * The base timeout serves as the base multiplier for all timeouts and is expressed in milliseconds.
     * Must not be less than 10ms.
     */
    public long getBaseTimeout() {
        return baseTimeout;
    }

    /**
     * Should be the Timeout default
     *
     * @return
     */
    public String getTimeoutInStr() {
        return String.valueOf(getTimeoutInMillis(1000));
    }

    public String getTimeoutInStr(final int originalInMillis) {
        return String.valueOf(getTimeoutInMillis(originalInMillis));
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
