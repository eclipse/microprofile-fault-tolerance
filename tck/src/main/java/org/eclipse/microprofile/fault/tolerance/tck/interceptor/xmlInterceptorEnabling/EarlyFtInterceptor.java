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
package org.eclipse.microprofile.fault.tolerance.tck.interceptor.xmlInterceptorEnabling;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.eclipse.microprofile.fault.tolerance.tck.interceptor.OrderQueueProducer;
import org.eclipse.microprofile.fault.tolerance.tck.interceptor.xmlInterceptorEnabling.EarlyFtInterceptor.InterceptEarly;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor that is called before the FT interceptor in the chain and count the invocation.
 * 
 * @author carlosdlr
 */
@Interceptor
@InterceptEarly
public class EarlyFtInterceptor {

    @Inject
    private OrderQueueProducer orderFactory;

    /**
     * Interceptor binding for {@link EarlyFtInterceptor}
     */
    @Retention(RUNTIME)
    @Target({TYPE, METHOD})
    @InterceptorBinding
    public @interface InterceptEarly {
    }

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        orderFactory.getOrderQueue().add("EarlyOrderFtInterceptor");
        return ctx.proceed();
    }
}
