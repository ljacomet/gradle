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

package org.gradle.api.problems.fixtures

import org.gradle.integtests.fixtures.GroovyBuildScriptLanguage

class ReportingScript {

    static String getProblemReportingScript(@GroovyBuildScriptLanguage String taskActionMethodBody) {
        return """
            import org.gradle.api.problems.Severity

            abstract class ProblemReportingTask extends DefaultTask {

                private final Problems problems;

                @Inject
                public ProblemReportingTask(Problems problems) {
                    this.problems = problems;
                }

                @Internal
                protected Problems getProblems() {
                    return problems;
                }

                @TaskAction
                void run() {
                    $taskActionMethodBody
                }
            }

            tasks.register("reportProblem", ProblemReportingTask)
        """
    }
}
