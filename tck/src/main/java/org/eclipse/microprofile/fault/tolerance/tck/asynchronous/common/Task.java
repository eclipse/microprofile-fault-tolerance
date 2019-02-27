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
package org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common;

import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;

/**
 * A common task common behavior to be used for asynchronous tests
 * @author <a href="mailto:kusanagi12002@gmail.com">Carlos Andres De La Rosa</a>
 *
 */
public interface Task  extends Connection {
    /**
     * execute the service task
     * @param taskResult the expected task result string
     */
    void doTask(String taskResult);

    /**
     * get the task result
     * @return  task result value
     */
    String getTaskResult();
}
