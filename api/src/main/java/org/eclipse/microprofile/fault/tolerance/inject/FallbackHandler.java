/*
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
 */
package org.eclipse.microprofile.fault.tolerance.inject;
/**
 * The handler interface to be implemenated by any fallback handling.
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author Ken Finnigan
 *
 */
public interface FallbackHandler<T>
{
    /**
     * Handle the previous calling failure and then call alternative methods
     * or perform any alternative operations.
     * @param context the execution context 
     */
    T handle(ExecutionContext context);

}
