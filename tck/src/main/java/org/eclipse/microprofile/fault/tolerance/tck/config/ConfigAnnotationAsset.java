
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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.shrinkwrap.api.asset.Asset;

public class ConfigAnnotationAsset implements Asset {

    private final Properties props = new Properties();

    @Override
    public InputStream openStream() {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            props.store(os, null);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            // Shouldn't happen since we're only using in memory streams
            throw new RuntimeException("Unexpected error saving properties", e);
        }
    }

    /**
     * Generate config which scales the timeout values of all annotations on a single method
     * <p>
     * The following values are scaled using the scale factor in TCKConfig:
     * <ul>
     * <li>Retry.maxDuration</li>
     * <li>Retry.delay</li>
     * <li>Retry.jitter</li>
     * <li>Timeout.value</li>
     * <li>CircuitBreaker.delay</li>
     * </ul>
     * 
     * @return {@code this}
     */
    public ConfigAnnotationAsset autoscaleMethod(Class<?> clazz, final String method) {
        List<Method> methods = Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(method))
                .collect(Collectors.toList());

        if (methods.size() == 0) {
            throw new RuntimeException("No such method " + method + " on class " + clazz.getName());
        }

        if (methods.size() > 1) {
            throw new RuntimeException("More than one method named " + method + " on class " + clazz.getName());
        }

        autoscaleElement(clazz, methods.get(0));

        return this;
    }

    /**
     * Generate config which scales the timeout values of all annotations on the class
     * <p>
     * Only annotations directly on the class are affected, config is not generated for any annotations on methods. Use
     * {@link #autoscaleMethod(Class, String)} to generate config for methods.
     * <p>
     * The following values are scaled using the scale factor in TCKConfig:
     * <ul>
     * <li>Retry.maxDuration</li>
     * <li>Retry.delay</li>
     * <li>Retry.jitter</li>
     * <li>Timeout.value</li>
     * <li>CircuitBreaker.delay</li>
     * </ul>
     * 
     * @return {@code this}
     */
    public ConfigAnnotationAsset autoscaleClass(Class<?> clazz) {
        autoscaleElement(clazz, null);
        return this;
    }

    /**
     * Generate config which scales the timeout values of all annotations on a class or method
     * <p>
     * If {@code method} is provided, then config will be generated for annotations on the method, otherwise config will
     * be generated for annotations on the class.
     * 
     * @param clazz
     *            the class
     * @param method
     *            the method, may be {@code null}
     */
    private void autoscaleElement(Class<?> clazz, Method method) {
        AnnotatedElement element = clazz;
        String methodName = null;
        if (method != null) {
            element = method;
            methodName = method.getName();
        }

        TCKConfig config = TCKConfig.getConfig();

        Retry retry = element.getAnnotation(Retry.class);
        if (retry != null) {
            Duration maxDuration = Duration.of(retry.maxDuration(), retry.durationUnit());
            props.put(keyFor(clazz, methodName, Retry.class, "maxDuration"),
                    config.getTimeoutInStr(maxDuration.toMillis()));
            props.put(keyFor(clazz, methodName, Retry.class, "maxDurationUnit"), ChronoUnit.MILLIS.name());

            Duration delay = Duration.of(retry.delay(), retry.delayUnit());
            props.put(keyFor(clazz, methodName, Retry.class, "delay"), config.getTimeoutInStr(delay.toMillis()));
            props.put(keyFor(clazz, methodName, Retry.class, "delayUnit"), ChronoUnit.MILLIS.name());

            Duration jitter = Duration.of(retry.jitter(), retry.jitterDelayUnit());
            props.put(keyFor(clazz, methodName, Retry.class, "jitter"), config.getTimeoutInStr(jitter.toMillis()));
            props.put(keyFor(clazz, methodName, Retry.class, "jitterDelayUnit"), ChronoUnit.MILLIS.name());
        }

        Timeout timeout = element.getAnnotation(Timeout.class);
        if (timeout != null) {
            Duration maxDuration = Duration.of(timeout.value(), timeout.unit());
            props.put(keyFor(clazz, methodName, Timeout.class, "value"),
                    config.getTimeoutInStr(maxDuration.toMillis()));
            props.put(keyFor(clazz, methodName, Timeout.class, "unit"), ChronoUnit.MILLIS.name());
        }

        CircuitBreaker cb = element.getAnnotation(CircuitBreaker.class);
        if (cb != null) {
            Duration delay = Duration.of(cb.delay(), cb.delayUnit());
            props.put(keyFor(clazz, methodName, CircuitBreaker.class, "delay"),
                    config.getTimeoutInStr(delay.toMillis()));
            props.put(keyFor(clazz, methodName, CircuitBreaker.class, "delayUnit"), ChronoUnit.MILLIS.name());
        }
    }

    /**
     * Configure the {@code value} parameter on an annotation applied to a method
     * 
     * @param clazz
     *            the class which has the method
     * @param method
     *            the method which has the annotation
     * @param annotation
     *            the annotation type which has the parameter
     * @param value
     *            the value to configure
     * @return {@code this}
     */
    public ConfigAnnotationAsset setValue(final Class<?> clazz, final String method,
            final Class<? extends Annotation> annotation, final String value) {
        props.put(keyFor(clazz, method, annotation, "value"), value);
        return this;
    }

    /**
     * Configure a parameter on an annotation applied to a method
     * 
     * @param clazz
     *            the class which has the method
     * @param method
     *            the name of the method which has the annotation
     * @param annotation
     *            the annotation type which has the parameter
     * @param parameter
     *            the parameter name
     * @param value
     *            the value to configure
     * @return {@code this}
     */
    public ConfigAnnotationAsset set(final Class<?> clazz,
            final String method,
            final Class<? extends Annotation> annotation,
            final String parameter,
            final String value) {
        props.put(keyFor(clazz, method, annotation, parameter), value);
        return this;
    }

    /**
     * Configure the parameter of an annotation globally
     * 
     * @param annotation
     *            the annotation type which has the parameter
     * @param parameter
     *            the parameter name
     * @param value
     *            the value to configure
     * @return {@code this}
     */
    public ConfigAnnotationAsset setGlobally(final Class<? extends Annotation> annotation,
            final String parameter,
            final String value) {
        props.put(keyFor(null, null, annotation, parameter), value);
        return this;
    }

    /**
     * Build config key used to configure a property of an annotation
     * <p>
     * E.g. {@code com.example.MyClass/myMethod/Retry/maxRetries}
     *
     * @param clazz
     *            may be null
     * @param method
     *            may be null
     * @param annotation
     *            required
     * @param property
     *            required
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

    public ConfigAnnotationAsset mergeProperties(final Properties properties) {
        this.props.putAll(properties);
        return this;
    }
}
