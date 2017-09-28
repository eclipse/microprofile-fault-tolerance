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

import java.util.ServiceLoader;

import org.jboss.shrinkwrap.api.spec.WebArchive;

public class TckAdditions {
    private TckAdditions() {
        // ignore
    }

    /**
     * Loads via {@link ServiceLoader} additional archive producers. Delegate to each of them the addition of an archive
     * to the @{@link org.jboss.arquillian.container.test.api.Deployment} under test.
     * @param web the Arquillian {@link WebArchive} to enhance with additional libraries
     * @return the modified {@link WebArchive}
     */
    public static WebArchive decorate(WebArchive web) {
        ServiceLoader<TckArchiveProvider> providers = ServiceLoader.load(TckArchiveProvider.class);
        providers.forEach(p -> web.addAsLibrary(p.additional()));
        return web;
    }
}
