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
package org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics;

import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.BulkheadResult.ACCEPTED;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.BulkheadResult.REJECTED;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.CIRCUIT_BREAKER_OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.FAILURE;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.SUCCESS;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerState.CLOSED;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerState.HALF_OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerState.OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.AllMetricsBean;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.TimeoutTimedOut;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import jakarta.inject.Inject;

/**
 * Test that metrics are created when all the Fault Tolerance annotations are placed on the same method
 */
public class AllAnnotationTelemetryTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {

        Properties props = new Properties();
        props.put("otel.sdk.disabled", "false");
        props.put("otel.traces.exporter", "none");

        // Scales the following method's annotation values by the TCKConfig baseMultiplier
        ConfigAnnotationAsset allMetricsBeanConfig = new ConfigAnnotationAsset()
                .autoscaleMethod(AllMetricsBean.class, "doWork")
                .mergeProperties(props);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftMetricAll.jar")
                .addClass(AllMetricsBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.TELEMETRY_METRIC_UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(allMetricsBeanConfig, "microprofile-config.properties")
                .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                        PullExporterAutoConfigurationCustomizerProvider.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricAll.war")
                .addAsLibrary(jar);

        return war;
    }

    @Inject
    private AllMetricsBean allMetricsBean;

    @Test
    public void testAllMetrics() throws InterruptedException, ExecutionException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(AllMetricsBean.class, "doWork");
        m.baselineMetrics();

        allMetricsBean.doWork().get(); // Should succeed on first attempt

        // General metrics
        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        // Retry metrics
        assertThat("value returned, no retry", m.getRetryCalls(RetryRetried.FALSE, RetryResult.VALUE_RETURNED).delta(),
                is(1L));
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
                is(true));
        assertThat("timed out calls", m.getTimeoutCalls(TimeoutTimedOut.TRUE).delta(), is(0L));
        assertThat("non timed out calls", m.getTimeoutCalls(TimeoutTimedOut.FALSE).delta(), is(1L));

        // CircuitBreaker metrics
        assertThat("circuitbreaker succeeded calls", m.getCircuitBreakerCalls(SUCCESS).delta(), is(1L));
        assertThat("circuitbreaker failed calls", m.getCircuitBreakerCalls(FAILURE).delta(), is(0L));
        assertThat("circuitbreaker prevented calls", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(0L));
        assertThat("circuitbreaker closed time", m.getCircuitBreakerState(CLOSED).delta(), greaterThan(0L));
        assertThat("circuitbreaker half open time", m.getCircuitBreakerState(HALF_OPEN).delta(), is(0L));
        assertThat("circuitbreaker open time", m.getCircuitBreakerState(OPEN).delta(), is(0L));
        assertThat("circuitbreaker times opened", m.getCircuitBreakerOpened().delta(), is(0L));

        // Bulkhead metrics
        assertThat("bulkhead accepted calls", m.getBulkheadCalls(ACCEPTED).delta(), is(1L));
        assertThat("bulkhead rejected calls", m.getBulkheadCalls(REJECTED).delta(), is(0L));
        assertThat("bulkhead executions running present", m.getBulkheadExecutionsRunning().isPresent(),
                is(true));
        assertThat("bulkhead executions running value", m.getBulkheadExecutionsRunning().value(), is(0L));
        assertThat("bulkhead running duration histogram present", m.getBulkheadRunningDuration().isPresent(),
                is(true));
        assertThat("bulkhead executions waiting present", m.getBulkheadExecutionsWaiting().isPresent(),
                is(true));
        assertThat("bulkhead executions waiting value", m.getBulkheadExecutionsWaiting().value(), is(0L));
        assertThat("bulkhead queue wait time histogram present", m.getBulkheadRunningDuration().isPresent(),
                is(true));
    }

}
