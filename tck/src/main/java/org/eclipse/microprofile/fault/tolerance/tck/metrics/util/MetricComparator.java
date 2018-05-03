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

import static org.hamcrest.Matchers.closeTo;

import org.hamcrest.Matcher;

public class MetricComparator {

    // Utility class, no public constructor
    private MetricComparator() {}
    
    /**
     * Check that a nanosecond time is with 100ms of an expected time in milliseconds
     * <p>
     * Note that this method does both the millseconds to nanoseconds conversion and creates a {@link Matcher} to do the check.
     * <p>
     * Useful for checking the results from Histograms.
     * 
     * @param millis the expected time in milliseconds
     * @return a {@link Matcher} which matches against a time in milliseconds
     */
    public static Matcher<Double> approxMillis(long millis) {
        long nanos = millis * 1_000_000;
        return closeTo(nanos, 1_000_000 * 100);
    }
    
}
