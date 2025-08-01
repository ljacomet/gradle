/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.initialization;

import com.google.common.collect.ImmutableMap;
import org.gradle.initialization.properties.MutableGradleProperties;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class DefaultGradleProperties implements MutableGradleProperties {
    private final Map<String, String> defaultProperties;
    private final Map<String, String> overrideProperties;
    private ImmutableMap<String, String> gradleProperties;

    public DefaultGradleProperties(
        Map<String, String> defaultProperties,
        Map<String, String> overrideProperties
    ) {
        this.defaultProperties = defaultProperties;
        this.overrideProperties = overrideProperties;
        this.gradleProperties = mergePropertiesWith(ImmutableMap.of());
    }

    @Nullable
    @Override
    public String find(String propertyName) {
        return gradleProperties.get(propertyName);
    }

    @Override
    public Map<String, String> mergeProperties(Map<String, String> properties) {
        return properties.isEmpty()
            ? gradleProperties
            : mergePropertiesWith(properties);
    }

    @Override
    public void updateOverrideProperties(Map<String, String> properties) {
        overrideProperties.putAll(properties);
        gradleProperties = mergePropertiesWith(ImmutableMap.of());
    }

    @Override
    public Map<String, String> getProperties() {
        return gradleProperties;
    }

    private ImmutableMap<String, String> mergePropertiesWith(Map<String, String> properties) {
        return ImmutableMap.<String, String>builder()
            .putAll(defaultProperties)
            .putAll(properties)
            .putAll(overrideProperties)
            .buildKeepingLast();
    }
}
