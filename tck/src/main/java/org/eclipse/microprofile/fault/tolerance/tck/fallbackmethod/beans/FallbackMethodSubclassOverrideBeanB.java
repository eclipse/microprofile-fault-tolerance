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

package org.eclipse.microprofile.fault.tolerance.tck.fallbackmethod.beans;

import org.eclipse.microprofile.faulttolerance.Fallback;

public class FallbackMethodSubclassOverrideBeanB {

    @Fallback(fallbackMethod = "fallback")
    public String method(int a, Long b) {
        throw new RuntimeException("test");
    }

    protected String fallback(int a, Long b) {
        // This fallback method should not be called as it is overridden in subclass
        return "Not this fallback";
    }

}
