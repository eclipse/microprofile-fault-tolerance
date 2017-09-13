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
package org.eclipse.microprofile.fault.tolerance.tck.illegalConfig;


import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * A client to demonstrate the fallback after doing retries
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
public class FallbackMethodWithArgsClient {

    /**
     * Retry 5 times and then fallback
     * @param name a string
     * @param type an Integer
     * @return a dummy number
     */
    @Retry(maxRetries = 4)
    @Fallback(fallbackMethod = "fallbackForServiceB")
    public Integer serviceB(String name, Integer type) {
        return 42;
    }
    
    /**
     * Fallback method with incompatible signature, only one parameter
     * @param name a string
     * @return a dummy number
     */
    public Integer fallbackForServiceB(String name) {
        return 42;
    }
}
