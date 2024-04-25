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

import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.FallbackMetricBean;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.FallbackMetricBean.Action;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.FallbackMetricBean.NonFallbackException;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.FallbackMetricHandler;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.InMemoryMetricReader;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import jakarta.inject.Inject;

public class FallbackTelemetryTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricFallback.war")
                .addClasses(FallbackMetricBean.class, FallbackMetricHandler.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.TELEMETRY_METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset(
                        "otel.sdk.disabled=false\notel.traces.exporter=none"),
                        "META-INF/microprofile-config.properties")
                .addAsServiceProvider(AutoConfigurationCustomizerProvider.class,
                        PullExporterAutoConfigurationCustomizerProvider.class);
        return war;
    }

    @Inject
    private FallbackMetricBean fallbackBean;

    @Test(groups = "main")
    public void fallbackMetricMethodTest() {
        TelemetryMetricGetter m = new TelemetryMetricGetter(FallbackMetricBean.class, "doWork");
        m.baselineMetrics();

        fallbackBean.setFallbackAction(Action.PASS);
        fallbackBean.doWork(Action.PASS);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.doWork(Action.FAIL);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.setFallbackAction(Action.FAIL);
        expectTestException(() -> fallbackBean.doWork(Action.FAIL));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));

        fallbackBean.setFallbackAction(Action.PASS);
        expectThrows(NonFallbackException.class, () -> fallbackBean.doWork(Action.NON_FALLBACK_EXCEPTION));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));
    }

    @Test(groups = "main")
    public void fallbackMetricHandlerTest() {
        TelemetryMetricGetter m = new TelemetryMetricGetter(FallbackMetricBean.class, "doWorkWithHandler");
        m.baselineMetrics();

        fallbackBean.setFallbackAction(Action.PASS);
        fallbackBean.doWorkWithHandler(Action.PASS);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.doWorkWithHandler(Action.FAIL);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.setFallbackAction(Action.FAIL);
        expectTestException(() -> fallbackBean.doWorkWithHandler(Action.FAIL));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));

        fallbackBean.setFallbackAction(Action.PASS);
        expectThrows(NonFallbackException.class, () -> fallbackBean.doWorkWithHandler(Action.NON_FALLBACK_EXCEPTION));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));
    }

    @Test(dependsOnGroups = "main")
    public void testMetricUnits() throws InterruptedException, ExecutionException {
        InMemoryMetricReader reader = InMemoryMetricReader.current();

        // Validate that each metric has metadata which declares the correct unit
        for (TelemetryMetricDefinition metric : TelemetryMetricDefinition.values()) {
            if (!metric.getName().equals("ft.invocations.total")) {
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
