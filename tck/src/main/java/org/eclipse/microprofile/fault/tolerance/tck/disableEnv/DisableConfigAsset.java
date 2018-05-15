/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.disableEnv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Properties;

import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Asset which writes a config file with lines to enable and disable annotations
 * 
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 */
public class DisableConfigAsset implements Asset {
    
    private Properties props = new Properties();

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
    
    /**
     * Add config entry to disable an annotation on the given class and method
     * 
     * @param clazz the class
     * @param method the method
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset disable(Class<?> clazz, String method, Class<? extends Annotation> annotation) {
        props.put(keyFor(clazz, method, annotation), "false");
        return this;
    }
    
    /**
     * Add config entry to disable an annotation on the given class
     * 
     * @param clazz the class
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset disable(Class<?> clazz, Class<? extends Annotation> annotation) {
        props.put(keyFor(clazz, null, annotation), "false");
        return this;
    }
    
    /**
     * Add config entry to disable an annotation globally
     * 
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset disable(Class<? extends Annotation> annotation) {
        props.put(keyFor(null, null, annotation), "false");
        return this;
    }

    /**
     * Add config entry to enable an annotation on the given class and method
     * 
     * @param clazz the class
     * @param method the method
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset enable(Class<?> clazz, String method, Class<? extends Annotation> annotation) {
        props.put(keyFor(clazz, method, annotation), "true");
        return this;
    }
    
    /**
     * Add config entry to enable an annotation on the given class
     * 
     * @param clazz the class
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset enable(Class<?> clazz, Class<? extends Annotation> annotation) {
        props.put(keyFor(clazz, null, annotation), "true");
        return this;
    }
    
    /**
     * Add config entry to enable an annotation globally
     * 
     * @param annotation the annotation
     * @return itself
     */
    public DisableConfigAsset enable(Class<? extends Annotation> annotation) {
        props.put(keyFor(null, null, annotation), "true");
        return this;
    }

    
    /**
     * Build config key used to enable an annotation for a class and method
     * <p>
     * E.g. {@code com.example.MyClass/myMethod/Retry/enabled}
     * 
     * @param clazz may be null
     * @param method may be null
     * @param annotation required
     * @return config key
     */
    private String keyFor(Class<?> clazz, String method, Class<? extends Annotation> annotation) {
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
        sb.append("enabled");
        
        return sb.toString();
    }

}
