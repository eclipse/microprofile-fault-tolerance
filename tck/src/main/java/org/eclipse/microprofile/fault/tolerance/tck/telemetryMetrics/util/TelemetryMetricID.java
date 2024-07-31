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

import java.util.Map.Entry;
import java.util.Objects;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

public class TelemetryMetricID {

    public final String name;
    public final Attributes attributes;
    public final TelemetryMetricDefinition.MetricType type;

    public TelemetryMetricID(String classMethodName, TelemetryMetricDefinition.MetricType type, Attributes attributes) {
        this.name = classMethodName;
        this.attributes = attributes;
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(" Type: ").append(type);
        sb.append(" Attributes: ");
        for (Entry<AttributeKey<?>, Object> e : attributes.asMap().entrySet()) {
            sb.append("[");
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TelemetryMetricID other = (TelemetryMetricID) obj;
        return Objects.equals(attributes, other.attributes) && Objects.equals(name, other.name) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, name, type);
    }

}
