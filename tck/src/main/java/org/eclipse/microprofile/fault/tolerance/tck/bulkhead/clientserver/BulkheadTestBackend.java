/**********************************************************************
* Copyright (c) 2017 Contributors to the Eclipse Foundation 
*
* See the NOTICES file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* All rights reserved. This program and the accompanying materials 
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/
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
     * @param action
     * @return a Future compatible with @Asynchronous @Bulkhead tests.
     */
    public Future test(BackendTestDelegate action);

}
