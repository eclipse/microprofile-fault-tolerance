/*
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
 */
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import static java.util.stream.Collectors.toList;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTimeout;
import static org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig.getConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricComparator;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class TimeoutMetricTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        final ConfigAnnotationAsset config = new ConfigAnnotationAsset()
            .setValue(TimeoutMetricBean.class,"counterTestWorkForMillis", Timeout.class,getConfig().getTimeoutInStr(500))
            .setValue(TimeoutMetricBean.class,"histogramTestWorkForMillis", Timeout.class,getConfig().getTimeoutInStr(2000));

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricTimeout.war")
                .addClasses(TimeoutMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
            .addAsManifestResource(config, "microprofile-config.properties")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }
    
    @Inject
    private TimeoutMetricBean timeoutBean;
    
    @Test
    public void testTimeoutMetric() {
        MetricGetter m = new MetricGetter(TimeoutMetricBean.class, "counterTestWorkForMillis");
        m.baselineCounters();
        
        expectTimeout(() -> timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(2000))); // Should timeout
        expectTimeout(() -> timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(2000))); // Should timeout
        timeoutBean.counterTestWorkForMillis(getConfig().getTimeoutInMillis(100)); // Should not timeout
        
        assertThat("calls timed out", m.getTimeoutCallsTimedOutDelta(), is(2L));
        assertThat("calls not timed out", m.getTimeoutCallsNotTimedOutDelta(), is(1L));
        
        assertThat("invocations", m.getInvocationsDelta(), is(3L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(2L));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testTimeoutHistogram() {
        MetricGetter m = new MetricGetter(TimeoutMetricBean.class, "histogramTestWorkForMillis");
        
        timeoutBean.histogramTestWorkForMillis(getConfig().getTimeoutInMillis(300));
        expectTimeout(() -> timeoutBean.histogramTestWorkForMillis(getConfig().getTimeoutInMillis(5000))); // Will timeout after 2000
        
        Histogram histogram = m.getTimeoutExecutionDuration().get();
        Snapshot snapshot = histogram.getSnapshot();
        List<Long> values = Arrays.stream(snapshot.getValues()).boxed().sorted().collect(toList());
        
        assertThat("Histogram count", histogram.getCount(), is(2L));
        assertThat("SnapshotValues", values, contains(MetricComparator.approxMillis(300),
                                                      MetricComparator.approxMillis(2000)));
    }
    
}
