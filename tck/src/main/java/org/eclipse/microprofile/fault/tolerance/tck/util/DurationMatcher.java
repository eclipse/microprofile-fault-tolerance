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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import java.time.Duration;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class DurationMatcher extends TypeSafeDiagnosingMatcher<Duration> {

    private Duration target;
    private Duration margin;

    public DurationMatcher(Duration target, Duration margin) {
        super();
        this.target = target;
        this.margin = margin;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Duration within ")
                   .appendValue(margin)
                   .appendText(" of ")
                   .appendValue(target);
    }

    @Override
    protected boolean matchesSafely(Duration item, Description mismatchDescription) {
        Duration difference = item.minus(target).abs();
        mismatchDescription.appendValue(item)
                           .appendText(" which is ")
                           .appendValue(difference)
                           .appendText(" from ")
                           .appendValue(target);
        return difference.compareTo(margin) <= 0; // difference <= margin
    }
    
    /**
     * Matcher that asserts that a duration is within {@code margin} of {@code target}
     * 
     * @param target the target duration
     * @param margin the margin
     * @return the matcher
     */
    public static DurationMatcher closeTo(Duration target, Duration margin) {
        return new DurationMatcher(target, margin);
    }
    
    /**
     * Matcher that asserts that a duration is within 100ms of {@code target}
     * 
     * @param target the target duration
     * @return the matcher
     */
    public static DurationMatcher closeTo(Duration target) {
        return new DurationMatcher(target, Duration.ofMillis(100));
    }

}
