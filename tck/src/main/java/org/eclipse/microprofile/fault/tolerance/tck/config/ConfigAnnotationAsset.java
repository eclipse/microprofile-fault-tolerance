package org.eclipse.microprofile.fault.tolerance.tck.config;/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import org.jboss.shrinkwrap.api.asset.Asset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Properties;

public class ConfigAnnotationAsset implements Asset {

    private final Properties props = new Properties();

    @Override
    public InputStream openStream() {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            props.store(os, null);
            return new ByteArrayInputStream(os.toByteArray());
        }
        catch (IOException e) {
            // Shouldn't happen since we're only using in memory streams
            throw new RuntimeException("Unexpected error saving properties", e);
        }
    }

    public ConfigAnnotationAsset setValue(final Class<?> clazz, final String method,
                                          final Class<? extends Annotation> annotation, final String value) {
        props.put(keyFor(clazz, method, annotation, "value"), value);
        return this;
    }

    /**
     * Build config key used to enable an annotation for a class and method
     * <p>
     * E.g. {@code com.example.MyClass/myMethod/Retry/enabled}
     *
     * @param clazz      may be null
     * @param method     may be null
     * @param annotation required
     * @return config key
     */
    private String keyFor(final Class<?> clazz, final String method, final Class<? extends Annotation> annotation,
                          final String property) {
        StringBuilder sb = new StringBuilder();
        if (clazz != null) {
            sb.append(clazz.getCanonicalName());
            sb.append("/");
        }

        if (method != null) {
            sb.append(method);
            sb.append("/");
        }

        sb.append(annotation.getSimpleName());
        sb.append("/");
        sb.append(property);

        return sb.toString();
    }

    public void mergeProperties(final Properties properties) {
        this.props.putAll(properties);
    }
}
