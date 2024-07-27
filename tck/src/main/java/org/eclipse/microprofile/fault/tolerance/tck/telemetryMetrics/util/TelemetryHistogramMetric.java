/*
 *******************************************************************************
 * Copyright (c) 2020-2022 Contributors to the Eclipse Foundation
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

import static java.lang.Double.max;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;

/**
 * Allows tests to get the value of a Histogram, it does not support comparing with a baseline
 * <p>
 * Most tests should use {@link TelemetryMetricGetter} to create instances of this class.
 */
public class TelemetryHistogramMetric {

    public static final List<Double> EXPECTED_BOUNDARIES = Arrays.asList(0.005, 0.01, 0.025, 0.05, 0.075, 0.1,
            0.25, 0.5, 0.75, 1d, 2.5, 5d, 7.5, 10d);
    private final TelemetryMetricID metricId;
    private final double MINIMUM_UPPER_BOUND_SECONDS;

    public TelemetryHistogramMetric(TelemetryMetricID metricId) {
        this.metricId = metricId;
        MINIMUM_UPPER_BOUND_SECONDS = TCKConfig.getConfig().getTimeoutInMillis(200) / 1_000d;
    }

    public Optional<HistogramPointData> getHistogramPoint() {
        InMemoryMetricReader reader = InMemoryMetricReader.current();
        return reader.getMetric(metricId)
                .flatMap(md -> InMemoryMetricReader.getHistogramPointData(md, metricId));
    }

    public Optional<Long> getHistogramCount() {
        return getHistogramPoint()
                .map(HistogramPointData::getCount);
    }

    public boolean isPresent() {
        return getHistogramPoint().isPresent();
    }

    public void assertBoundaries() {
        HistogramPointData pointData =
                getHistogramPoint().orElseThrow(() -> new AssertionError("No data point for " + metricId));
        assertThat(pointData.getBoundaries(), equalTo(EXPECTED_BOUNDARIES));
    }

    /**
     * Assert that the bucket counts of the histogram are consistent with the expected set of recorded results.
     * <p>
     * The results are adjusted using {@link TCKConfig#getTimeoutInMillis(long)}.
     * <p>
     * A 20% deviation from the expected time is permitted and the upper limit is extended to 200ms. E.g.:
     * <ul>
     * <li>An expected time of 1000ms will allow a time recorded between 800ms and 1200ms
     * <li>An expected time of 0ms will allow a time recorded between 0ms and 200ms
     * </ul>
     *
     * @param expectedResultsMillis
     *            the list of values which the histogram is expected to have recorded
     */
    public void assertBucketCounts(long... expectedResultsMillis) {
        HistogramPointData pointData =
                getHistogramPoint().orElseThrow(() -> new AssertionError("No data point for " + metricId));

        // Check we have the right number of results in the histogram:
        assertEquals(pointData.getCount(), expectedResultsMillis.length,
                "Wrong number of results in histogram getCounts for " + metricId);
        assertEquals(pointData.getCounts().stream().mapToLong(Long::longValue).sum(), expectedResultsMillis.length,
                "Wrong number of results in histogram buckets for " + metricId);

        // Based on which buckets our expected results may have fallen into, compute the minimum and maximum expected
        // counts for each bucket
        List<Long> minCounts = new ArrayList<Long>(pointData.getCounts());
        minCounts.replaceAll(x -> 0L);
        List<Long> maxCounts = new ArrayList<Long>(pointData.getCounts());
        maxCounts.replaceAll(x -> 0L);

        List<Double> expectedResultsSeconds = Arrays.stream(expectedResultsMillis)
                .mapToDouble(millis -> TCKConfig.getConfig().getTimeoutInMillis(millis) / 1_000d)
                .boxed()
                .collect(Collectors.toList());

        for (double expectedResult : expectedResultsSeconds) {
            int minBucket = findBucket(pointData.getBoundaries(), expectedResult * 0.8);
            int maxBucket =
                    findBucket(pointData.getBoundaries(), max(expectedResult * 1.2, MINIMUM_UPPER_BOUND_SECONDS));
            if (minBucket == maxBucket) {
                // We know the exact bucket the result is expected in
                minCounts.set(minBucket, minCounts.get(minBucket) + 1);
                maxCounts.set(minBucket, maxCounts.get(minBucket) + 1);
            } else {
                // We only know a range of buckets the result could be in
                for (int i = minBucket; i <= maxBucket; i++) {
                    maxCounts.set(i, maxCounts.get(i) + 1);
                }
            }
        }

        List<Integer> wrongCountBuckets = new ArrayList<>();
        List<Long> actualCounts = pointData.getCounts();
        for (int i = 0; i < actualCounts.size(); i++) {
            if (actualCounts.get(i) < minCounts.get(i) || actualCounts.get(i) > maxCounts.get(i)) {
                wrongCountBuckets.add(i);
            }
        }

        if (!wrongCountBuckets.isEmpty()) {
            fail("For metric " + metricId + "\n"
                    + "The following buckets have incorrect counts " + wrongCountBuckets.toString() + "\n"
                    + "      Bucket boundaries: " + pointData.getBoundaries() + "\n"
                    + "     Expected times (s): " + expectedResultsSeconds + "\n"
                    + "Expected minimum counts: " + minCounts + "\n"
                    + "Expected maximum counts: " + maxCounts + "\n"
                    + "   Actual bucket counts: " + actualCounts);
        }
    }

    /**
     * Given a list of bucket boundaries, find the bucket that the given value falls into
     *
     * @param boundaries
     *            the list of boundaries
     * @param value
     *            the value to test
     * @return the bucket index that should contain {@code value}
     */
    public static int findBucket(List<Double> boundaries, double value) {
        // Find the first boundary that's larger than our value
        for (int i = 0; i < boundaries.size(); i++) {
            if (value <= boundaries.get(i)) {
                // If the value is smaller than the first boundary, it falls in the first bucket
                // If the value is larger than the n-1th boundary, but smaller than the nth boundary
                // it falls in the nth bucket
                return i;
            }
        }
        // If the value is larger than all the boundaries, it falls in the last bucket
        // Note: n boundaries implies n+1 buckets
        return boundaries.size();
    }
}
