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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

/**
 * A hierarchy of test exceptions
 * <p>
 * Exceptions in this package:
 * 
 * <pre>
 * The &lt;: symbol denotes the subtyping relation (Foo &lt;: Bar means "Foo is a subtype of Bar")
 * Note that subtyping is reflexive (Foo &lt;: Foo)
 * 
 * E0  &lt;: Exception
 * E1  &lt;: E0
 * E2  &lt;: E1
 * E2S &lt;: E2
 * E1S &lt;: E1, but E1S &lt;/: E2
 * E0S &lt;: E0, but E0S &lt;/: E1
 * </pre>
 */
package org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy;
