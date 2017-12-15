/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.timeout.clientserver.TimeoutClient;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests to exercise Fault Tolerance Timeouts.
 *
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 */
public class TimeoutGlobalConfigTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftTimeout.jar")
            .addClasses(TimeoutClient.class)
            .addAsManifestResource(new StringAsset(
                "Timeout/value=200"), "microprofile-config.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftTimeout.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * A test to override timeout value by global config.
     * Default timeout is 1 second. We override its value with global property config.
     * The code below will timeout thanks to config
     */
    @Test
    public void testTimeout() {
        try {
            clientForTimeout.serviceA(500);
            Assert.fail("serviceA should throw a TimeoutException in testTimeout");
        }
        catch (TimeoutException ex) {
            // Expected
        }
        catch (RuntimeException ex) {
            // Not Expected
            Assert.fail("serviceA should throw a TimeoutException in testTimeout not a RuntimeException");
        }
    }

    private @Inject
    TimeoutClient clientForTimeout;


}
