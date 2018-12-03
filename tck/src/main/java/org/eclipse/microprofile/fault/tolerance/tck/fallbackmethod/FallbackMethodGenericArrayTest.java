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

package org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod;

import static org.testng.Assert.assertEquals;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod.beans.FallbackMethodGenericArrayBeanA;
import org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod.beans.FallbackMethodGenericArrayBeanB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

/**
 * Test for a fallback method with a generic array parameter
 */
public class FallbackMethodGenericArrayTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftFallbackMethodGenericArray.jar")
                .addClasses(FallbackMethodGenericArrayBeanA.class, FallbackMethodGenericArrayBeanB.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftFallbackMethodGenericArray.war")
                .addAsLibrary(testJar);
        return war;
    }
    
    @Inject private FallbackMethodGenericArrayBeanA bean;
    
    @Test
    public void fallbackMethodGenericArray() {
        assertEquals(bean.method(new String[3][4]), "fallback");
    }
    
}
