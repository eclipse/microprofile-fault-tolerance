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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.eclipse.microprofile.fault.tolerance.tck.extension;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Transfers TCK config to all deployed apps
 * <p>
 * This appender adds a jar which contains one resource containing the configured timeout multiplier. This resource will
 * be picked up by TCKConfig when run within a test application.
 */
public class TCKConfigArchiveAppender extends CachedAuxilliaryArchiveAppender {

    @Override
    protected Archive<?> buildArchive() {
        TCKConfig config = TCKConfig.getConfig();
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addAsResource(new StringAsset(Double.toString(config.getBaseMultiplier())), TCKConfig.RESOURCE_NAME)
                .addClass(TCKConfig.class);
        return archive;
    }

}
