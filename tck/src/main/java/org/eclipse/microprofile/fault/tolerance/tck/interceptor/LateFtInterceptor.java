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
package org.eclipse.microprofile.fault.tolerance.tck.interceptor;


import org.eclipse.microprofile.fault.tolerance.tck.interceptor.CounterFactory.CounterId;
import org.eclipse.microprofile.fault.tolerance.tck.interceptor.LateFtInterceptor.InterceptLate;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An interceptor that is called after the FT interceptor
 * in the chain and count the invocation.
 * @author carlosdlr
 */
@Interceptor
@InterceptLate
@Priority(Interceptor.Priority.LIBRARY_AFTER)
public class LateFtInterceptor {

    @Inject
    @CounterId("LateFtInterceptor")
    private AtomicInteger counter;


    /**
     * Interceptor binding for {@link LateFtInterceptor}
     */
    @Retention(RUNTIME)
    @Target({ TYPE, METHOD })
    @InterceptorBinding
    public @interface InterceptLate {}


    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        counter.incrementAndGet();
        return ctx.proceed();
    }
}
