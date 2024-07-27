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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

/**
 * Enum containing a definition for each metric defined in the spec.
 * <p>
 * All tests should not use this enum directly, but should use {@link TelemetryMetricGetter} to access metric values.
 * However, if we add new metrics to the spec, their definitions should be added here. Having a defined list of all
 * metrics allows us to easily iterate through them for stuff like {@link TelemetryMetricGetter#baselineMetrics()}.
 * <p>
 * Each metric definition has a name, a type and a set of tags.
 * <p>
 * Each fault tolerance metric is assumed to have a {@code method} tag, so the set of tags does not include a method
 * tag.
 * <p>
 * Each tag in the set is represented by an enum which implements {@link AttributeValue}. Each enum has one entry for
 * each valid value for that tag.
 * <p>
 * For example, the metric {@code ft.bulkhead.calls.total} has one tag, {@code bulkheadResult} which can have the value
 * {@code accepted} or {@code rejected}. The value for this is {@link #BULKHEAD_CALLS} and calling
 * {@code BULKHEAD_CALLS.getArgumentClasses()} returns {@link BulkheadResult}, which is an enum with two entries,
 * {@link BulkheadResult#ACCEPTED} and {@link BulkheadResult#REJECTED}.
 */
public enum TelemetryMetricDefinition {
    INVOCATIONS("ft.invocations.total", MetricType.COUNTER, InvocationResult.class, InvocationFallback.class),
    RETRY_CALLS("ft.retry.calls.total", MetricType.COUNTER, RetryRetried.class, RetryResult.class),
    RETRY_RETRIES("ft.retry.retries.total", MetricType.COUNTER),
    TIMEOUT_CALLS("ft.timeout.calls.total", MetricType.COUNTER, TimeoutTimedOut.class),
    TIMEOUT_EXECUTION_DURATION("ft.timeout.executionDuration", MetricType.HISTOGRAM, "nanoseconds"),
    CIRCUITBREAKER_CALLS("ft.circuitbreaker.calls.total", MetricType.COUNTER, CircuitBreakerResult.class),
    CIRCUITBREAKER_STATE("ft.circuitbreaker.state.total", MetricType.COUNTER, "nanoseconds",
            CircuitBreakerState.class),
    CIRCUITBREAKER_OPENED("ft.circuitbreaker.opened.total", MetricType.COUNTER),
    BULKHEAD_CALLS("ft.bulkhead.calls.total", MetricType.COUNTER, BulkheadResult.class),
    BULKHEAD_EXECUTIONS_RUNNING("ft.bulkhead.executionsRunning", MetricType.UPDOWNCOUNTER),
    BULKHEAD_EXECUTIONS_WAITING("ft.bulkhead.executionsWaiting", MetricType.UPDOWNCOUNTER),
    BULKHEAD_RUNNING_DURATION("ft.bulkhead.runningDuration", MetricType.HISTOGRAM, "nanoseconds"),
    BULKHEAD_WAITING_DURATION("ft.bulkhead.waitingDuration", MetricType.HISTOGRAM, "nanoseconds");

    public enum MetricType {
        COUNTER,
        UPDOWNCOUNTER,
        GAUGE,
        HISTOGRAM
    }

    private String name;
    private String unit;
    private MetricType metricType;
    private Class<? extends AttributeValue>[] getAttributeClasses;

    @SafeVarargs
    private TelemetryMetricDefinition(String name, MetricType metricType, String unit,
            Class<? extends AttributeValue>... tagClasses) {
        this.name = name;
        this.unit = unit;
        this.metricType = metricType;
        this.getAttributeClasses = tagClasses;
    }

    @SafeVarargs
    private TelemetryMetricDefinition(String name, MetricType metricType,
            Class<? extends AttributeValue>... tagClasses) {
        this(name, metricType, null, tagClasses);
    }

    /**
     * The metric name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The metric unit
     *
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * The type of this metric
     *
     * @return the metric type
     */
    public MetricType getMetricType() {
        return metricType;
    }

    /**
     * The tags which are applied to this metric
     * <p>
     * The classes returned from this method will be enums which implement {@link AttributeValue}
     *
     * @return the tags which are applied to this metric
     */
    public Class<? extends AttributeValue>[] getAttributeClasses() {
        return getAttributeClasses;
    }

    public interface AttributeValue {
        public Attributes getAttribute();
    }

    public enum BulkheadResult implements AttributeValue {
        ACCEPTED("accepted"), REJECTED("rejected");

        private Attributes attributes;

        private BulkheadResult(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("bulkheadResult");
            attributes = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attributes;
        }
    }

    public enum CircuitBreakerResult implements AttributeValue {
        SUCCESS("success"), FAILURE("failure"), CIRCUIT_BREAKER_OPEN("circuitBreakerOpen");

        private Attributes attribute;

        private CircuitBreakerResult(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("circuitBreakerResult");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum CircuitBreakerState implements AttributeValue {
        OPEN("open"), CLOSED("closed"), HALF_OPEN("halfOpen");

        private Attributes attribute;

        private CircuitBreakerState(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("state");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum InvocationFallback implements AttributeValue {
        APPLIED("applied"), NOT_APPLIED("notApplied"), NOT_DEFINED("notDefined");

        private Attributes attribute;

        private InvocationFallback(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("fallback");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum InvocationResult implements AttributeValue {
        VALUE_RETURNED("valueReturned"), EXCEPTION_THROWN("exceptionThrown");

        private Attributes attribute;

        private InvocationResult(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("result");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum RetryResult implements AttributeValue {
        VALUE_RETURNED("valueReturned"), EXCEPTION_NOT_RETRYABLE("exceptionNotRetryable"), MAX_RETRIES_REACHED(
                "maxRetriesReached"),
        MAX_DURATION_REACHED("maxDurationReached");

        private Attributes attribute;

        private RetryResult(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("retryResult");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum RetryRetried implements AttributeValue {
        TRUE("true"), FALSE("false");

        private Attributes attribute;

        private RetryRetried(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("retried");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

    public enum TimeoutTimedOut implements AttributeValue {
        TRUE("true"), FALSE("false");

        private Attributes attribute;

        private TimeoutTimedOut(String attributeValue) {
            AttributeKey<String> key = AttributeKey.stringKey("timedOut");
            attribute = Attributes.builder().put(key, attributeValue).build();
        }

        public Attributes getAttribute() {
            return attribute;
        }
    }

}
