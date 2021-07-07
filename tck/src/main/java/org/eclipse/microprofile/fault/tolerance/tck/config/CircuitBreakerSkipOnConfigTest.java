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

import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test configuring CircuitBreaker.skipOn globally
 */
public class CircuitBreakerSkipOnConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset();
        config.setGlobally(CircuitBreaker.class, "skipOn", TestConfigExceptionA.class.getCanonicalName());

        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "ftCircuitBreakerSkipOnConfig.jar")
                .addPackage(CircuitBreakerConfigTest.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftCircuitBreakerSkipOnConfig.war")
                .addAsLibraries(jar);

        return war;
    }

    @Inject
    private CircuitBreakerConfigBean bean;

    @Test
    public void testConfigureSkipOn() {
        Exceptions.expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
        Exceptions.expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());

        // If skipOn is not configured to include TestConfigExceptionA, this would throw a CircuitBreakerOpenException
        Exceptions.expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
    }

}
