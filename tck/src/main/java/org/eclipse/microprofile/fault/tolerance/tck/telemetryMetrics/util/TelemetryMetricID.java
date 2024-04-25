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

import java.util.Arrays;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

public class TelemetryMetricID {

    public final String name;
    public final Attributes attributes;

    public TelemetryMetricID(String classMethodName, Attributes attributes) {
        this.name = classMethodName;
        this.attributes = attributes;

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name + " Attributes:");
        for (AttributeKey<?> key : attributes.asMap().keySet()) {
            sb.append("[" + key.toString() + "=" + attributes.asMap().get(key).toString() + "]");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        TelemetryMetricID other = (TelemetryMetricID) o;

        if (name != other.name) {
            return false;
        }

        if (attributes.size() != other.attributes.size()) {
            return false;
        }

        for (AttributeKey<?> key : attributes.asMap().keySet()) {
            if (attributes.get(key) != other.attributes.get(key)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        Map<?, ?> map = attributes.asMap();
        Object[] names = map.entrySet().toArray();
        Object[] value = map.keySet().toArray();

        int namesHash = Arrays.deepHashCode(names);
        int valuesHash = Arrays.deepHashCode(value);

        int hash = 17;
        hash = hash * 31 + namesHash;
        hash = hash * 31 + valuesHash;
        hash = hash * 31 + name.hashCode();

        return hash;
    }

}
