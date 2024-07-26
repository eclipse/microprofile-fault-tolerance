/*
 *******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;

@ApplicationScoped
public class InMemoryMetricReader implements MetricReader {

    private CollectionRegistration collectionRegistration;
    private boolean isShutdown = false;

    public static InMemoryMetricReader current() {
        return CDI.current().select(InMemoryMetricReader.class).get();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public void register(CollectionRegistration registration) {
        if (isShutdown) {
            throw new IllegalStateException("InMemoryMetricReader has been shutdown");
        }

        collectionRegistration = registration;
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        collectionRegistration = null;
        isShutdown = true;
        return CompletableResultCode.ofSuccess();
    }

    /**
     * Get the metric for the given {@code id}.
     * <p>
     * If the metric exists but is the wrong type, an assertionError is thrown.
     *
     * @param id
     *            the metric ID
     * @return the metric data, or an empty {@code Optional}
     * @throws AssertionError
     *             if the metric exists but has the wrong type
     */
    public Optional<MetricData> getMetric(TelemetryMetricID id) {
        Optional<MetricData> result = getMetric(id.name);
        result.ifPresent(md -> validateMetricType(md, id));
        return result;
    }

    private Optional<MetricData> getMetric(String name) {
        Collection<MetricData> allMetrics = collectionRegistration.collectAllMetrics();
        List<MetricData> matchingMetrics = allMetrics.stream()
                .filter(md -> md.getName().equals(name))
                .collect(Collectors.toList());

        assertThat("More than one MetricData found for name: " + name,
                matchingMetrics, hasSize(lessThan(2)));

        return matchingMetrics.isEmpty() ? Optional.empty() : Optional.of(matchingMetrics.get(0));
    }

    public long readLongData(TelemetryMetricID id) {
        return getMetric(id)
                .flatMap(md -> getLongPointData(md, id))
                .map(LongPointData::getValue)
                .orElse(0L);
    }

    public String getUnit(String metricName) {
        return getMetric(metricName)
                .orElseThrow(() -> new IllegalStateException("No metric found for name: " + metricName))
                .getUnit();
    }

    public static Optional<LongPointData> getLongPointData(MetricData md, TelemetryMetricID id) {
        switch (md.getType()) {
            case LONG_GAUGE :
                GaugeData<LongPointData> gaugeData = md.getLongGaugeData();
                return getGaugePointData(gaugeData, id);
            case LONG_SUM :
                SumData<LongPointData> sumData = md.getLongSumData();
                return getSumPointData(sumData, id);
            default :
                throw new IllegalStateException("Metric " + id.name + " does not have long type data");
        }
    }

    public static Optional<HistogramPointData> getHistogramPointData(MetricData md, TelemetryMetricID id) {
        assertEquals(md.getType(), MetricDataType.HISTOGRAM, "Metric " + id.name + " is not a histogram");
        assertEquals(md.getHistogramData().getAggregationTemporality(), AggregationTemporality.CUMULATIVE,
                "Metric " + id.name + " has wrong temporality");

        List<HistogramPointData> data = md.getHistogramData().getPoints().stream()
                .filter(hasAllAttributes(id.attributes))
                .collect(Collectors.toList());

        assertThat("Found more than one data point for metric: " + id, data, hasSize(lessThan(2)));

        return data.isEmpty() ? Optional.empty() : Optional.of(data.get(0));
    }

    private static <T extends PointData> Optional<T> getGaugePointData(GaugeData<T> gaugeData, TelemetryMetricID id) {
        List<T> data = gaugeData.getPoints().stream()
                .filter(hasAllAttributes(id.attributes))
                .collect(Collectors.toList());
        assertThat("Found more than one data point for metric: " + id, data, hasSize(lessThan(2)));

        return data.isEmpty() ? Optional.empty() : Optional.of(data.get(0));
    }

    private static <T extends PointData> Optional<T> getSumPointData(SumData<T> sumData, TelemetryMetricID id) {
        assertEquals(sumData.getAggregationTemporality(), CUMULATIVE, "Wrong temporality type for metric " + id.name);

        List<T> data = sumData.getPoints().stream()
                .filter(hasAllAttributes(id.attributes))
                .collect(Collectors.toList());
        assertThat("Found more than one data point for metric: " + id, data, hasSize(lessThan(2)));

        return data.isEmpty() ? Optional.empty() : Optional.of(data.get(0));
    }

    /**
     * Returns a predicate which checks whether a {@code PointData} contains all of the given {@code Attributes}.
     * <p>
     * Permits package access for testing
     *
     * @param attributes
     *            the attributes to check for
     * @return a predicate which returns {@code true} if {@link PointData#getAttributes()} returns a superset of
     *         {@code attributes}, otherwise {@code false}
     */
    static Predicate<PointData> hasAllAttributes(Attributes attributes) {
        return pointData -> {
            for (Entry<AttributeKey<?>, Object> e : attributes.asMap().entrySet()) {
                if (!pointData.getAttributes().asMap().containsKey(e.getKey())) {
                    return false;
                }
                if (!Objects.equals(pointData.getAttributes().get(e.getKey()), e.getValue())) {
                    return false;
                }
            }
            return true;
        };
    }

    private static void validateMetricType(MetricData md, TelemetryMetricID id) {
        switch (id.type) {
            case COUNTER :
                assertEquals(md.getType(), MetricDataType.LONG_SUM,
                        "Wrong type for metric " + id.name);
                assertTrue(md.getLongSumData().isMonotonic(),
                        "Metric is not monotonic: " + id.name);
                break;
            case UPDOWNCOUNTER :
                assertEquals(md.getType(), MetricDataType.LONG_SUM,
                        "Wrong type for metric " + id.name);
                assertFalse(md.getLongSumData().isMonotonic(),
                        "Metric should not be monotonic: " + id.name);
                break;
            case GAUGE :
                assertEquals(md.getType(), MetricDataType.LONG_GAUGE,
                        "Wrong type for metric " + id.name);
                break;
            case HISTOGRAM :
                assertEquals(md.getType(), MetricDataType.HISTOGRAM,
                        "Wrong type for metric " + id.name);
                break;
            default :
                // Shouldn't happen because we validate this in the constructor
                throw new IllegalStateException("Invalid metric type: " + id.type);
        }
    }
}
