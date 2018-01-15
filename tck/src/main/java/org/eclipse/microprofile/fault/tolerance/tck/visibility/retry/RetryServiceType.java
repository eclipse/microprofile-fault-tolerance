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
package org.eclipse.microprofile.fault.tolerance.tck.visibility.retry;

public enum RetryServiceType {
    // Services with inheritance from a super class defining retry at the top class level
    // ROC => Retry On Class
    BASE_ROC,                                           // Retry defined at class level
    BASE_ROC_RETRY_REDEFINED_ON_CLASS,                  // derived class with Retry redefined at class level, no service method overridden
    BASE_ROC_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE,  // derived class with Retry redefined at class level, service method overridden
    BASE_ROC_RETRY_REDEFINED_ON_METHOD,                 // derived class with Retry redefined on overridden service method
    BASE_ROC_RETRY_MISSING_ON_METHOD,                   // derived class with no Retry annotation on overridden service method
    BASE_ROC_DERIVED_CLASS_NO_REDEFINITION,             // derived class, no annotation at class level, no service method overridden

    // Services with inheritance from a super class defining retry at the method level
    // ROM => Retry On Method
    BASE_ROM,                                           // class with Retry on service method
    BASE_ROM_RETRY_REDEFINED_ON_CLASS,                  // derived class with Retry redefined at class level, no service method overridden
    BASE_ROM_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE,  // derived class with Retry redefined at class level, service method overridden
    BASE_ROM_RETRY_REDEFINED_ON_METHOD,                 // derived class with Retry redefined on overridden service method
    BASE_ROM_RETRY_MISSING_ON_METHOD,                   // derived class with no Retry annotation on overridden service method
    BASE_ROM_DERIVED_CLASS_NO_REDEFINITION,             // derived class, no annotation at class level, no service method overridden

    // ROCM => Retry On Class and Method
    BASE_ROCM,                                          // class with Retry on class &and service method
    BASE_ROCM_RETRY_REDEFINED_ON_CLASS,                 // derived class with Retry redefined at class level, no service method overridden
    BASE_ROCM_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE, // derived class with Retry redefined at class level, service method overridden
    BASE_ROCM_RETRY_REDEFINED_ON_METHOD,                // derived class with Retry redefined on overridden service method
    BASE_ROCM_RETRY_MISSING_ON_METHOD,                  // derived class with no Retry annotation on overridden service method
    BASE_ROCM_DERIVED_CLASS_NO_REDEFINITION,            // derived class, no annotation at class level, no method override
    ;
}
