/*
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
 */
package org.eclipse.microprofile.faulttolerance;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Wrap the execution and invoke it asynchronously.
 * Any methods marked with this annotation must return one of:
 * <ul>
 *   <li>{@link java.util.concurrent.Future}</li>
 *   <li>{@link java.util.concurrent.CompletionStage}</li>
 * </ul>
 * Otherwise, {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException} occurs
 * (at deploy time if the bean is discovered during deployment).
 * 
 * <p>
 * When a method marked with this annotation is executed, the call returns immediately while the method
 * is executed in another thread. The returned Future or CompletionStage is not completed until the result 
 * of the method running asynchronously is completed (either normally or exceptionally) or the method 
 * throws an exception.
 * </p>
 * 
 * <p>After a method marked with this annotation returns normally, the Future or CompletionStage returned
 * in the calling thread delegate all method calls to the object returned by the method. 
 * CompletionStage returned in the calling thread is completed when the stage returned by the method is completed
 * and with the same completion value or exception.</p>
 * 
 * <p>If a method marked with this annotation throws an exception when invoked, the exception isn't thrown 
 * in the callers thread therefore it's not possible to catch it with a {@code try..catch} block. 
 * The exception is propagated asynchronously:
 * </p>
 * <ul>
 *   <li>If the method declares {@link java.util.concurrent.Future} as the return type, 
 * calling {@link java.util.concurrent.Future#get()} will throw an 
 * {@link java.util.concurrent.ExecutionException} wrapping the original exception</li>
 *   <li>If the method declares {@link java.util.concurrent.CompletionStage} as the return type, 
 * it is completed exceptionally with the exception.</li>
 * </ul>
 * It's recommended that methods throw only runtime exceptions to avoid unnecessary {@code try..catch} blocks.
 * 
 * <p>If a class is annotated with this annotation, all class methods are treated as if they were marked
 * with this annotation. If one of the methods doesn't return either Future or CompletionStage,
 * {@link org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException}
 * occurs (at deploy time if the bean is discovered during deployment).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * <code>@Asynchronous
 * public CompletionStage&lt;String&gt; getString() {
 *  return CompletableFuture.completedFuture("hello");
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Example call with exception handling:
 * </p>
 *
 * <pre><code>
 * CompletionStage stage = getString().exceptionally(e -&gt; {
 *     handleException(e); 
 *     return null;
 * });
 * </code>
 * </pre>
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Inherited
public @interface Asynchronous {

}