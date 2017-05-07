/*
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
 */
package org.eclipse.microprofile.fault.tolerance.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * The Retry annotation to define the number of the retries and the fallback method on reaching the
 * retry counts.
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 * @author John Ament
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Retry {

    /**
     * @return The max number of retries. -1 means retry forever. If less than -1, an IllegalArgumentException will be thrown.
     *
     */
    int maxRetries() default 3;

    /**
     * The delay between retries. Defaults to 0.
     * @return the delay time
     */
    int delay() default 0;

    /**
     *
     * @return the delay unit
     */

    TimeUnit delayUnit() default TimeUnit.MILLISECONDS;

    /**
     * @return the maximum duration to perform retries for.
     */
    int maxDuration() default 20;

    /**
     *
     * @return the duration unit
     */
    TimeUnit durationUnit() default TimeUnit.MILLISECONDS;

    /**
     *
     * @return the jitter that randomly vary retry delays by. e.g. a jitter of 200 milliseconds
     * will randomly add betweem -200 and 200 milliseconds to each retry delay.
     */
    int jitter() default 200;

    /**
     *
     * @return the jitter delay unit.
     */
    TimeUnit jitterDelayUnit() default TimeUnit.MILLISECONDS;

    /**
     * For each retry delay, a randomly portion of the delay multiplied by the jitterFactor will be added or subtracted to the delay.
     * e.g. a retry delay of 200 milliseconds and a jitter of 0.25 will result in a random retry delay between 150 and 250 milliseconds.
     * @return the jitter factor.
     */

    double jitterFactor() default 0.25;

    /**
     *
     * @return Specify the failure to retry on
     */
    Class<? extends Throwable>[] retryOn() default { Exception.class };

    /**
     *
     * @return Specify the failure to abort on
     */
    Class<? extends Throwable>[] abortOn() default { Throwable.class };

    /**
     *
     * @return The fallback method name
     */
    String fallBack();
}
