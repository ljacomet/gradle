/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.model.internal.registry;

import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public class RuleContext {

    private static final ThreadLocal<Deque<ModelRuleDescriptor>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    @Nullable
    public static ModelRuleDescriptor get() {
        return STACK.get().peek();
    }

    public static void run(ModelRuleDescriptor descriptor, Runnable runnable) {
        STACK.get().push(descriptor);
        try {
            runnable.run();
        } finally {
            STACK.get().pop();
        }
    }
}
