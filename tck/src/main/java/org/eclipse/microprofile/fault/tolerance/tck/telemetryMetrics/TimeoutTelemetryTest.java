/*
 * Copyright (c) 2018-2022 Contributors to the Eclipse Foundation
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

import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTimeout;
import static org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig.getConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.TimeoutMetricBean;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.InMemoryMetricReader;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.TimeoutTimedOut;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import jakarta.inject.Inject;

public class TimeoutTelemetryTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        Properties props = new Properties();
        props.put("otel.sdk.disabled", "false");
        props.put("otel.traces.exporter", "none");

        final ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .setValue(TimeoutMetricBean.class, "counterTestWorkForMillis", Timeout.class,
                        getConfig().getTimeoutInStr(500))
                .setValue(TimeoutMetricBean.class, "histogramTestWorkForMillis", Timeout.class,
                        getConfig().getTimeoutInStr(2000))
                .mergeProperties(props);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftMetricTimeout.jar")
                .addClasses(TimeoutMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.TELEMETRY_METRIC_UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                        PullExporterAutoConfigurationCustomizerProvider.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricTimeout.war")
                .addAsLibrary(jar);
        return war;
    }

    @Inject
    private TimeoutMetricBean timeoutBean;

    @Test(groups = "main")
    public void testTimeoutMetric() {
        TelemetryMetricGetter m = new TelemetryMetricGetter(TimeoutMetricBean.class, "counterTestWorkForMillis");
        m.baselineMetrics();

        expectTimeout(() -> timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(2000))); // Should
                                                                                                         // timeout
        expectTimeout(() -> timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(2000))); // Should
                                                                                                         // timeout
        timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(100)); // Should not timeout

        assertThat("calls timed out", m.getTimeoutCalls(TimeoutTimedOut.TRUE).delta(), is(2L));
        assertThat("calls not timed out", m.getTimeoutCalls(TimeoutTimedOut.FALSE).delta(), is(1L));

        assertThat("successful invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(),
                is(1L));
        assertThat("failed invocations", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(),
                is(2L));
    }

    @Test(groups = "main")
    public void testTimeoutHistogram() {
        TelemetryMetricGetter m = new TelemetryMetricGetter(TimeoutMetricBean.class, "histogramTestWorkForMillis");

        timeoutBean.histogramTestWorkForMillis(getConfig().getTimeoutInMillis(300));
        expectTimeout(() -> timeoutBean.histogramTestWorkForMillis(getConfig().getTimeoutInMillis(5000))); // Will
                                                                                                           // timeout
                                                                                                           // after 2000

        Long histogramCount = m.getTimeoutExecutionDuration().getHistogramCount().get();
        assertThat("Histogram count", histogramCount, is(2L));
    }

    @Test(dependsOnGroups = "main")
    public void testMetricUnits() throws InterruptedException, ExecutionException {
        InMemoryMetricReader reader = InMemoryMetricReader.current();

        // Validate that each metric has metadata which declares the correct unit
        for (TelemetryMetricDefinition metric : TelemetryMetricDefinition.values()) {
            if (!metric.getName().startsWith("ft.timeout")) {
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
