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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
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

    public long readLongData(TelemetryMetricID id) {
        @SuppressWarnings("unchecked")
        List<LongPointData> longData = (List<LongPointData>) getPointData(id);

        return longData.stream()
                .mapToLong(LongPointData::getValue)
                .sum();
    }

    protected List<?> getPointData(TelemetryMetricID id) {
        Collection<MetricData> allMetrics = collectionRegistration.collectAllMetrics();

        return allMetrics.stream()
                .filter(
                        md -> md.getName().equals(id.name))
                .flatMap(
                        md -> md.getData().getPoints().stream())
                .filter(
                        point -> id.attributes.asMap().keySet().stream()
                                .allMatch(key -> point.getAttributes().asMap().containsKey(key)
                                        && id.attributes.asMap().get(key)
                                                .equals(point.getAttributes().asMap().get(key))))
                .collect(Collectors.toList());
    }

    public Optional<LongPointData> getGaugueMetricLatestValue(TelemetryMetricID id) {
        Collection<MetricData> allMetrics = collectionRegistration.collectAllMetrics();

        Optional<LongPointData> gague = allMetrics.stream()
                .filter(
                        md -> md.getName().equals(id.name))
                .flatMap(md -> md.getLongGaugeData().getPoints().stream())
                .filter(point -> id.attributes.asMap().keySet().stream()
                        .allMatch(key -> point.getAttributes().asMap().containsKey(key)
                                && id.attributes.asMap().get(key)
                                        .equals(point.getAttributes().asMap().get(key))))
                // feeding the points into Long.compare in reverse order will return the largest first.
                .sorted((pointOne, pointTwo) -> Long.compare(pointTwo.getEpochNanos(), pointOne.getEpochNanos()))
                .findFirst();

        return gague;
    }

    public String getUnit(String metricName) {
        try {
            Collection<MetricData> allMetrics = collectionRegistration.collectAllMetrics();
            Optional<MetricData> mathcingData = allMetrics.stream()
                    .filter(
                            md -> md.getName().equals(metricName))
                    .findAny();

            return mathcingData.get().getUnit();
        } catch (NoSuchElementException e) {
            // If we didn't find anything throwing an exception to fail the test is reasonable
            throw new RuntimeException("Found no results for " + metricName);
        }
    }
}
