/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
package org.testng;

/**
 * @deprecated only for temporary switch to junit
 */
@Deprecated
public class Assert {
    private Assert() {
        // ignore
    }
    public static void fail(String message) {
        org.junit.Assert.fail(message);
    }

    public static void fail(String message, Throwable ignore) {
        org.junit.Assert.fail(message);
    }

    public static void assertEquals(int actual, int expected, String message) {
        org.junit.Assert.assertEquals(message, expected, actual);
    }

    public static void assertTrue(boolean condition, String message) {
        org.junit.Assert.assertTrue(message, condition);
    }

    public static void assertFalse(boolean condition, String message) {
        org.junit.Assert.assertFalse(message, condition);
    }

    public static void assertNull(Object o) {
        org.junit.Assert.assertNull(o);
    }
}
