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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.BulkheadResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerState;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.TagValue;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.TimeoutTimedOut;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

import jakarta.enterprise.inject.spi.CDI;

/**
 * Retrieves metrics for a specific method
 * <p>
 * The MetricGetter facilitates testing of metrics by providing helper methods for retrieving metrics defined by the
 * faulttolerance spec for a specific class and method name.
 * <p>
 * This class will never create a metric which does not already exist.
 */
public class MetricGetter {

    private MetricRegistryProxy registry;
    private final Tag methodTag;

    private Map<MetricID, CounterMetric> counterMetrics = new HashMap<>();
    private Map<MetricID, GaugeMetric> gaugeMetrics = new HashMap<>();

    public MetricGetter(Class<?> clazz, String methodName) {
        validateClassAndMethodName(clazz, methodName);
        methodTag = new Tag("method", clazz.getCanonicalName() + "." + methodName);
        registry = CDI.current().select(MetricRegistryProxy.class, RegistryTypeLiteral.BASE).get();
    }

    public CounterMetric getInvocations(InvocationResult result, InvocationFallback fallbackUsed) {
        return getCounterMetric(getMetricId(MetricDefinition.INVOCATIONS, result, fallbackUsed));
    }

    public CounterMetric getRetryCalls(RetryRetried retried, RetryResult result) {
        return getCounterMetric(getMetricId(MetricDefinition.RETRY_CALLS, retried, result));
    }

    public CounterMetric getRetryRetries() {
        return getCounterMetric(getMetricId(MetricDefinition.RETRY_RETRIES));
    }

    public CounterMetric getTimeoutCalls(TimeoutTimedOut timedOut) {
        return getCounterMetric(getMetricId(MetricDefinition.TIMEOUT_CALLS, timedOut));
    }

    public Optional<Histogram> getTimeoutExecutionDuration() {
        return getMetric(getMetricId(MetricDefinition.TIMEOUT_EXECUTION_DURATION), Histogram.class);
    }

    public CounterMetric getCircuitBreakerCalls(CircuitBreakerResult cbResult) {
        return getCounterMetric(getMetricId(MetricDefinition.CIRCUITBREAKER_CALLS, cbResult));
    }

    public CounterMetric getCircuitBreakerOpened() {
        return getCounterMetric(getMetricId(MetricDefinition.CIRCUITBREAKER_OPENED));
    }

    public GaugeMetric getCircuitBreakerState(CircuitBreakerState cbState) {
        return getGaugeMetric(getMetricId(MetricDefinition.CIRCUITBREAKER_STATE, cbState));
    }

    public CounterMetric getBulkheadCalls(BulkheadResult bulkheadResult) {
        return getCounterMetric(getMetricId(MetricDefinition.BULKHEAD_CALLS, bulkheadResult));
    }

    public GaugeMetric getBulkheadExecutionsRunning() {
        return getGaugeMetric(getMetricId(MetricDefinition.BULKHEAD_EXECUTIONS_RUNNING));
    }

    public GaugeMetric getBulkheadExecutionsWaiting() {
        return getGaugeMetric(getMetricId(MetricDefinition.BULKHEAD_EXECUTIONS_WAITING));
    }

    public Optional<Histogram> getBulkheadRunningDuration() {
        return getMetric(getMetricId(MetricDefinition.BULKHEAD_RUNNING_DURATION), Histogram.class);
    }

    public Optional<Histogram> getBulkheadWaitingDuration() {
        return getMetric(getMetricId(MetricDefinition.BULKHEAD_WAITING_DURATION), Histogram.class);
    }

    /**
     * Calls {@code baseline()} on all relevant metrics.
     * <p>
     * Extracts all of the {@code Counter} and {@code Gauge} metrics from {@link MetricDefinition} and calls
     * {@link CounterMetric#baseline()} or {@link GaugeMetric#baseline()} on them.
     * <p>
     * This allows us to check how they've changed later in the test using the {@code CounterMetric.delta()} or
     * {@code GaugeMetric.delta()} methods, without having to explicitly baseline every metric ourselves up front.
     */
    public void baselineMetrics() {
        for (MetricDefinition definition : MetricDefinition.values()) {
            for (TagValue[] tags : getTagCombinations(definition.getTagClasses())) {
                MetricID id = getMetricId(definition, tags);
                if (definition.getMetricClass() == Counter.class) {
                    getCounterMetric(id).baseline();
                }
                if (definition.getMetricClass() == Gauge.class) {
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
     * We can use this to iterate across all of the {@link MetricID}s which could be created for a metric which has
     * multiple tags.
     * <p>
     * If called with no arguments, this method returns an array containing an empty array (indicating the only possible
     * combination the the one with no tag values at all).
     * 
     * @param tagValueClazzes
     *            the set of tags
     * @return every possible combination when taking one value for each of the given tags
     */
    @SafeVarargs
    public static final TagValue[][] getTagCombinations(Class<? extends TagValue>... tagValueClazzes) {
        int combinations = 1;
        for (Class<? extends TagValue> clazz : tagValueClazzes) {
            combinations *= clazz.getEnumConstants().length;
        }

        TagValue[][] result = new TagValue[combinations][];
        List<List<TagValue>> tagLists = getTagCombinations(Arrays.asList(tagValueClazzes));
        for (int i = 0; i < tagLists.size(); i++) {
            List<TagValue> tagList = tagLists.get(i);
            result[i] = tagList.toArray(new TagValue[tagList.size()]);
        }
        return result;
    }

    private static List<List<TagValue>> getTagCombinations(List<Class<? extends TagValue>> tagValueClazzes) {
        if (tagValueClazzes.isEmpty()) {
            return Collections.singletonList(Collections.emptyList());
        }

        List<List<TagValue>> result = new ArrayList<>();
        Class<? extends TagValue> firstClazz = tagValueClazzes.get(0);
        for (TagValue value : firstClazz.getEnumConstants()) {
            for (List<TagValue> tagList : getTagCombinations(tagValueClazzes.subList(1, tagValueClazzes.size()))) {
                ArrayList<TagValue> newList = new ArrayList<>();
                newList.add(value);
                newList.addAll(tagList);
                result.add(newList);
            }
        }
        return result;
    }

    /**
     * Creates a {@link MetricID} for a {@link MetricDefinition} and a set of tag values.
     * <p>
     * This method will check that the {@code TagValue}s passed in match the value of
     * {@link MetricDefinition#getTagClasses()}.
     * 
     * @param metricDefinition
     *            the definition of the metric
     * @param metricTags
     *            the values for the tags of the metric
     * @return the MetricID for {@code metricDefinition} with the tags in {@code metricTags}
     */
    private MetricID getMetricId(MetricDefinition metricDefinition, TagValue... metricTags) {
        if (metricDefinition.getTagClasses().length != metricTags.length) {
            throw new IllegalArgumentException("Wrong number of arguments passed for " + metricDefinition);
        }

        Tag[] tags = new Tag[metricTags.length + 1];

        tags[0] = methodTag;

        for (int i = 0; i < metricTags.length; i++) {
            Class<?> argClazz = metricDefinition.getTagClasses()[i];
            if (!argClazz.isInstance(metricTags[i])) {
                throw new IllegalArgumentException("Argument " + i + " has the wrong type. "
                        + "Was " + metricTags[i].getClass() + " but expected " + argClazz);
            }

            tags[i + 1] = metricTags[i].getTag();
        }

        return new MetricID(metricDefinition.getName(), tags);
    }

    /**
     * Get or create the {@link CounterMetric} for the given {@link MetricID}
     * <p>
     * Each created {@code CounterMetric} will be stored and calling this method twice with the same {@code MetricID}
     * will return the same {@code CounterMetric}.
     * 
     * @param metricId
     *            the {@code MetricID}
     * @return the {@code CounterMetric} for {@code metricId}
     */
    private CounterMetric getCounterMetric(MetricID metricId) {
        return counterMetrics.computeIfAbsent(metricId, m -> new CounterMetric(registry, m));
    }

    /**
     * Get or create the {@link GaugeMetric} for the given {@link MetricID}
     * <p>
     * Each created {@code GaugeMetric} will be stored and calling this method twice with the same {@code MetricID} will
     * return the same {@code GaugeMetric}.
     * 
     * @param metricId
     *            the {@code MetricID}
     * @return the {@code GaugeMetric} for {@code metricId}
     */
    private GaugeMetric getGaugeMetric(MetricID metricId) {
        return gaugeMetrics.computeIfAbsent(metricId, m -> new GaugeMetric(registry, m));
    }

    private <T> Optional<T> getMetric(MetricID id, Class<T> metricType) {
        Metric m = registry.getMetrics().get(id);
        if (m != null) {
            assertThat("Metric " + id, m, instanceOf(metricType));
            return Optional.of(metricType.cast(m));
        } else {
            return Optional.empty();
        }
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
