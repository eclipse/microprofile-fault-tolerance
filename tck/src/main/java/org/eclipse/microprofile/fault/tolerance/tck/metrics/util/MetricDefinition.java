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

package org.eclipse.microprofile.fault.tolerance.tck.metrics.util;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

/**
 * Enum containing a definition for each metric defined in the spec.
 * <p>
 * All tests should not use this enum directly, but should use {@link MetricGetter} to access metric values. However, if
 * we add new metrics to the spec, their definitions should be added here. Having a defined list of all metrics allows
 * us to easily iterate through them for stuff like {@link MetricGetter#baselineMetrics()}.
 * <p>
 * Each metric definition has a name, a type and a set of tags.
 * <p>
 * Each fault tolerance metric is assumed to have a {@code method} tag, so the set of tags does not include a method
 * tag.
 * <p>
 * Each tag in the set is represented by an enum which implements {@link TagValue}. Each enum has one entry for each
 * valid value for that tag.
 * <p>
 * For example, the metric {@code ft.bulkhead.calls.total} has one tag, {@code bulkheadResult} which can have the value
 * {@code accepted} or {@code rejected}. The value for this is {@link #BULKHEAD_CALLS} and calling
 * {@code BULKHEAD_CALLS.getArgumentClasses()} returns {@link BulkheadResult}, which is an enum with two entries,
 * {@link BulkheadResult#ACCEPTED} and {@link BulkheadResult#REJECTED}.
 */
public enum MetricDefinition {
    INVOCATIONS("ft.invocations.total", Counter.class, InvocationResult.class, InvocationFallback.class), RETRY_CALLS(
            "ft.retry.calls.total", Counter.class, RetryRetried.class,
            RetryResult.class), RETRY_RETRIES("ft.retry.retries.total", Counter.class), TIMEOUT_CALLS(
                    "ft.timeout.calls.total", Counter.class,
                    TimeoutTimedOut.class), TIMEOUT_EXECUTION_DURATION("ft.timeout.executionDuration", Histogram.class,
                            MetricUnits.NANOSECONDS), CIRCUITBREAKER_CALLS("ft.circuitbreaker.calls.total",
                                    Counter.class, CircuitBreakerResult.class), CIRCUITBREAKER_STATE(
                                            "ft.circuitbreaker.state.total", Gauge.class, MetricUnits.NANOSECONDS,
                                            CircuitBreakerState.class), CIRCUITBREAKER_OPENED(
                                                    "ft.circuitbreaker.opened.total", Counter.class), BULKHEAD_CALLS(
                                                            "ft.bulkhead.calls.total", Counter.class,
                                                            BulkheadResult.class), BULKHEAD_EXECUTIONS_RUNNING(
                                                                    "ft.bulkhead.executionsRunning",
                                                                    Gauge.class), BULKHEAD_EXECUTIONS_WAITING(
                                                                            "ft.bulkhead.executionsWaiting",
                                                                            Gauge.class), BULKHEAD_RUNNING_DURATION(
                                                                                    "ft.bulkhead.runningDuration",
                                                                                    Histogram.class,
                                                                                    MetricUnits.NANOSECONDS), BULKHEAD_WAITING_DURATION(
                                                                                            "ft.bulkhead.waitingDuration",
                                                                                            Histogram.class,
                                                                                            MetricUnits.NANOSECONDS);

    private String name;
    private String unit;
    private Class<? extends Metric> metricClass;
    private Class<? extends TagValue>[] tagClasses;

    @SafeVarargs
    private MetricDefinition(String name, Class<? extends Metric> metricClass, String unit,
            Class<? extends TagValue>... tagClasses) {
        this.name = name;
        this.unit = unit;
        this.metricClass = metricClass;
        this.tagClasses = tagClasses;
    }

    @SafeVarargs
    private MetricDefinition(String name, Class<? extends Metric> metricClass,
            Class<? extends TagValue>... tagClasses) {
        this(name, metricClass, MetricUnits.NONE, tagClasses);
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
     * The subclass of {@link Metric} used by this metric
     * 
     * @return the metric class
     */
    public Class<? extends Metric> getMetricClass() {
        return metricClass;
    }

    /**
     * The tags which are applied to this metric
     * <p>
     * The classes returned from this method will be enums which implement {@link TagValue}
     * 
     * @return the tags which are applied to this metric
     */
    public Class<? extends TagValue>[] getTagClasses() {
        return tagClasses;
    }

    public interface TagValue {
        public Tag getTag();
    }

    public enum BulkheadResult implements TagValue {
        ACCEPTED("accepted"), REJECTED("rejected");

        private Tag tag;

        private BulkheadResult(String tagValue) {
            tag = new Tag("bulkheadResult", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum CircuitBreakerResult implements TagValue {
        SUCCESS("success"), FAILURE("failure"), CIRCUIT_BREAKER_OPEN("circuitBreakerOpen");

        private Tag tag;

        private CircuitBreakerResult(String tagValue) {
            tag = new Tag("circuitBreakerResult", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum CircuitBreakerState implements TagValue {
        OPEN("open"), CLOSED("closed"), HALF_OPEN("halfOpen");

        private Tag tag;

        private CircuitBreakerState(String tagValue) {
            tag = new Tag("state", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum InvocationFallback implements TagValue {
        APPLIED("applied"), NOT_APPLIED("notApplied"), NOT_DEFINED("notDefined");

        private Tag tag;

        private InvocationFallback(String tagValue) {
            tag = new Tag("fallback", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum InvocationResult implements TagValue {
        VALUE_RETURNED("valueReturned"), EXCEPTION_THROWN("exceptionThrown");

        private Tag tag;

        private InvocationResult(String tagValue) {
            tag = new Tag("result", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum RetryResult implements TagValue {
        VALUE_RETURNED("valueReturned"), EXCEPTION_NOT_RETRYABLE("exceptionNotRetryable"), MAX_RETRIES_REACHED(
                "maxRetriesReached"), MAX_DURATION_REACHED("maxDurationReached");

        private Tag tag;

        private RetryResult(String tagValue) {
            tag = new Tag("retryResult", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum RetryRetried implements TagValue {
        TRUE("true"), FALSE("false");

        private Tag tag;

        private RetryRetried(String tagValue) {
            tag = new Tag("retried", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

    public enum TimeoutTimedOut implements TagValue {
        TRUE("true"), FALSE("false");

        private Tag tag;

        private TimeoutTimedOut(String tagValue) {
            this.tag = new Tag("timedOut", tagValue);
        }

        public Tag getTag() {
            return tag;
        }
    }

}