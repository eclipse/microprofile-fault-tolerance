/*
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.BulkheadResult.ACCEPTED;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.BulkheadResult.REJECTED;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerResult.CIRCUIT_BREAKER_OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerResult.FAILURE;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerResult.SUCCESS;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerState.CLOSED;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.CircuitBreakerState.HALF_OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.TimeoutTimedOut;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Check that metrics are not added when disabled by a config parameter
 */
public class MetricsDisabledTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricDisabled.war")
                .addClasses(AllMetricsBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsResource(new StringAsset("MP_Fault_Tolerance_Metrics_Enabled=false"),
                        "META-INF/microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return war;
    }

    @Inject
    private AllMetricsBean allMetricsBean;

    @Test
    public void testMetricsDisabled() throws InterruptedException, ExecutionException {
        MetricGetter m = new MetricGetter(AllMetricsBean.class, "doWork");
        m.baselineMetrics();

        allMetricsBean.doWork().get(); // Should succeed on first attempt

        // General metrics
        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        // Retry metrics
        assertThat("value returned, no retry", m.getRetryCalls(RetryRetried.FALSE, RetryResult.VALUE_RETURNED).delta(),
                is(0L));
        assertThat("exception thrown, no retry",
                m.getRetryCalls(RetryRetried.FALSE, RetryResult.EXCEPTION_NOT_RETRYABLE).delta(), is(0L));
        assertThat("max retries reached, no retry",
                m.getRetryCalls(RetryRetried.FALSE, RetryResult.MAX_RETRIES_REACHED).delta(), is(0L));
        assertThat("max duration reached, no retry",
                m.getRetryCalls(RetryRetried.FALSE, RetryResult.MAX_DURATION_REACHED).delta(), is(0L));
        assertThat("value returned after retry", m.getRetryCalls(RetryRetried.TRUE, RetryResult.VALUE_RETURNED).delta(),
                is(0L));
        assertThat("exception thrown after retry",
                m.getRetryCalls(RetryRetried.TRUE, RetryResult.EXCEPTION_NOT_RETRYABLE).delta(), is(0L));
        assertThat("max retries reached after retry",
                m.getRetryCalls(RetryRetried.TRUE, RetryResult.MAX_RETRIES_REACHED).delta(), is(0L));
        assertThat("max duration reached after retry",
                m.getRetryCalls(RetryRetried.TRUE, RetryResult.MAX_DURATION_REACHED).delta(), is(0L));
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        // Timeout metrics
        assertThat("timeout execution duration histogram present", m.getTimeoutExecutionDuration().isPresent(),
                is(false));
        assertThat("timed out calls", m.getTimeoutCalls(TimeoutTimedOut.TRUE).delta(), is(0L));
        assertThat("non timed out calls", m.getTimeoutCalls(TimeoutTimedOut.FALSE).delta(), is(0L));

        // CircuitBreaker metrics
        assertThat("circuitbreaker succeeded calls", m.getCircuitBreakerCalls(SUCCESS).delta(), is(0L));
        assertThat("circuitbreaker failed calls", m.getCircuitBreakerCalls(FAILURE).delta(), is(0L));
        assertThat("circuitbreaker prevented calls", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(0L));
        assertThat("circuitbreaker closed time", m.getCircuitBreakerState(CLOSED).delta(), is(0L));
        assertThat("circuitbreaker half open time", m.getCircuitBreakerState(HALF_OPEN).delta(), is(0L));
        assertThat("circuitbreaker open time", m.getCircuitBreakerState(CLOSED).delta(), is(0L));
        assertThat("circuitbreaker times opened", m.getCircuitBreakerOpened().delta(), is(0L));

        // Bulkhead metrics
        assertThat("bulkhead accepted calls", m.getBulkheadCalls(ACCEPTED).delta(), is(0L));
        assertThat("bulkhead rejected calls", m.getBulkheadCalls(REJECTED).delta(), is(0L));
        assertThat("bulkhead executions running present", m.getBulkheadExecutionsRunning().gauge().isPresent(),
                is(false));
        assertThat("bulkhead executions running value", m.getBulkheadExecutionsRunning().value(), is(0L));
        assertThat("bulkhead running duration histogram present", m.getBulkheadRunningDuration().isPresent(),
                is(false));
        assertThat("bulkhead executions waiting present", m.getBulkheadExecutionsWaiting().gauge().isPresent(),
                is(false));
        assertThat("bulkhead executions waiting value", m.getBulkheadExecutionsWaiting().value(), is(0L));
        assertThat("bulkhead queue wait time histogram present", m.getBulkheadWaitingDuration().isPresent(), is(false));
    }

}
