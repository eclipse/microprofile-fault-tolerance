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

import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Adds a jar containing the hamcrest matcher packages
 */
public class HamcrestArchiveAppender extends CachedAuxilliaryArchiveAppender {

    /* (non-Javadoc)
     * @see org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender#buildArchive()
     */
    @Override
    protected Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class, "hamcrest.jar")
                         .addPackages(true, "org.hamcrest");
    }

}
