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

import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.CIRCUIT_BREAKER_OPEN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.FAILURE;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.CircuitBreakerResult.SUCCESS;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectCbOpen;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.CircuitBreakerMetricBean;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.CircuitBreakerMetricBean.Result;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.CircuitBreakerMetricBean.SkippedException;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.InMemoryMetricReader;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import jakarta.inject.Inject;

public class CircuitBreakerTelemetryTest extends Arquillian {

    private static final long CB_CLOSE_TIMEOUT = TCKConfig.getConfig().getTimeoutInDuration(5000).toNanos();

    @Deployment
    public static WebArchive deploy() {

        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .autoscaleMethod(CircuitBreakerMetricBean.class, "doWork");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftMetricCircuitBreaker.jar")
                .addClasses(CircuitBreakerMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.TELEMETRY_METRIC_UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricCircuitBreaker.war")
                .addAsResource(new StringAsset(
                        "otel.sdk.disabled=false\notel.traces.exporter=none"),
                        "META-INF/microprofile-config.properties")
                .addAsLibrary(jar)
                .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                        PullExporterAutoConfigurationCustomizerProvider.class);

        return war;
    }

    @Inject
    private CircuitBreakerMetricBean cbBean;

    @BeforeTest
    public void closeTheCircuit() throws Exception {

        // Condition is needed because BeforeTest runs on both client and server
        if (cbBean != null) {

            // Assume the circuit is open
            // Attempt to put successful work through it until it stops throwing CircuitBreakerOpenExceptions
            boolean circuitOpen = true;
            long startTime = System.nanoTime();
            while (circuitOpen && System.nanoTime() - startTime < CB_CLOSE_TIMEOUT) {
                try {
                    for (int i = 0; i < 2; i++) {
                        cbBean.doWork(Result.PASS);
                    }
                    circuitOpen = false;
                } catch (CircuitBreakerOpenException e) {
                    Thread.sleep(100);
                }
            }

            if (circuitOpen) {
                throw new RuntimeException("Timed out waiting for circuit breaker to close");
            }
        }
    }

    @Test(groups = "main")
    public void testCircuitBreakerMetric() throws Exception {
        TelemetryMetricGetter m = new TelemetryMetricGetter(CircuitBreakerMetricBean.class, "doWork");

        // First failure, circuit remains closed
        expectTestException(() -> cbBean.doWork(Result.FAIL));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(1L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(0L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(0L));

        // Second failure
        expectTestException(() -> cbBean.doWork(Result.FAIL));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(0L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(1L));

        // Circuit is open, causing failure
        expectCbOpen(() -> cbBean.doWork(Result.PASS));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(1L));

        // Wait a while for the circuit to be half-open
        Thread.sleep(TCKConfig.getConfig().getTimeoutInMillis(5000));

        // Lots of successful work, causing the circuit to close again
        for (int i = 0; i < 2; i++) {
            cbBean.doWork(Result.PASS);
        }

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(2L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(1L));

        // exception that is considered a success
        expect(RuntimeException.class, () -> cbBean.doWork(Result.PASS_EXCEPTION));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(3L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(1L));

        // skipped exception also considered a success
        expect(SkippedException.class, () -> cbBean.doWork(Result.SKIPPED_EXCEPTION));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCalls(SUCCESS).delta(), is(4L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCalls(FAILURE).delta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCalls(CIRCUIT_BREAKER_OPEN).delta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpened().delta(), is(1L));

        // General metrics should be updated
        assertThat("successful invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(),
                is(2L));
        assertThat("failed invocations", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(),
                is(5L));
    }

    @Test(dependsOnGroups = "main")
    public void testMetricUnits() throws InterruptedException, ExecutionException {
        InMemoryMetricReader reader = InMemoryMetricReader.current();

        // Validate that each metric has metadata which declares the correct unit
        for (TelemetryMetricDefinition metric : TelemetryMetricDefinition.values()) {
            if (!metric.getName().startsWith("ft.circuitbreaker")) {
                continue;
            }

            String unit = reader.getUnit(metric.getName());

            if (metric.getUnit() == null) {
                assertTrue(unit.isEmpty(), "Unexpected metadata for metric " + metric.getName());
            } else {
                assertFalse(unit.isEmpty(), "Missing metadata for metric " + metric.getName());
                assertEquals(unit, metric.getUnit(), "Incorrect unit for metric " + metric.getName());
            }
        }
    }
}
