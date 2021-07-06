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

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test configuration of parameters of {@link Fallback}
 */
public class FallbackConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset();
        config.set(FallbackConfigBean.class, "applyOnMethod", Fallback.class, "applyOn",
                TestConfigExceptionA.class.getCanonicalName());
        config.set(FallbackConfigBean.class, "skipOnMethod", Fallback.class, "skipOn",
                TestConfigExceptionA.class.getCanonicalName());
        config.set(FallbackConfigBean.class, "fallbackMethodConfig", Fallback.class, "fallbackMethod",
                "anotherFallback");
        config.set(FallbackConfigBean.class, "fallbackHandlerConfig", Fallback.class, "value",
                FallbackHandlerB.class.getName());

        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "ftFallbackConfigTest.jar")
                .addPackage(FallbackConfigTest.class.getPackage())
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftFallbackConfigTest.war")
                .addAsLibraries(jar);
        return war;
    }

    @Inject
    private FallbackConfigBean bean;

    @Test
    public void testApplyOn() {
        // In annotation: applyOn = TestConfigExceptionB
        // In config: applyOn = TestConfigExceptionA

        // applyOnMethod throws TestConfigExceptionA
        assertEquals("FALLBACK", bean.applyOnMethod());
    }

    @Test
    public void testSkipOn() {
        // In annotation: skipOn = {}
        // In config: skipOn = TestConfigExceptionA

        // skipOnMethod throws TestConfigExceptionA
        Exceptions.expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
    }

    @Test
    public void testFallbackMethod() {
        // In annotation: fallbackMethod = "theFallback"
        // In config: fallbackMethod = "anotherFallback"

        assertEquals("ANOTHER FALLBACK", bean.fallbackMethodConfig());
    }

    @Test
    public void testFallbackHandler() {
        // In annotation: value = FallbackHandlerA
        // In config: value = FallbackHandlerB

        assertEquals("FallbackHandlerB", bean.fallbackHandlerConfig());
    }
}
