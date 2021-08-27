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
package org.eclipse.microprofile.fault.tolerance.tck.metrics.util;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class MetricComparator {

    // Utility class, no public constructor
    private MetricComparator() {
    }

    /**
     * Check that a nanosecond time is within 20% of an expected time in milliseconds
     * <p>
     * Note that this method applies any timeout scaling configured in TCKConfig, does the millseconds to nanoseconds
     * conversion and creates a {@link Matcher} to do the check.
     * <p>
     * Useful for checking the results from Histograms.
     * 
     * @param originalMillis
     *            the expected time in milliseconds
     * @return a {@link Matcher} which matches against a time in nanoseconds
     */
    public static Matcher<Long> approxMillis(final long originalMillis) {
        long millis = TCKConfig.getConfig().getTimeoutInMillis(originalMillis);
        long nanos = millis * 1_000_000;
        long error = Math.round(nanos * 0.2);
        return Matchers.allOf(greaterThan(nanos - error), lessThan(nanos + error));
    }

    /**
     * Check that a nanosecond time is less than an expected time in milliseconds
     * <p>
     * This method applies any timeout scaling configured in TCKConfig, does the millseconds to nanoseconds conversion
     * and creates a {@link Matcher} to do the check.
     * <p>
     * Useful for checking the results from Histograms.
     * 
     * @param originalMillis
     *            the expected time in milliseconds
     * @return a {@link Matcher} which matches against a time in nanoseconds
     */
    public static Matcher<Long> lessThanMillis(final long originalMillis) {
        long millis = TCKConfig.getConfig().getTimeoutInMillis(originalMillis);
        long nanos = millis * 1_000_000;
        return lessThan(nanos);
    }

}
