/*
 * Copyright 2014 the original author or authors.
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
package org.gradle.api.plugins.antlr

import org.gradle.test.fixtures.file.TestFile

class Antlr3PluginIntegrationTest extends AbstractAntlrIntegrationTest {

    String antlrDependency = "org.antlr:antlr:3.5.2"

    def "analyze good grammar"() {
        goodGrammar()
        goodProgram()
        expect:
        succeeds("generateGrammarSource")

        assertGrammarSourceGenerated("AnotherGrammar")
        assertGrammarSourceGenerated("org/acme/test/Test")
        assertAntlrVersion(3)

        succeeds("build")
    }

    private goodProgram() {
        file("grammar-user/src/main/java/com/example/Main.java") << """
            package com.example;
            import org.acme.test.TestLexer;
            import org.acme.test.TestParser;
            import org.antlr.runtime.CommonTokenStream;
            import org.antlr.runtime.RecognitionException;
            import org.antlr.runtime.ANTLRFileStream;
            import java.io.IOException;

            public class Main {
                public static void main(String[] args) throws IOException {
                    TestLexer lexer = new TestLexer(new ANTLRFileStream(args[0]));
                    CommonTokenStream tokens = new CommonTokenStream(lexer);
                    TestParser parser = new TestParser(tokens);
                    try {
                        parser.list();
                    } catch (RecognitionException e)  {
                        e.printStackTrace();
                    }
                }
            }
        """
    }

    private void assertGrammarSourceGenerated(String grammarName) {
        assertGrammarSourceGenerated(file('grammar-builder/build/generated-src/antlr/main'), grammarName)
    }

    private static void assertGrammarSourceGenerated(TestFile root, String grammarName) {
        def slashIndex = grammarName.lastIndexOf("/")
        assert root.file("${slashIndex == -1 ? grammarName : grammarName.substring(slashIndex)}.tokens").exists()
        assert root.file("${grammarName}Lexer.java").exists()
        assert root.file("${grammarName}Parser.java").exists()
    }

    def "analyze bad grammar"() {
        badGrammar()

        expect:
        fails("generateGrammarSource")
        failure.assertHasCause("There were 9 errors during grammar generation")
        assertAntlrVersion(3)
    }

    def "exception when package is set using #description"() {
        goodGrammar()

        buildFile "grammar-builder/build.gradle", """
            generateGrammarSource {
                ${expression}
            }
        """
        if (expectDeprecationWarning) {
            expectPackageArgumentDeprecationWarning(executer)
        }

        expect:
        fails("generateGrammarSource")
        failure.assertHasCause("The -package argument is not supported by ANTLR 3.")

        where:
        description                     | expression                                    | expectDeprecationWarning
        "arguments"                     | "arguments = ['-package', 'org.acme.test']"   | true
        "packageName property"          | "packageName = 'org.acme.test'"               | false
    }

    def "can change output directory and source set reflects change"() {
        goodGrammar()
        goodProgram()
        buildFile "grammar-builder/build.gradle", """
            generateGrammarSource {
                outputDirectory = file("build/generated/antlr/main")
            }
        """

        expect:
        succeeds("generateGrammarSource")

        assertGrammarSourceGenerated(file("grammar-builder/build/generated/antlr/main"), "AnotherGrammar")
        assertGrammarSourceGenerated(file("grammar-builder/build/generated/antlr/main"), "org/acme/test/Test")
        assertAntlrVersion(3)

        succeeds("build")
    }

    private goodGrammar() {
        file("grammar-builder/src/main/antlr/org/acme/test/Test.g") << """grammar Test;
            @header {
                package org.acme.test;
            }
            @lexer::header {
                package org.acme.test;
            }

            list    :   item (item)*
                    ;

            item    :
                ID
                | INT
                ;

            ID  :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
                ;

            INT :   '0'..'9'+
                ;
        """

        file("grammar-builder/src/main/antlr/AnotherGrammar.g") << """grammar AnotherGrammar;
            list    :   item (item)*
                    ;

            item    :
                ID
                | INT
                ;

            ID  :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
                ;

            INT :   '0'..'9'+
                ;
        """
    }

    private badGrammar() {
        file("grammar-builder/src/main/antlr/Test.g") << """grammar Test;
            list    :   item (item)*
                    ; some extra stuff

            item    :
                ID
                | INT
                ;

            ID  :   ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
                ;

            INT :   '0'..'9'+
                ;
        """
    }
}
