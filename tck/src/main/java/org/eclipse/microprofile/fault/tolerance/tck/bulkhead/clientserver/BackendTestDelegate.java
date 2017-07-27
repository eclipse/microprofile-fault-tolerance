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
 * Common interface for backend test delegates (not all methods need return
 * Futures)
 * 
 * @author Gordon Hutchison
 */
public interface BackendTestDelegate {
    /**
     * Called once per test from inside the Bulkhead
     * 
     * @return the 'Future' result
     */
    public Future perform();
}
