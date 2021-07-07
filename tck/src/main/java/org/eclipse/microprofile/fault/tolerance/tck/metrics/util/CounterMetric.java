/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
 *******************************************************************************/

package org.eclipse.microprofile.fault.tolerance.tck.metrics.util;

import java.util.Optional;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;

/**
 * Allows tests to get the value of a counter and compare it with a baseline.
 * <p>
 * Most methods on this class will treat a non-existent counter as having a value of zero to allow implementations to
 * lazily create metrics.
 * <p>
 * Most tests should use {@link MetricGetter} to create instances of this class.
 */
public class CounterMetric {

    private MetricRegistryProxy registry;
    private MetricID metricId;
    private long baseline;

    public CounterMetric(MetricRegistryProxy registry, MetricID metricId) {
        this.registry = registry;
        this.metricId = metricId;
        this.baseline = 0;
    }

    /**
     * Get the counter value, or zero if the metric doesn't exist
     * <p>
     * This method will not create the metric if it does not exist.
     * 
     * @return the counter value, or zero if the metric doesn't exist
     */
    public long value() {
        return counter().map(Counter::getCount).orElse(0L);
    }

    /**
     * Capture the current counter value for later comparison with {@link #delta()}
     * <p>
     * This method will not create the metric if it does not exist.
     */
    public void baseline() {
        baseline = value();
    }

    /**
     * Return the difference between the current value of the metric and the value when {@link #baseline} was called.
     * 
     * @return the difference between the metric value and the baseline
     */
    public long delta() {
        return value() - baseline;
    }

    /**
     * Return the actual {@link Counter} object for the metric, if it exists.
     * 
     * @return an {@code Optional} containing the {@code Counter}, or an empty {@code Optional} if the metric does not
     *         exist.
     */
    public Optional<Counter> counter() {
        return Optional.ofNullable(registry.getCounters().get(metricId));
    }

}
