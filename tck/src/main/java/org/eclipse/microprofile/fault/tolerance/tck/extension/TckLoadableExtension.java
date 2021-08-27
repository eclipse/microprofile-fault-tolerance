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

package org.eclipse.microprofile.fault.tolerance.tck.extension;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Extension to register extensions required by the TCK
 */
public class TckLoadableExtension implements LoadableExtension {

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.arquillian.core.spi.LoadableExtension#register(org.jboss.arquillian.core.spi.LoadableExtension.
     * ExtensionBuilder)
     */
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(AuxiliaryArchiveAppender.class, HamcrestArchiveAppender.class);
        builder.service(AuxiliaryArchiveAppender.class, AwaitilityArchiveAppender.class);
        builder.service(AuxiliaryArchiveAppender.class, TCKConfigArchiveAppender.class);
    }

}
