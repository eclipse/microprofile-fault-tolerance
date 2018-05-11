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
package org.eclipse.microprofile.fault.tolerance.tck.util;

/**
 * An identifiable exception thrown by tests which test handling of exceptions thrown by user code.
 * <p>
 * It's basically a just runtime exception which isn't a {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException}.
 */
public class TestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TestException() {
        super("Test Exception");
    }
    
    public TestException(String message) {
        super("Test Exception - " + message);
    }
}
