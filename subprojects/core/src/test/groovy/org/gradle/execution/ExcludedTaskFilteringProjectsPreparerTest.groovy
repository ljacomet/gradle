/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.execution


import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.specs.Spec
import org.gradle.execution.taskgraph.TaskExecutionGraphInternal
import spock.lang.Specification

class ExcludedTaskFilteringProjectsPreparerTest extends Specification {
    final StartParameterInternal startParameter = Mock()
    final TaskExecutionGraphInternal taskGraph = Mock()
    final DefaultTaskSelector selector = Mock()
    final GradleInternal gradle = Mock()
    final action = new ExcludedTaskFilteringProjectsPreparer(selector)

    def setup() {
        _ * gradle.startParameter >> startParameter
        _ * gradle.taskGraph >> taskGraph
    }

    def "does nothing when there are no excluded tasks defined"() {
        given:
        _ * startParameter.excludedTaskNames >> []

        when:
        action.prepareProjects(gradle)

        then:
        0 * taskGraph._
    }

    def "applies a filter for excluded tasks before proceeding"() {
        def filter = Stub(Spec)

        given:
        _ * startParameter.excludedTaskNames >> ['a']

        when:
        action.prepareProjects(gradle)

        then:
        1 * selector.getFilter('a') >> filter
        1 * taskGraph.useFilter(filter)
    }
}
