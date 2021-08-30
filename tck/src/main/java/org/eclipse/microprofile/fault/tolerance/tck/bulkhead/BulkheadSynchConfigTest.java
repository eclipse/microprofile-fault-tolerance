/*
 *******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Bulkhead3ClassSemaphoreBean;
import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * @author Gordon Hutchison
 * @author Andrew Rouse
 */
public class BulkheadSynchConfigTest extends Arquillian {

    /**
     * This is the Arquillian deploy method that controls the contents of the war that contains all the tests.
     *
     * @return the test war "ftBulkheadSynchTest.war"
     */
    @Deployment
    public static WebArchive deploy() {

        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .setGlobally(Bulkhead.class, "value", "5");

        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadSynchConfigTest.jar")
                .addPackage(Bulkhead3ClassSemaphoreBean.class.getPackage())
                .addClass(BulkheadSynchTest.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap.create(WebArchive.class, "ftBulkheadSynchConfigTest.war").addAsLibrary(testJar);
    }

    /**
     * Tests taking Bulkhead3 waiting for 3 and no more workers and change its configuration by propoerty to switch to 5
     * workers. Test wouldn't pass without property config working
     */
    @Test()
    public void testBulkheadClassSemaphore3() {
        BulkheadSynchTest.testBulkhead(5, bhBeanClassSemaphore3::test);
    }

    /*
     * As the FaultTolerance annotation only work on business methods of injected objects we need to inject a variety of
     * these for use by the tests below. The naming convention indicates if the annotation is on a class or method,
     * asynchronous or semaphore based, the size/value of the {@code @Bulkhead} and whether we have queueing or not.
     */
    @Inject
    private Bulkhead3ClassSemaphoreBean bhBeanClassSemaphore3;

}
