/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.fallback.exception.hierarchy;

import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2;
import org.eclipse.microprofile.faulttolerance.Fallback;

import jakarta.enterprise.context.Dependent;

@Dependent
public class FallbackService {
    private String fallbackValue = "Fallback result";
    @Fallback(applyOn = {E0.class, E2.class}, skipOn = E1.class, fallbackMethod = "myFallback")
    public String serviceA(Throwable exception) throws Throwable {
        throw exception;
    }

    @Fallback(applyOn = {Exception.class, E1.class}, skipOn = {E0.class, E2.class}, fallbackMethod = "myFallback")
    public String serviceB(Throwable exception) throws Throwable {
        throw exception;
    }

    @Fallback(applyOn = {E1.class, E2.class}, skipOn = E0.class, fallbackMethod = "myFallback")
    public String serviceC(Throwable exception) throws Throwable {
        throw exception;
    }

    String myFallback(Throwable exception) throws Throwable {
        return fallbackValue;
    }

    public String getFallbackValue() {
        return fallbackValue;
    }

}
