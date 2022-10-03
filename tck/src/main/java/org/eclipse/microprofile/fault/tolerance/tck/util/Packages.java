/*
 * Copyright (c) 2018-2022 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.util;

/**
 * Contains constants for utilities packages which need to be included by lots of tests.
 */
public class Packages {

    // Utility class only, no public constructors
    private Packages() {
    }

    /**
     * The {@code org.eclipse.microprofile.fault.tolerance.tck.util} package
     */
    public static final Package UTILS = org.eclipse.microprofile.fault.tolerance.tck.util.Packages.class.getPackage();

    /**
     * The {@code org.eclipse.microprofile.fault.tolerance.tck.metrics.util} package
     */
    public static final Package METRIC_UTILS =
            org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricComparator.class.getPackage();

    private static final Package METRIC_COMMON =
            org.eclipse.microprofile.fault.tolerance.tck.metrics.common.BaseRegistry.class.getPackage();

    private static final Package METRIC_V4 =
            org.eclipse.microprofile.fault.tolerance.tck.metrics.v40.BaseRegistryProducer.class.getPackage();

    private static final Package METRIC_V5 =
            org.eclipse.microprofile.fault.tolerance.tck.metrics.v50.BaseRegistryProducer.class.getPackage();

    /**
     * The support packages for metrics tests. Includes util, common and either v4 or v5 depending on the version of the
     * MP Metrics API which is present
     */
    public static final Package[] METRICS_SUPPORT;

    static {
        METRICS_SUPPORT = new Package[3];
        METRICS_SUPPORT[0] = METRIC_UTILS;
        METRICS_SUPPORT[1] = METRIC_COMMON;
        if (isMetrics50()) {
            METRICS_SUPPORT[2] = METRIC_V5;
        } else {
            METRICS_SUPPORT[2] = METRIC_V4;
        }
    }

    /**
     * Returns whether the MP Metrics 5.0 API is in use
     * <p>
     * The value of this can be forced with the system property {@code ft.tck.use.metrics.5 = true|false}. If that
     * system property is not set, this method looks to see whether the MP Metrics 5.0 API classes are available.
     * 
     * @return
     */
    private static boolean isMetrics50() {
        String property = System.getProperty("ft.tck.use.metrics.5");
        if (property != null) {
            return Boolean.valueOf(property);
        }

        try {
            Class.forName("org.eclipse.microprofile.metrics.annotation.RegistryScope");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
