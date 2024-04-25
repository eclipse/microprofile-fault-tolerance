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

import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.AttributeValue;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.BulkheadResult;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerState;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.TimeoutTimedOut;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

/**
 * Retrieves metrics for a specific method
 * <p>
 * The MetricGetter facilitates testing of metrics by providing helper methods for retrieving metrics defined by the
 * faulttolerance spec for a specific class and method name.
 * <p>
 * This class will never create a metric which does not already exist.
 */
public class TelemetryMetricGetter {

    private final String classMethodName;

    private Map<TelemetryMetricID, TelemetryCounterMetric> counterMetrics = new HashMap<>();
    private Map<TelemetryMetricID, TelemetryGaugeMetric> gaugeMetrics = new HashMap<>();
    private Map<TelemetryMetricID, TelemetryHistogramMetric> histogramMetrics = new HashMap<>();

    public TelemetryMetricGetter(Class<?> clazz, String methodName) {
        validateClassAndMethodName(clazz, methodName);
        classMethodName = clazz.getCanonicalName() + "." + methodName;
    }

    public TelemetryCounterMetric getInvocations(InvocationResult result, InvocationFallback fallbackUsed) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.INVOCATIONS, result, fallbackUsed));
    }

    public TelemetryCounterMetric getRetryCalls(RetryRetried retried, RetryResult result) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.RETRY_CALLS, retried, result));
    }

    public TelemetryCounterMetric getRetryRetries() {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.RETRY_RETRIES));
    }

    public TelemetryCounterMetric getTimeoutCalls(TimeoutTimedOut timedOut) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.TIMEOUT_CALLS, timedOut));
    }

    public TelemetryHistogramMetric getTimeoutExecutionDuration() {
        return getHistogramMetric(getMetricId(TelemetryMetricDefinition.TIMEOUT_EXECUTION_DURATION));
    }

    public TelemetryCounterMetric getCircuitBreakerCalls(CircuitBreakerResult cbResult) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.CIRCUITBREAKER_CALLS, cbResult));
    }

    public TelemetryCounterMetric getCircuitBreakerOpened() {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.CIRCUITBREAKER_OPENED));
    }

    public TelemetryCounterMetric getCircuitBreakerState(CircuitBreakerState cbState) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.CIRCUITBREAKER_STATE, cbState));
    }

    public TelemetryCounterMetric getBulkheadCalls(BulkheadResult bulkheadResult) {
        return getCounterMetric(getMetricId(TelemetryMetricDefinition.BULKHEAD_CALLS, bulkheadResult));
    }

    public TelemetryGaugeMetric getBulkheadExecutionsRunning() {
        return getGaugeMetric(getMetricId(TelemetryMetricDefinition.BULKHEAD_EXECUTIONS_RUNNING));
    }

    public TelemetryGaugeMetric getBulkheadExecutionsWaiting() {
        return getGaugeMetric(getMetricId(TelemetryMetricDefinition.BULKHEAD_EXECUTIONS_WAITING));
    }

    public TelemetryHistogramMetric getBulkheadRunningDuration() {
        return getHistogramMetric(getMetricId(TelemetryMetricDefinition.BULKHEAD_RUNNING_DURATION));
    }

    public TelemetryHistogramMetric getBulkheadWaitingDuration() {
        return getHistogramMetric(getMetricId(TelemetryMetricDefinition.BULKHEAD_WAITING_DURATION));
    }

    /**
     * Calls {@code baseline()} on all relevant metrics.
     * <p>
     * Extracts all of the {@code Counter} and {@code Gauge} metrics from {@link TelemetryMetricDefinition} and calls
     * {@link TelemetryCounterMetric#baseline()} or {@link TelemetryGaugeMetric#baseline()} on them.
     * <p>
     * This allows us to check how they've changed later in the test using the {@code CounterMetric.delta()} or
     * {@code GaugeMetric.delta()} methods, without having to explicitly baseline every metric ourselves up front.
     */
    public void baselineMetrics() {
        for (TelemetryMetricDefinition definition : TelemetryMetricDefinition.values()) {
            for (AttributeValue[] tags : getTagCombinations(definition.getAttributeClasses())) {
                TelemetryMetricID id = getMetricId(definition, tags);
                if (definition.getMetricType() == TelemetryMetricDefinition.MetricType.COUNTER) {
                    getCounterMetric(id).baseline();
                }
                if (definition.getMetricType() == TelemetryMetricDefinition.MetricType.GAUGE) {
                    getGaugeMetric(id).baseline();
                }
            }
        }
    }

    // -----------------------
    // Private methods
    // -----------------------

    /**
     * Computes all possible values for a set of tags.
     * <p>
     * Given an array of TagValue enums, this method find every combination of values from across this set of tags.
     * <p>
     * For example, if we had two tags {@code foo=[a|b]} and {@code bar=[x|y]}, this method would return
     *
     * <pre>
     * [[foo=a, bar=x],
     *  [foo=a, bar=y],
     *  [foo=b, bar=x],
     *  [foo=b, bar=y]]
     * </pre>
     * <p>
     * We can use this to iterate across all of the {@link TelemetryMetricID}s which could be created for a metric which
     * has multiple tags.
     * <p>
     * If called with no arguments, this method returns an array containing an empty array (indicating the only possible
     * combination the the one with no tag values at all).
     *
     * @param tagValueClazzes
     *            the set of tags
     * @return every possible combination when taking one value for each of the given tags
     */
    @SafeVarargs
    public static final AttributeValue[][] getTagCombinations(Class<? extends AttributeValue>... tagValueClazzes) {
        int combinations = 1;
        for (Class<? extends AttributeValue> clazz : tagValueClazzes) {
            combinations *= clazz.getEnumConstants().length;
        }

        AttributeValue[][] result = new AttributeValue[combinations][];
        List<List<AttributeValue>> tagLists = getTagCombinations(Arrays.asList(tagValueClazzes));
        for (int i = 0; i < tagLists.size(); i++) {
            List<AttributeValue> tagList = tagLists.get(i);
            result[i] = tagList.toArray(new AttributeValue[tagList.size()]);
        }
        return result;
    }

    private static List<List<AttributeValue>> getTagCombinations(
            List<Class<? extends AttributeValue>> tagValueClazzes) {
        if (tagValueClazzes.isEmpty()) {
            return Collections.singletonList(Collections.emptyList());
        }

        List<List<AttributeValue>> result = new ArrayList<>();
        Class<? extends AttributeValue> firstClazz = tagValueClazzes.get(0);
        for (AttributeValue value : firstClazz.getEnumConstants()) {
            for (List<AttributeValue> tagList : getTagCombinations(
                    tagValueClazzes.subList(1, tagValueClazzes.size()))) {
                ArrayList<AttributeValue> newList = new ArrayList<>();
                newList.add(value);
                newList.addAll(tagList);
                result.add(newList);
            }
        }
        return result;
    }

    /**
     * Creates a {@link TelemetryMetricID} for a {@link TelemetryMetricDefinition} and a set of tag values.
     * <p>
     * This method will check that the {@code TagValue}s passed in match the value of
     * {@link TelemetryMetricDefinition#getTagClasses()}.
     *
     * @param metricDefinition
     *            the definition of the metric
     * @param metricTags
     *            the values for the tags of the metric
     * @return the MetricID for {@code metricDefinition} with the tags in {@code metricTags}
     */
    private TelemetryMetricID getMetricId(TelemetryMetricDefinition metricDefinition,
            AttributeValue... metricAttributes) {
        if (metricDefinition.getAttributeClasses().length != metricAttributes.length) {
            throw new IllegalArgumentException("Wrong number of arguments passed for " + metricDefinition);
        }

        AttributesBuilder builder = Attributes.builder();

        for (int i = 0; i < metricAttributes.length; i++) {
            Class<?> argClazz = metricDefinition.getAttributeClasses()[i];
            if (!argClazz.isInstance(metricAttributes[i])) {
                throw new IllegalArgumentException("Argument " + i + " has the wrong type. "
                        + "Was " + metricAttributes[i].getClass() + " but expected " + argClazz);
            }

            builder.putAll(metricAttributes[i].getAttribute());
        }

        builder.put("method", classMethodName);

        return new TelemetryMetricID(metricDefinition.getName(), builder.build());
    }

    /**
     * Get or create the {@link TelemetryCounterMetric} for the given {@link TelemetryMetricID}
     * <p>
     * Each created {@code CounterMetric} will be stored and calling this method twice with the same {@code MetricID}
     * will return the same {@code CounterMetric}.
     *
     * @param metricId
     *            the {@code MetricID}
     * @return the {@code CounterMetric} for {@code metricId}
     */
    private TelemetryCounterMetric getCounterMetric(TelemetryMetricID metricId) {
        return counterMetrics.computeIfAbsent(metricId, m -> new TelemetryCounterMetric(m));
    }

    /**
     * Get or create the {@link TelemetryGaugeMetric} for the given {@link TelemetryMetricID}
     * <p>
     * Each created {@code GaugeMetric} will be stored and calling this method twice with the same {@code MetricID} will
     * return the same {@code GaugeMetric}.
     *
     * @param metricId
     *            the {@code MetricID}
     * @return the {@code GaugeMetric} for {@code metricId}
     */
    private TelemetryGaugeMetric getGaugeMetric(TelemetryMetricID metricId) {
        return gaugeMetrics.computeIfAbsent(metricId, m -> new TelemetryGaugeMetric(m));
    }

    private TelemetryHistogramMetric getHistogramMetric(TelemetryMetricID metricId) {
        return histogramMetrics.computeIfAbsent(metricId, m -> new TelemetryHistogramMetric(m));
    }

    private void validateClassAndMethodName(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return;
            }
        }

        fail("Couldn't find method " + methodName + " on class " + clazz.getCanonicalName());
    }
}
