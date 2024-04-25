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

import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.ClashingNameBean;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
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

public class ClashingNameTelemetryTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricClash.war")
                .addClasses(ClashingNameBean.class)
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
    private ClashingNameBean clashingNameBean;

    @Test
    public void testClashingName() throws InterruptedException, ExecutionException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(ClashingNameBean.class, "doWork");
        m.baselineMetrics();

        clashingNameBean.doWork().get();
        clashingNameBean.doWork("dummy").get();

        assertThat("invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(),
                is(greaterThan(0L)));
    }

}
