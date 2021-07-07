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

import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import jakarta.enterprise.util.AnnotationLiteral;

/**
 * Literal for {@link RegistryType}
 * <p>
 * Constants are provided for the base and vendor registries.
 */
@SuppressWarnings("serial")
public class RegistryTypeLiteral extends AnnotationLiteral<RegistryType> implements RegistryType {

    public static final RegistryTypeLiteral BASE = new RegistryTypeLiteral(Type.BASE);
    public static final RegistryTypeLiteral VENDOR = new RegistryTypeLiteral(Type.VENDOR);

    private Type type;

    private RegistryTypeLiteral(Type type) {
        this.type = type;
    }

    @Override
    public Type type() {
        return type;
    }
}
