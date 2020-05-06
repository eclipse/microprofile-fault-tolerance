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

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricComparator.approxMillis;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricComparator.lessThanMillis;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectBulkheadException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class BulkheadMetricTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {


        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricBulkhead.war")
                .addClasses(BulkheadMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }
    
    @Inject private BulkheadMetricBean bulkheadBean;
    @Inject private AsyncCaller async;
    
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
    
    @Test
    public void bulkheadMetricTest() throws InterruptedException, ExecutionException, TimeoutException {
        MetricGetter m = new MetricGetter(BulkheadMetricBean.class, "waitFor");
        m.baselineCounters();
        
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        Future<?> f1 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        Future<?> f2 = async.run(() -> bulkheadBean.waitFor(waitingFuture));

        bulkheadBean.waitForRunningExecutions(2);
        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(2L));
        
        waitingFuture.complete(null);
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);
        
        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(0L));
        assertThat("accepted calls", m.getBulkheadCallsAcceptedDelta(), is(2L));
        assertThat("rejected calls", m.getBulkheadCallsRejectedDelta(), is(0L));
        
        // Async metrics should not be present
        assertThat("bulkhead queue population present", m.getBulkheadQueuePopulation().isPresent(), is(false));
        assertThat("bulkhead wait time histogram present", m.getBulkheadWaitTime().isPresent(), is(false));
        
        // General metrics should be updated
        assertThat("invocations", m.getInvocationsDelta(), is(2L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
    }
    
    @Test
    public void bulkheadMetricRejectionTest() throws InterruptedException, ExecutionException, TimeoutException {
        MetricGetter m = new MetricGetter(BulkheadMetricBean.class, "waitFor");
        m.baselineCounters();
        
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        Future<?> f1 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        Future<?> f2 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        
        bulkheadBean.waitForRunningExecutions(2);
        
        Future<?> f3 = async.run(() -> bulkheadBean.waitFor(waitingFuture));
        expectBulkheadException(f3);
        
        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(2L));
        
        waitingFuture.complete(null);
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);
        
        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(0L));
        assertThat("accepted calls", m.getBulkheadCallsAcceptedDelta(), is(2L));
        assertThat("rejected calls", m.getBulkheadCallsRejectedDelta(), is(1L));
        
        // General metrics should be updated
        assertThat("invocations", m.getInvocationsDelta(), is(3L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(1L));

    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void bulkheadMetricHistogramTest() throws InterruptedException, ExecutionException, TimeoutException {
        MetricGetter m = new MetricGetter(BulkheadMetricBean.class, "waitForHistogram");
        m.baselineCounters();
        
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
        
        Histogram executionTimes = m.getBulkheadExecutionDuration().get();
        Snapshot snap = executionTimes.getSnapshot();
        
        assertThat("histogram count", executionTimes.getCount(), is(2L)); // Rejected executions not recorded in histogram
        assertThat("median", Math.round(snap.getMedian()), approxMillis(1000));
        assertThat("mean", Math.round(snap.getMean()), approxMillis(1000));
        
        // Now let's put some quick results through the bulkhead
        bulkheadBean.waitForHistogram(CompletableFuture.completedFuture(null));
        bulkheadBean.waitForHistogram(CompletableFuture.completedFuture(null));
        
        // Should have 4 results, ~0ms * 2 and ~1000ms * 2
        snap = executionTimes.getSnapshot();
        assertThat("histogram count", executionTimes.getCount(), is(4L));
        List<Long> values = Arrays.stream(snap.getValues()).sorted().boxed().collect(toList());
        assertThat("histogram values", values, contains(lessThanMillis(500),
                                                        lessThanMillis(500),
                                                        approxMillis(1000),
                                                        approxMillis(1000)));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void bulkheadMetricAsyncTest() throws InterruptedException, ExecutionException, TimeoutException {
        MetricGetter m = new MetricGetter(BulkheadMetricBean.class, "waitForAsync");
        m.baselineCounters();
        
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        Future<?> f1 = bulkheadBean.waitForAsync(waitingFuture);
        Future<?> f2 = bulkheadBean.waitForAsync(waitingFuture);
        bulkheadBean.waitForRunningExecutions(2);
        long startTime = System.nanoTime();

        Future<?> f3 = bulkheadBean.waitForAsync(waitingFuture);
        Future<?> f4 = bulkheadBean.waitForAsync(waitingFuture);
        waitForQueuePopulation(m, 2, config.getTimeoutInMillis(2000));
        
        expectBulkheadException(bulkheadBean.waitForAsync(waitingFuture));

        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(2L));
        assertThat("queue population", m.getBulkheadQueuePopulation().get(), is(2L));
        
        Thread.sleep(config.getTimeoutInMillis(1000));
        waitingFuture.complete(null);
        long durationms = (System.nanoTime() - startTime) / 1_000_000;
        durationms /= config.getBaseMultiplier(); // This value is used with approxMillis which always applies the baseMultiplier
                                                  // so preemptively divide it by the baseMultiplier here
        
        f1.get(1, MINUTES);
        f2.get(1, MINUTES);
        f3.get(1, MINUTES);
        f4.get(1, MINUTES);

        assertThat("concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(0L));
        assertThat("accepted calls", m.getBulkheadCallsAcceptedDelta(), is(4L));
        assertThat("rejections", m.getBulkheadCallsRejectedDelta(), is(1L));
        
        Histogram queueWaits = m.getBulkheadWaitTime().get();
        Snapshot snap = queueWaits.getSnapshot();
        List<Long> values = Arrays.stream(snap.getValues()).sorted().boxed().collect(toList());
        
        // Expect 2 * wait for 0ms, 2 * wait for durationms
        assertThat("queue wait histogram counts", queueWaits.getCount(), is(4L));
        assertThat("queue wait histogram values", values, contains(lessThanMillis(500),
                                                                   lessThanMillis(500),
                                                                   approxMillis(durationms),
                                                                   approxMillis(durationms)));
        
        // General metrics should be updated
        assertThat("invocations", m.getInvocationsDelta(), is(5L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(1L));
    }

    private void waitForQueuePopulation(MetricGetter m,
                                        int expectedQueuePopulation,
                                        long timeoutInMs) throws InterruptedException {
        long timeoutTime = System.currentTimeMillis() + timeoutInMs;
        while (System.currentTimeMillis() < timeoutTime) {
            if (m.getBulkheadQueuePopulation().orElse(0L) == expectedQueuePopulation) {
                return;
            }
            Thread.sleep(100L);
        }
    }

}
