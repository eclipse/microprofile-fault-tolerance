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

import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.expectThrows;

import java.time.Duration;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.RetryMetricBean.CallCounter;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.RetryMetricBean.NonRetryableException;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class RetryMetricTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {

        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .autoscaleMethod(RetryMetricBean.class, "failAfterDelay"); // Scale maxDuration

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftMetricRetry.jar")
                .addClasses(RetryMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricRetry.war")
                .addAsLibrary(jar);

        return war;
    }

    @Inject
    private RetryMetricBean retryBean;

    @Test
    public void testRetryMetricSuccessfulImmediately() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failSeveralTimes");
        m.baselineMetrics();

        retryBean.failSeveralTimes(0, new CallCounter()); // Should succeed on first attempt

        assertRetryCallsIncremented(m, RetryRetried.FALSE, RetryResult.VALUE_RETURNED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(1L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(0L));
    }

    @Test
    public void testRetryMetricSuccessfulAfterRetry() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failSeveralTimes");
        m.baselineMetrics();

        retryBean.failSeveralTimes(3, new CallCounter()); // Should retry 3 times, and eventually succeed

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.VALUE_RETURNED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(3L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(1L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(0L));
    }

    @Test
    public void testRetryMetricNonRetryableImmediately() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failSeveralTimesThenNonRetryable");
        m.baselineMetrics();

        // Should throw non-retryable exception on first attempt
        expectThrows(NonRetryableException.class,
                () -> retryBean.failSeveralTimesThenNonRetryable(0, new CallCounter()));

        assertRetryCallsIncremented(m, RetryRetried.FALSE, RetryResult.EXCEPTION_NOT_RETRYABLE, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(1L));
    }

    @Test
    public void testRetryMetricNonRetryableAfterRetries() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failSeveralTimesThenNonRetryable");
        m.baselineMetrics();

        // Should throw non-retryable exception after 3 retries
        expectThrows(NonRetryableException.class,
                () -> retryBean.failSeveralTimesThenNonRetryable(3, new CallCounter()));

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.EXCEPTION_NOT_RETRYABLE, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(3L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(1L));
    }

    @Test
    public void testRetryMetricMaxRetries() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failSeveralTimes");
        m.baselineMetrics();

        expectTestException(() -> retryBean.failSeveralTimes(20, new CallCounter())); // Should retry 5 times, then fail
        expectTestException(() -> retryBean.failSeveralTimes(20, new CallCounter())); // Should retry 5 times, then fail

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.MAX_RETRIES_REACHED, 2L);
        assertThat("retries", m.getRetryRetries().delta(), is(10L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(2L));
    }

    @Test
    public void testRetryMetricMaxRetriesHitButNoRetry() {
        // This is an edge case which can only occur when maxRetries = 0
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "maxRetriesZero");
        m.baselineMetrics();

        expectTestException(() -> retryBean.maxRetriesZero()); // Should fail immediately and not retry

        assertRetryCallsIncremented(m, RetryRetried.FALSE, RetryResult.MAX_RETRIES_REACHED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(1L));
    }

    @Test
    public void testRetryMetricMaxDuration() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failAfterDelay");
        m.baselineMetrics();

        Duration testDelay = TCKConfig.getConfig().getTimeoutInDuration(100);
        expectTestException(() -> retryBean.failAfterDelay(testDelay)); // Should retry ~10 times, then reach max
                                                                        // duration

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.MAX_DURATION_REACHED, 1L);

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(1L));
    }

    @Test
    public void testRetryMetricMaxDurationNoRetries() {
        MetricGetter m = new MetricGetter(RetryMetricBean.class, "failAfterDelay");
        m.baselineMetrics();

        Duration testDelay = TCKConfig.getConfig().getTimeoutInDuration(1500);
        expectTestException(() -> retryBean.failAfterDelay(testDelay)); // Should fail after first attempt due to
                                                                        // reaching maxDuration

        assertRetryCallsIncremented(m, RetryRetried.FALSE, RetryResult.MAX_DURATION_REACHED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(1L));
    }

    private void assertRetryCallsIncremented(MetricGetter m, RetryRetried retriedValue, RetryResult resultValue,
            Long expectedDelta) {
        for (RetryRetried retried : RetryRetried.values()) {
            for (RetryResult result : RetryResult.values()) {
                if (retried == retriedValue && result == resultValue) {
                    assertThat("Retry calls (" + retried + ", " + result + ")",
                            m.getRetryCalls(retried, result).delta(), is(expectedDelta));
                } else {
                    assertThat("Retry calls (" + retried + ", " + result + ")",
                            m.getRetryCalls(retried, result).delta(), is(0L));
                }
            }
        }
    }

}
