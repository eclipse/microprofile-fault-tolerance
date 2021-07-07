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

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.TagValue;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.TimeoutTimedOut;
import org.testng.annotations.Test;

/**
 * Tests for MetricGetterTest.getTagCombinations()
 */
public class MetricGetterTest {

    @Test
    public void testTagComboZero() {
        TagValue[][] expected = {{}};
        assertEquals(MetricGetter.getTagCombinations(), expected);
    }

    @Test
    public void testTagComboOne() {
        TagValue[][] expected = {{TimeoutTimedOut.TRUE}, {TimeoutTimedOut.FALSE}};
        assertEquals(MetricGetter.getTagCombinations(TimeoutTimedOut.class), expected);
    }

    @Test
    public void testTagComboTwo() {
        TagValue[][] expected = {{TimeoutTimedOut.TRUE, InvocationResult.VALUE_RETURNED},
                {TimeoutTimedOut.TRUE, InvocationResult.EXCEPTION_THROWN},
                {TimeoutTimedOut.FALSE, InvocationResult.VALUE_RETURNED},
                {TimeoutTimedOut.FALSE, InvocationResult.EXCEPTION_THROWN}};
        assertEquals(MetricGetter.getTagCombinations(TimeoutTimedOut.class, InvocationResult.class), expected);
    }

}
