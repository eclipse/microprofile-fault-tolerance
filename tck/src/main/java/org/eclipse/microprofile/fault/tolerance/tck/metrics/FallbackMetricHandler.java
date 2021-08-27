/*
 *******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.FallbackMetricBean.Action;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.FallbackMetricBean.NonFallbackException;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class FallbackMetricHandler implements FallbackHandler<Void> {

    @Inject
    private FallbackMetricBean fallbackBean;

    @Override
    public Void handle(ExecutionContext context) {
        if (fallbackBean.getFallbackAction() == Action.PASS) {
            return null;
        } else if (fallbackBean.getFallbackAction() == Action.FAIL) {
            throw new TestException();
        } else {
            throw new NonFallbackException();
        }
    }

}
