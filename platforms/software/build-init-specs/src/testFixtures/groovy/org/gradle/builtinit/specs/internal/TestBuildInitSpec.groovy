/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.builtinit.specs.internal


import org.gradle.buildinit.specs.BuildInitParameter
import org.gradle.buildinit.specs.BuildInitSpec

/**
 * A sample {@link BuildInitSpec} implementation for testing purposes.
 */
class TestBuildInitSpec implements BuildInitSpec {
    private final String type
    private final String name

    TestBuildInitSpec(String type, String name = null) {
        this.type = type
        this.name = name
    }

    @Override
    String getDisplayName() {
        if (name) {
            return name
        } else {
            String spaced = getType().replace("-", " ");
            @SuppressWarnings("deprecation")
            String capitalized = org.apache.commons.lang3.text.WordUtils.capitalizeFully(spaced);
            return capitalized;
        }
    }

    @Override
    String getType() {
        return type
    }

    @Override
    List<BuildInitParameter<?>> getParameters() {
        return null
    }
}
