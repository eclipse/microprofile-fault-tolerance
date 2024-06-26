/*
 *******************************************************************************
 * Copyright (c) 2020-2022 Contributors to the Eclipse Foundation
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;

/**
 * Allows tests to get the value of a Histogram, it does not support comparing with a baseline
 * <p>
 * Most tests should use {@link TelemetryMetricGetter} to create instances of this class.
 */
public class TelemetryHistogramMetric {
    private final TelemetryMetricID metricId;

    public TelemetryHistogramMetric(TelemetryMetricID metricId) {
        this.metricId = metricId;
    }

    @SuppressWarnings("unchecked") // Fail early on a misconfigured test
    public Collection<HistogramPointData> getHistogramPoints() {
        InMemoryMetricReader reader = InMemoryMetricReader.current();
        return (List<HistogramPointData>) reader.getPointData(metricId);
    }

    public Optional<Long> getHistogramCount() {
        Collection<HistogramPointData> hpd = getHistogramPoints();

        if (hpd.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(hpd.stream().mapToLong(
                pd -> pd.getCounts().stream().reduce(0L, Long::sum))
                .sum());
    }

    public boolean isPresent() {
        return !getHistogramPoints().isEmpty();
    }
}
