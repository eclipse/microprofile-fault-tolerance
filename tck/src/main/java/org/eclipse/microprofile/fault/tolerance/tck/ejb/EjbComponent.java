/*
 *******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.ejb;

import org.eclipse.microprofile.fault.tolerance.tck.ejb.CounterFactory.CounterId;
import org.eclipse.microprofile.fault.tolerance.tck.ejb.EarlyFtInterceptor.InterceptEarly;
import org.eclipse.microprofile.fault.tolerance.tck.ejb.LateFtInterceptor.InterceptLate;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An EJB component to show the ordering between EJB, CDI
 * with FT annotations
 */
@Stateless
public class EjbComponent {

    @Inject
    @CounterId("serviceA")
    private AtomicInteger serviceACounter;

    @Inject
    @CounterId("connectionService")
    private AtomicInteger connectionServiceCounter;

    @InterceptEarly
    @InterceptLate
    @Retry(maxRetries = 5)
    public Connection serviceA() {
        serviceACounter.incrementAndGet();
        return connectionService();
    }

    public Connection connectionService() {
        connectionServiceCounter.incrementAndGet();
        throw new RuntimeException("Connection failed");
    }

}
