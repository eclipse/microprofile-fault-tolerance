/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util;

/**
 * Allows tests to get the value of a counter and compare it with a baseline.
 * <p>
 * Most methods on this class will treat a non-existent counter as having a value of zero to allow implementations to
 * lazily create metrics.
 * <p>
 * Most tests should use {@link TelemetryMetricGetter} to create instances of this class.
 */
public class TelemetryCounterMetric {

    private TelemetryMetricID metricId;
    private long baseline;

    public TelemetryCounterMetric(TelemetryMetricID metricId) {
        this.metricId = metricId;
        this.baseline = 0;
    }

    /**
     * Get the counter value, or zero if the metric doesn't exist
     * <p>
     * This method will not create the metric if it does not exist.
     *
     * @param startTime
     *            a start time as a unix epoch, all metrics from before this time will be ignored
     * @param endTime
     *            a end time as a unix epoch, all metrics from after this time will be ignored
     * @return the counter value, or zero if the metric doesn't exist
     */
    public long value() {
        return InMemoryMetricReader.current().readLongData(metricId);
    }

    /**
     * Capture the current counter value for later comparison with {@link #delta()}
     * <p>
     * This method will not create the metric if it does not exist.
     */
    public void baseline() {
        baseline = value();
    }

    /**
     * Return the difference between the current value of the metric and the value when {@link #baseline} was called.
     *
     * @return the difference between the metric value and the baseline
     */
    public long delta() {
        return value() - baseline;
    }

}
