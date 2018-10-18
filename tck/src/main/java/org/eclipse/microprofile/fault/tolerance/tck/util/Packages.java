/*
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
 */
package org.eclipse.microprofile.fault.tolerance.tck.util;

/**
 * Contains constants for utilities packages which need to be included by lots of tests.
 */
public class Packages {
    
    // Utility class only, no public constructors
    private Packages() {}
    
    /**
     * The {@code org.eclipse.microprofile.fault.tolerance.tck.util} package
     */
    public static final Package UTILS = org.eclipse.microprofile.fault.tolerance.tck.util.Packages.class.getPackage();
    
    /**
     * The {@code org.eclipse.microprofile.fault.tolerance.tck.metrics.util} package
     */
    public static final Package METRIC_UTILS = org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricComparator.class.getPackage();
}
