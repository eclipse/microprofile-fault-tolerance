/*
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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TCKConfig {

    private static final TCKConfig INSTANCE = new TCKConfig();
    public static final String RESOURCE_NAME = "timeout-multiplier";

    public static TCKConfig getConfig() {
        return INSTANCE;
    }

    private double baseMultiplier;

    private TCKConfig() {

        try (InputStream s = TCKConfig.class.getResourceAsStream("/" + RESOURCE_NAME)) {
            if (s != null) {
                // Expected path when in deployed application
                BufferedReader reader = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8));
                String multiplier = reader.readLine();
                baseMultiplier = Double.valueOf(multiplier);
                System.out.println("Loaded timeout-multiplier from resource: " + baseMultiplier);
            } else {
                // Expected path when running in client
                baseMultiplier = Double.valueOf(
                        System.getProperty("org.eclipse.microprofile.fault.tolerance.tck.timeout.multiplier", "1.0"));
                System.out.println("Loaded timeout-multiplier from system property: " + baseMultiplier);
            }
        } catch (IOException e) {
            fail("Resource " + RESOURCE_NAME + " is present but could not be read", e);
        }
    }

    public String getTimeoutInStr(final long originalInMillis) {
        return String.valueOf(getTimeoutInMillis(originalInMillis));
    }

    public long getTimeoutInMillis(final long originalInMillis) {
        return Math.round(originalInMillis * baseMultiplier);
    }

    public Duration getTimeoutInDuration(final int originalInMillis) {
        return Duration.ofMillis(getTimeoutInMillis(originalInMillis));
    }

    public double getBaseMultiplier() {
        return baseMultiplier;
    }
}
