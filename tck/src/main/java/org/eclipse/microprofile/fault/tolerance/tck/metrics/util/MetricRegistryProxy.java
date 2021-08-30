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

import java.util.Map;
import java.util.SortedMap;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;

/**
 * Interface which includes the methods we need to call on MetricRegistry
 * <p>
 * This allows us to proxy these calls so that this code can run against MP Metrics 2.3 and MP Metrics 3.0 where
 * MetricRegistry was changed from an abstract class to an interface
 */
public interface MetricRegistryProxy {

    Map<MetricID, Metric> getMetrics();

    SortedMap<MetricID, Counter> getCounters();

    @SuppressWarnings("rawtypes") // Must match MetricRegistry signature
    SortedMap<MetricID, Gauge> getGauges();

    Map<String, Metadata> getMetadata();
}
