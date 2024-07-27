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

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.common.util.TimeUtils.approxMillisFromSeconds;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.BulkheadResult.ACCEPTED;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.BulkheadResult.REJECTED;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectBulkheadException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.BulkheadMetricBean;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.common.util.TimeUtils;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.InMemoryMetricReader;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.PullExporterAutoConfigurationCustomizerProvider;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util.TelemetryMetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import jakarta.inject.Inject;

public class BulkheadTelemetryTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricBulkhead.war")
                .addClasses(BulkheadMetricBean.class, TimeUtils.class)
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
    private BulkheadMetricBean bulkheadBean;
    @Inject
    private AsyncCaller async;

    private TCKConfig config = TCKConfig.getConfig();

    private List<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

    /**
     * Ensure that any waiting futures get completed at the end of each test
     * <p>
     * Important in case tests end early due to an exception or failure.
     */
    @AfterMethod
    public void completeWaitingFutures() {
        for (CompletableFuture<Void> future : waitingFutures) {
            future.complete(null);
        }
        waitingFutures.clear();
    }

    /**
     * Use this method to obtain futures for passing to methods on {@link BulkheadMetricBean}
     * <p>
     * Using this factory method ensures they will be completed at the end of the test if your test fails.
     */
    private CompletableFuture<Void> newWaitingFuture() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        waitingFutures.add(result);
        return result;
    }

    @Test(groups = "main")
    public void bulkheadMetricTest() throws InterruptedException, ExecutionException, TimeoutException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(BulkheadMetricBean.class, "waitFor");
        m.baselineMetrics();

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        Future<?> f1 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        Future<?> f2 = async.run(() -> bulkheadBean.waitFor(waitingFuture));

        bulkheadBean.waitForRunningExecutions(2);
        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(2L));

        waitingFuture.complete(null);
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);

        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(0L));
        assertThat("accepted calls", m.getBulkheadCalls(ACCEPTED).delta(), is(2L));
        assertThat("rejected calls", m.getBulkheadCalls(REJECTED).delta(), is(0L));

        // Async metrics should not be present
        assertThat("bulkhead executions waiting present", m.getBulkheadExecutionsWaiting().isPresent(),
                is(false));
        assertThat("bulkhead waiting duration present", m.getBulkheadWaitingDuration().isPresent(), is(false));

        // General metrics should be updated
        assertThat("successful invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(),
                is(2L));
        assertThat("failed invocations", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(),
                is(0L));
    }

    @Test(groups = "main")
    public void bulkheadMetricRejectionTest() throws InterruptedException, ExecutionException, TimeoutException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(BulkheadMetricBean.class, "waitFor");
        m.baselineMetrics();

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        Future<?> f1 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        Future<?> f2 = async.run(() -> bulkheadBean.waitFor(waitingFuture));

        bulkheadBean.waitForRunningExecutions(2);

        Future<?> f3 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        expectBulkheadException(f3);

        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(2L));

        waitingFuture.complete(null);
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);

        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(0L));
        assertThat("accepted calls", m.getBulkheadCalls(ACCEPTED).delta(), is(2L));
        assertThat("rejected calls", m.getBulkheadCalls(REJECTED).delta(), is(1L));

        // General metrics should be updated
        assertThat("successful invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(),
                is(2L));
        assertThat("failed invocations", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(),
                is(1L));

    }

    @Test(groups = "main")
    public void bulkheadMetricHistogramTest() throws InterruptedException, ExecutionException, TimeoutException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(BulkheadMetricBean.class, "waitForHistogram");
        m.baselineMetrics();

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        Future<?> f1 = async.run(() -> bulkheadBean.waitForHistogram(waitingFuture));
        Future<?> f2 = async.run(() -> bulkheadBean.waitForHistogram(waitingFuture));
        bulkheadBean.waitForRunningExecutions(2);
        Future<?> f3 = async.run(() -> bulkheadBean.waitForHistogram(waitingFuture));

        expectBulkheadException(f3);

        Thread.sleep(config.getTimeoutInMillis(1000));

        waitingFuture.complete(null);
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);

        Long executionTimesCount = m.getBulkheadRunningDuration().getHistogramCount().get();
        assertThat("histogram count", executionTimesCount, is(2L)); // Rejected executions
                                                                    // not recorded in

        Optional<HistogramPointData> executionTimesPoint = m.getBulkheadRunningDuration().getHistogramPoint();
        double time = executionTimesPoint
                .map(HistogramPointData::getSum)
                .orElse(0.0);

        long count = executionTimesPoint
                .map(HistogramPointData::getCount)
                .orElse(0L);

        assertThat("mean", time / count, approxMillisFromSeconds(1000)); // histogram

        // Now let's put some quick results through the bulkhead
        bulkheadBean.waitForHistogram(CompletableFuture.completedFuture(null));
        bulkheadBean.waitForHistogram(CompletableFuture.completedFuture(null));

        // Should have 4 results, ~0ms * 2 and ~1000ms * 2
        executionTimesCount = m.getBulkheadRunningDuration().getHistogramCount().get();
        assertThat("histogram count", executionTimesCount, is(4L));
    }

    @Test(groups = "main")
    public void bulkheadMetricAsyncTest() throws InterruptedException, ExecutionException, TimeoutException {
        TelemetryMetricGetter m = new TelemetryMetricGetter(BulkheadMetricBean.class, "waitForAsync");
        m.baselineMetrics();

        CompletableFuture<Void> waitingFuture = newWaitingFuture();

        Future<?> f1 = bulkheadBean.waitForAsync(waitingFuture);
        Future<?> f2 = bulkheadBean.waitForAsync(waitingFuture);
        bulkheadBean.waitForRunningExecutions(2);

        Future<?> f3 = bulkheadBean.waitForAsync(waitingFuture);
        Future<?> f4 = bulkheadBean.waitForAsync(waitingFuture);
        waitForQueuePopulation(m, 2, config.getTimeoutInMillis(2000));

        expectBulkheadException(bulkheadBean.waitForAsync(waitingFuture));

        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(2L));
        assertThat("executions waiting", m.getBulkheadExecutionsWaiting().value(), is(2L));

        Thread.sleep(config.getTimeoutInMillis(1000));
        waitingFuture.complete(null);

        f1.get(1, MINUTES);
        f2.get(1, MINUTES);
        f3.get(1, MINUTES);
        f4.get(1, MINUTES);

        assertThat("executions running", m.getBulkheadExecutionsRunning().value(), is(0L));
        assertThat("accepted calls", m.getBulkheadCalls(ACCEPTED).delta(), is(4L));
        assertThat("rejections", m.getBulkheadCalls(REJECTED).delta(), is(1L));

        Long queueWaits = m.getBulkheadWaitingDuration().getHistogramCount().get();

        // Expect 2 * wait for 0ms, 2 * wait for durationms
        assertThat("waiting duration histogram counts", queueWaits, is(4L));

        // General metrics should be updated
        assertThat("successful invocations", m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(),
                is(4L));
        assertThat("failed invocations", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(),
                is(1L));
    }

    @Test(dependsOnGroups = "main")
    public void testMetricUnits() throws InterruptedException, ExecutionException {
        InMemoryMetricReader reader = InMemoryMetricReader.current();

        // Validate that each metric has metadata which declares the correct unit
        for (TelemetryMetricDefinition metric : TelemetryMetricDefinition.values()) {
            if (!metric.getName().startsWith("ft.bulkhead")) {
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

    private void waitForQueuePopulation(TelemetryMetricGetter m,
            int expectedQueuePopulation,
            long timeoutInMs) throws InterruptedException {
        long timeoutTime = System.currentTimeMillis() + timeoutInMs;
        while (System.currentTimeMillis() < timeoutTime) {
            if (m.getBulkheadExecutionsWaiting().value() == expectedQueuePopulation) {
                return;
            }
            Thread.sleep(100L);
        }
    }

}
