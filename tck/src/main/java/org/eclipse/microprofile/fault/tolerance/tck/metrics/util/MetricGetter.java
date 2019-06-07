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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.util.Optional;

import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * Retrieves metrics for a specific method
 * <p>
 * The MetricGetter facilitates testing of metrics by providing helper methods
 * for retrieving metrics defined by the faulttolerance spec for a specific
 * class and method name.
 * 
 * <h3>Counters</h3>
 * Helper methods for counter metrics follow a common pattern:
 * <ul>
 * <li>{@code getXxxxTotal()} (e.g. {@link #getRetryCallsSucceededNotRetriedTotal()}) gets the raw value of the counter</li>
 * <li>{@link #baselineCounters()} records the current value of <i>all</i> the counters for later comparison</li>
 * <li>{@code getXxxxDelta()} (e.g. {@link #getRetryCallsSucceededNotRetriedDelta()}) gets the difference between the current value of the counter,
 *     and the value recorded last time {@link #baselineCounters()} was called</li>
 * </ul>
 */
public class MetricGetter {
    // need to be compatible with both Metrics 1.1 and 2.0 for a while
    private static boolean isMetrics20;
    private static Class<?> metricIdClass;

    static {
        try {
            metricIdClass = Class.forName("org.eclipse.microprofile.metrics.MetricID");
            isMetrics20 = true;
        }
        catch (ClassNotFoundException e) {
            isMetrics20 = false;
        }
    }

    private String prefix;
    private MetricRegistry registry;
    
    // General
    private long invocationsBaseline;
    private long invocationsFailedBaseline;
    
    // Retry
    private long retryCallsSucceededNotRetriedBaseline;
    private long retryCallsSucceededRetriedBaseline;
    private long retryCallsFailedBaseline;
    private long retryRetriesBaseline;
    
    // Timeout
    private long timeoutCallsTimedOutBaseline;
    private long timeoutCallsNotTimedOutBaseline;
    
    // Circuit Breaker
    private long circuitBreakerCallsSucceededBaseline;
    private long circuitBreakerCallsFailedBaseline;
    private long circuitBreakerCallsPreventedBaseline;
    private long circuitBreakerTimeOpenBaseline;
    private long circuitBreakerTimeHalfOpenBaseline;
    private long circuitBreakerTimeClosedBaseline;
    private long circuitBreakerOpenedBaseline;
    
    // Bulkhead
    private long bulkheadCallsAcceptedBaseline;
    private long bulkheadCallsRejectedBaseline;
    
    // Fallback
    private long fallbackCallsBaseline;
    
    public MetricGetter(Class<?> clazz, String methodName) {
        validateClassAndMethodName(clazz, methodName);
        prefix = "ft." + clazz.getCanonicalName() + "." + methodName;
        registry = CDI.current().select(MetricRegistry.class).get();
    }
    
    /**
     * Takes the values of all counters so that we can check how they've
     * changed later in the test using the {@code getDeltaXxx()} methods
     */
    public void baselineCounters() {
        // General
        invocationsBaseline = getInvocationsTotal();
        invocationsFailedBaseline = getInvocationsFailedTotal();
        
        // Retry
        retryCallsSucceededNotRetriedBaseline = getRetryCallsSucceededNotRetriedTotal();
        retryCallsSucceededRetriedBaseline = getRetryCallsSucceededRetriedTotal();
        retryCallsFailedBaseline = getRetryCallsFailedTotal();
        retryRetriesBaseline = getRetryRetriesTotal();
        
        // Timeout
        timeoutCallsTimedOutBaseline = getTimeoutCallsTimedOutTotal();
        timeoutCallsNotTimedOutBaseline = getTimeoutCallsNotTimedOutTotal();
        
        // Circuit Breaker
        circuitBreakerCallsSucceededBaseline = getCircuitBreakerCallsSucceededTotal();
        circuitBreakerCallsFailedBaseline = getCircuitBreakerCallsFailedTotal();
        circuitBreakerCallsPreventedBaseline = getCircuitBreakerCallsPreventedTotal();
        circuitBreakerTimeOpenBaseline = getCircuitBreakerTimeOpenTotal();
        circuitBreakerTimeHalfOpenBaseline = getCircuitBreakerTimeHalfOpenTotal();
        circuitBreakerTimeClosedBaseline = getCircuitBreakerTimeClosedTotal();
        circuitBreakerOpenedBaseline = getCircuitBreakerOpenedTotal();
        
        // Bulkhead
        bulkheadCallsAcceptedBaseline = getBulkheadCallsAcceptedTotal();
        bulkheadCallsRejectedBaseline = getBulkheadCallsRejectedTotal();
        
        // Fallback
        fallbackCallsBaseline = getFallbackCallsTotal();
    }
    
    // ----------------------
    // General Metrics
    // ----------------------
    
    public long getInvocationsTotal() {
        return getCounterValue(prefix + ".invocations.total");
    }
    
    public long getInvocationsDelta() {
        return getInvocationsTotal() - invocationsBaseline;
    }
    
    public long getInvocationsFailedTotal() {
        return getCounterValue(prefix + ".invocations.failed.total");
    }
    
    public long getInvocationsFailedDelta() {
        return getInvocationsFailedTotal() - invocationsFailedBaseline;
    }
    
    // ----------------------
    // Retry Metrics
    // ----------------------
    
    public long getRetryCallsSucceededNotRetriedTotal() {
        return getCounterValue(prefix + ".retry.callsSucceededNotRetried.total");
    }
    
    public long getRetryCallsSucceededNotRetriedDelta() {
        return getRetryCallsSucceededNotRetriedTotal() - retryCallsSucceededNotRetriedBaseline;
    }
    
    public long getRetryCallsSucceededRetriedTotal() {
        return getCounterValue(prefix + ".retry.callsSucceededRetried.total");
    }
    
    public long getRetryCallsSucceededRetriedDelta() {
        return getRetryCallsSucceededRetriedTotal() - retryCallsSucceededRetriedBaseline;
    }
    
    public long getRetryCallsFailedTotal() {
        return getCounterValue(prefix + ".retry.callsFailed.total");
    }
    
    public long getRetryCallsFailedDelta() {
        return getRetryCallsFailedTotal() - retryCallsFailedBaseline;
    }
    
    public long getRetryRetriesTotal() {
        return getCounterValue(prefix + ".retry.retries.total");
    }
    
    public long getRetryRetriesDelta() {
        return getRetryRetriesTotal() - retryRetriesBaseline;
    }
    
    // -----------------------
    // Timeout Metrics
    // -----------------------
    
    public Optional<Histogram> getTimeoutExecutionDuration() {
        return getMetric(prefix + ".timeout.executionDuration", Histogram.class);
    }
    
    public long getTimeoutCallsTimedOutTotal() {
        return getCounterValue(prefix + ".timeout.callsTimedOut.total");
    }
    
    public long getTimeoutCallsTimedOutDelta() {
        return getTimeoutCallsTimedOutTotal() - timeoutCallsTimedOutBaseline;
    }
    
    public long getTimeoutCallsNotTimedOutTotal() {
        return getCounterValue(prefix + ".timeout.callsNotTimedOut.total");
    }
    
    public long getTimeoutCallsNotTimedOutDelta() {
        return getTimeoutCallsNotTimedOutTotal() - timeoutCallsNotTimedOutBaseline;
    }
    
    // -----------------------
    // Circuit Breaker Metrics
    // -----------------------
    
    public long getCircuitBreakerCallsSucceededTotal() {
        return getCounterValue(prefix + ".circuitbreaker.callsSucceeded.total");
    }
    
    public long getCircuitBreakerCallsSucceededDelta() {
        return getCircuitBreakerCallsSucceededTotal() - circuitBreakerCallsSucceededBaseline;
    }
    
    public long getCircuitBreakerCallsFailedTotal() {
        return getCounterValue(prefix + ".circuitbreaker.callsFailed.total");
    }
    
    public long getCircuitBreakerCallsFailedDelta() {
        return getCircuitBreakerCallsFailedTotal() - circuitBreakerCallsFailedBaseline;
    }
    
    public long getCircuitBreakerCallsPreventedTotal() {
        return getCounterValue(prefix + ".circuitbreaker.callsPrevented.total");
    }
    
    public long getCircuitBreakerCallsPreventedDelta() {
        return getCircuitBreakerCallsPreventedTotal() - circuitBreakerCallsPreventedBaseline;
    }
    
    public long getCircuitBreakerTimeOpenTotal() {
        return getGaugeValue(prefix + ".circuitbreaker.open.total", Long.class).orElse(0L);
    }
    
    public long getCircuitBreakerTimeOpenDelta() {
        return getCircuitBreakerTimeOpenTotal() - circuitBreakerTimeOpenBaseline;
    }
    
    public long getCircuitBreakerTimeHalfOpenTotal() {
        return getGaugeValue(prefix + ".circuitbreaker.halfOpen.total", Long.class).orElse(0L);
    }
    
    public long getCircuitBreakerTimeHalfOpenDelta() {
        return getCircuitBreakerTimeHalfOpenTotal() - circuitBreakerTimeHalfOpenBaseline;
    }
    
    public long getCircuitBreakerTimeClosedTotal() {
        return getGaugeValue(prefix + ".circuitbreaker.closed.total", Long.class).orElse(0L);
    }
    
    public long getCircuitBreakerTimeClosedDelta() {
        return getCircuitBreakerTimeClosedTotal() - circuitBreakerTimeClosedBaseline;
    }
    
    public long getCircuitBreakerOpenedTotal() {
        return getCounterValue(prefix + ".circuitbreaker.opened.total");
    }
    
    public long getCircuitBreakerOpenedDelta() {
        return getCircuitBreakerOpenedTotal() - circuitBreakerOpenedBaseline;
    }
    
    // -----------------------
    // Bulkhead Metrics
    // -----------------------
    
    public Optional<Long> getBulkheadConcurrentExecutions() {
        return getGaugeValue(prefix + ".bulkhead.concurrentExecutions", Long.class);
    }
    
    public long getBulkheadCallsAcceptedTotal() {
        return getCounterValue(prefix + ".bulkhead.callsAccepted.total");
    }
    
    public long getBulkheadCallsAcceptedDelta() {
        return getBulkheadCallsAcceptedTotal() - bulkheadCallsAcceptedBaseline;
    }
    
    public long getBulkheadCallsRejectedTotal() {
        return getCounterValue(prefix + ".bulkhead.callsRejected.total");
    }
    
    public long getBulkheadCallsRejectedDelta() {
        return getBulkheadCallsRejectedTotal() - bulkheadCallsRejectedBaseline;
    }
    
    public Optional<Histogram> getBulkheadExecutionDuration() {
        return getMetric(prefix + ".bulkhead.executionDuration", Histogram.class);
    }
    
    public Optional<Long> getBulkheadQueuePopulation() {
        return getGaugeValue(prefix + ".bulkhead.waitingQueue.population", Long.class);
    }
    
    public Optional<Histogram> getBulkheadWaitTime() {
        return getMetric(prefix + ".bulkhead.waiting.duration", Histogram.class);
    }
    
    // -----------------------
    // Fallback Metrics
    // -----------------------
    
    public long getFallbackCallsTotal() {
        return getCounterValue(prefix + ".fallback.calls.total");
    }
    
    public long getFallbackCallsDelta() {
        return getFallbackCallsTotal() - fallbackCallsBaseline;
    }
    
    // -----------------------
    // Private methods
    // -----------------------
    
    private long getCounterValue(String name) {
        return getMetric(name, Counter.class)
                .map(Counter::getCount)
                .orElse(0L);
    }
    
    private <T> Optional<T> getGaugeValue(String name, Class<T> resultType) {
        return getMetric(name, Gauge.class)
                .map((g) -> resultType.cast(g.getValue()));
    }
    
    private <T> Optional<T> getMetric(String name, Class<T> metricType) {
        Object key;
        if (isMetrics20) {
            try {
                key = metricIdClass.getConstructor(String.class).newInstance(name);
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            key = name;
        }
        Metric m = registry.getMetrics().get(key);
        if (m != null) {
            assertThat("Metric " + name, m, instanceOf(metricType));
            return Optional.of(metricType.cast(m));
        }
        else {
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
