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
package org.eclipse.microprofile.fault.tolerance.tck;

import org.jboss.shrinkwrap.api.Archive;

/**
 * Simple producer to provide additional archive to be included in tck webapp tests.
 * Producers implementing this interface will be called via {@link java.util.ServiceLoader} during initialization
 * of Arquillian tests to be integrated into the @{@link org.jboss.arquillian.container.test.api.Deployment} under test.
 */
public interface TckArchiveProvider {
    Archive<?> additional();
}
