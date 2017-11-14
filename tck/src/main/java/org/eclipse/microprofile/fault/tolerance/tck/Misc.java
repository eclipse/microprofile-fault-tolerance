/*
 *******************************************************************************
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
 *******************************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck;

/**
 * Simple utility class.
 */
public abstract class Misc {
    public abstract static class Ints {
        /**
         * Search for the existence of an int value in a given int array.
         * @param data the int data to search into
         * @param value the value to search
         * @return true if the data array contains at least once the expected value, false otherwise
         */
        public static boolean contains(int[] data, int value) {
            for (int i = 0; i < data.length; i++) {
                if (value == data[i]) {
                    return true;
                }
            }

            return false;
        }
        /*
         * prevent instanciation
         */
        private Ints() {
        }
    }
    /*
     * prevent instanciation
     */
    private Misc() {
    }
}
