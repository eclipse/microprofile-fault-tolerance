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

package org.eclipse.microprofile.fault.tolerance.tck.config.fallback;

import static org.testng.Assert.assertEquals;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.config.TestConfigExceptionA;
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

/**
 * Test configuration of parameters of {@link Fallback}
 */
public class FallbackConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset();
        config.set(FallbackConfigBean.class, "applyOnMethod", Fallback.class, "applyOn", TestConfigExceptionA.class.getCanonicalName());
        config.set(FallbackConfigBean.class, "skipOnMethod", Fallback.class, "skipOn", TestConfigExceptionA.class.getCanonicalName());

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
        // applyOn is configured to include TestConfigExceptionA, so method should fall back
        assertEquals("FALLBACK", bean.applyOnMethod());
    }

    @Test
    public void testSkipOn() {
        // skipOn is configured to include TestConfigExceptionA, so method should throw exception
        Exceptions.expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
    }
}
