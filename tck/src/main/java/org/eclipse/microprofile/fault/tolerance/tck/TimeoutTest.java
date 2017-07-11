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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests to exercise Fault Tolerance Timeouts.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class TimeoutTest extends Arquillian {

    private @Inject TimeoutClient clientForTimeout;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftTimeout.jar").addClasses(TimeoutClient.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftTimeout.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * A test to exercise the default timeout. The default Fault Tolerance
     * timeout is 1 second but serviceA will attempt to sleep for 20 seconds, so
     * should throw a TimeoutException.
     * 
     */
    @Test
    public void testTimeout() {
        try {
            clientForTimeout.serviceA(20000);
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

    /**
     * A test that should not time out. The default Fault Tolerance timeout is 1
     * second but serviceA will attempt to sleep for only 10 milliseconds before
     * throwing a RuntimeException. There should be no Timeout.
     * 
     */
    @Test
    public void testNoTimeout() {
        try {
            clientForTimeout.serviceA(10);
            Assert.fail("serviceB should throw a RuntimeException in testNoTimeout");
        } 
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceB should throw a RuntimeException in testNoTimeout not a TimeoutException");
        } 
        catch (RuntimeException ex) {
            // Expected
        }
    }
    
    /**
     * A test that should time out. The Fault Tolerance timeout is set to a (non-default) 2 seconds but serviceB 
     * will attempt to sleep for 2.5 seconds - so longer than a  default timeout.
     */
    @Test
    public void testGreaterThanDefaultTimeout() {
        try {
            clientForTimeout.serviceB(2500);
            Assert.fail("serviceB should throw a TimeoutException in testGreaterThanDefaultTimeout");
        } 
        catch (TimeoutException ex) {
            // Expected
        } 
        catch (RuntimeException ex) {
            // Not Expected
            Assert.fail("serviceB should throw a TimeoutException in testGreaterThanDefaultTimeout not a RuntimeException");
        }
    }
    
    /**
     * A test that should not time out. The Fault Tolerance timeout is set to 2
     * seconds but serviceB will attempt to sleep for 1.5 seconds - so longer than a  default
     * timeout but shorter than the timeout that has been configured, before throwing a 
     * RuntimeException. There should be no Timeout.
     * 
     */
    @Test
    public void testGreaterThanDefaultNoTimeout() {
        try {
            clientForTimeout.serviceB(1500);
            Assert.fail("serviceB should throw a RuntimeException in testGreaterThanDefaultNoTimeout");
        } 
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceB should throw a RuntimeException in testGreaterThanDefaultNoTimeout not a TimeoutException");
        } 
        catch (RuntimeException ex) {
            // Expected
        }
    }
    
    /**
     * A test that should time out. The Fault Tolerance timeout is set to a (non-default) 0.5 seconds but serviceC 
     * will attempt to sleep for 1 second - so longer than a  default timeout.
     */
    @Test
    public void testLessThanDefaultTimeout() {
        try {
            clientForTimeout.serviceC(1000);
            Assert.fail("serviceC should throw a TimeoutException in testLessThanDefaultTimeout");
        } 
        catch (TimeoutException ex) {
            // Expected
        } 
        catch (RuntimeException ex) {
            // Not Expected
            Assert.fail("serviceC should throw a TimeoutException in testLessThanDefaultTimeout not a RuntimeException");
        }
    }    
    /**
     * A test that should not time out. The Fault Tolerance timeout is set to a (non-default) 0.5
     * seconds but serviceC will attempt to sleep for only 10 milliseconds before
     * throwing a RuntimeException. There should be no Timeout.
     * 
     */
    @Test
    public void testLessThanDefaultNoTimeout() {
        try {
            clientForTimeout.serviceC(10);
            Assert.fail("serviceC should throw a RuntimeException in testLessThanDefaultNoTimeout");
        } 
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceC should throw a RuntimeException in testLessThanDefaultNoTimeout not a TimeoutException");
        } 
        catch (RuntimeException ex) {
            // Expected
        }
    }
    
    /**
     * A test that should time out. The Fault Tolerance timeout is set to a (non-default) 2 seconds but serviceD 
     * will attempt to sleep for 2.5 seconds - so longer than a  default timeout. serviceD specifies its timeout
     * in Seconds rather than milliseconds.
     */
    @Test
    public void testSecondsTimeout() {
        try {
            clientForTimeout.serviceD(2500);
            Assert.fail("serviceD should throw a TimeoutException in testSecondsTimeout");
        } 
        catch (TimeoutException ex) {
            // Expected
        } 
        catch (RuntimeException ex) {
            // Not Expected
            Assert.fail("serviceD should throw a TimeoutException in testSecondsTimeout not a RuntimeException");
        }
    }
    
    /**
     * A test that should not time out. The Fault Tolerance timeout is set to 2
     * seconds but serviceD will attempt to sleep for 1.5 seconds - so longer than a  default
     * timeout but shorter than the timeout that has been configured, before throwing a 
     * RuntimeException. There should be no Timeout.
     * 
     */
    @Test
    public void testSecondsNoTimeout() {
        try {
            clientForTimeout.serviceD(1500);
            Assert.fail("serviceD should throw a RuntimeException in testSecondsNoTimeout");
        } 
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceD should throw a RuntimeException in testSecondsNoTimeout not a TimeoutException");
        } 
        catch (RuntimeException ex) {
            // Expected
        }
    }
}
