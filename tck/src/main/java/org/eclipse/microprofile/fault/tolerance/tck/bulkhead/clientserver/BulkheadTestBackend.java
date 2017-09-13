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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

/**
 * This is a common backend for @Bulkhead tests. As it might be used
 * for @Asynchronous testing all the methods have to return a future. For common
 * code that has methods that don't return a Future delegate to the
 * BulkheadTestAction class
 * 
 * @author Gordon Hutchison
 */
public interface BulkheadTestBackend {

    /**
     * Perform the test
     * 
     * @param action test to execute
     * @return a Future compatible with @Asynchronous @Bulkhead tests.
     * @throws InterruptedException if interrupted
     */
    public Future test(BackendTestDelegate action) throws InterruptedException;

}
