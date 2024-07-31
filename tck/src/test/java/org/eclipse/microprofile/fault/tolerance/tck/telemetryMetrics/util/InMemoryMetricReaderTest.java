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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.PointData;

public class InMemoryMetricReaderTest {

    @Test
    public void testHasAllAttributes() {
        AttributeKey<Long> key1 = AttributeKey.longKey("key1");
        AttributeKey<String> key2 = AttributeKey.stringKey("key2");
        AttributeKey<List<Long>> key3 = AttributeKey.longArrayKey("key3");
        AttributeKey<List<String>> key4 = AttributeKey.stringArrayKey("key4");

        Attributes expected = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value2")
                .put(key3, Arrays.asList(1L, 2L, 3L))
                .build();

        Predicate<PointData> hasExpectedAttributes = InMemoryMetricReader.hasAllAttributes(expected);

        Attributes identical = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value2")
                .put(key3, Arrays.asList(1L, 2L, 3L))
                .build();

        assertTrue(hasExpectedAttributes.test(pointData(identical)), "identical");

        Attributes differentLongValue = Attributes.builder()
                .put(key1, 7)
                .put(key2, "value2")
                .put(key3, Arrays.asList(1L, 2L, 3L))
                .build();

        assertFalse(hasExpectedAttributes.test(pointData(differentLongValue)), "differentLongValue");

        Attributes differentStringValue = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value7")
                .put(key3, Arrays.asList(1L, 2L, 3L))
                .build();

        assertFalse(hasExpectedAttributes.test(pointData(differentStringValue)), "differentStringValue");

        Attributes differentListValue = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value2")
                .put(key3, Arrays.asList(1L, 2L, 3L, 7L))
                .build();

        assertFalse(hasExpectedAttributes.test(pointData(differentListValue)), "differentListValue");

        Attributes extraKey = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value2")
                .put(key3, Arrays.asList(1L, 2L, 3L))
                .put(key4, Arrays.asList("value1", "value2"))
                .build();

        // Attributes with additional keys have all the required attributes and so should return true
        assertTrue(hasExpectedAttributes.test(pointData(extraKey)), "extraKey");

        Attributes missingKey = Attributes.builder()
                .put(key1, 1)
                .put(key2, "value2")
                .build();

        assertFalse(hasExpectedAttributes.test(pointData(missingKey)), "missingKey");
    }

    /**
     * Create a mock PointData with the given attributes
     *
     * @param attributes
     *            the attributes
     * @return mock PointData
     */
    private static PointData pointData(Attributes attributes) {
        return new PointData() {

            @Override
            public long getStartEpochNanos() {
                return 0;
            }

            @Override
            public long getEpochNanos() {
                return 0;
            }

            @Override
            public List<? extends ExemplarData> getExemplars() {
                return Collections.emptyList();
            }

            @Override
            public Attributes getAttributes() {
                return attributes;
            }
        };
    }
}
