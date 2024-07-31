/*
 *******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.util;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

public class TelemetryHistogramMetricTest {

    @Test
    public void testFindBucket() {
        List<Double> boundaries = Arrays.asList(0.5, 1.0, 4.0, 5.0);
        assertEquals(0, TelemetryHistogramMetric.findBucket(boundaries, 0.0));
        assertEquals(0, TelemetryHistogramMetric.findBucket(boundaries, 0.5)); // Upper bounds are inclusive
        assertEquals(1, TelemetryHistogramMetric.findBucket(boundaries, 0.6));
        assertEquals(1, TelemetryHistogramMetric.findBucket(boundaries, 1.0));
        assertEquals(3, TelemetryHistogramMetric.findBucket(boundaries, 4.5));
        assertEquals(4, TelemetryHistogramMetric.findBucket(boundaries, 5.5)); // Anything after the last boundary is in
                                                                               // the last bucket
    }
}
