/*
 *******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithTimeout;
import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test the combination of {@code @CircuitBreaker} and {@code @Timeout}
 * 
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 */
public class CircuitBreakerTimeoutTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .autoscaleMethod(CircuitBreakerClientWithTimeout.class, "serviceWithTimeout")
                .autoscaleMethod(CircuitBreakerClientWithTimeout.class, "serviceWithTimeoutWithoutFailOn");

        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerTimeout.jar")
                .addClasses(CircuitBreakerClientWithTimeout.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftCircuitBreakerTimeout.war")
                .addAsLibrary(testJar);
        return war;
    }

    @Inject
    private CircuitBreakerClientWithTimeout timeoutClient;

    /**
     * Test that timeouts cause the circuit to open
     */
    @Test
    public void testTimeout() {
        // Run method twice, should timeout
        for (int i = 0; i < 2; i++) {
            Exceptions.expectTimeout(() -> timeoutClient.serviceWithTimeout());
        }

        // Circuit should now be open, next call should get CircuitBreakerOpenException
        Exceptions.expectCbOpen(() -> timeoutClient.serviceWithTimeout());
    }

    /**
     * Test that timeouts do not cause the circuit to open when failOn attribute does not include TimeoutException
     */
    @Test
    public void testTimeoutWithoutFailOn() {
        // Run method twice, should timeout
        for (int i = 0; i < 2; i++) {
            Exceptions.expectTimeout(() -> timeoutClient.serviceWithTimeoutWithoutFailOn());
        }

        // CircuitBreaker has failOn = TestException so the timeouts should not cause the circuit to open
        // Therefore expect timeout exception, not circuit breaker open exception
        Exceptions.expectTimeout(() -> timeoutClient.serviceWithTimeoutWithoutFailOn());
    }

}
