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

package org.gradle.internal.declarativedsl.dom.operations.overlay

import org.gradle.declarative.dsl.model.annotations.Adding
import org.gradle.declarative.dsl.model.annotations.Configuring
import org.gradle.declarative.dsl.model.annotations.Restricted
import org.gradle.internal.declarativedsl.analysis.DefaultOperationGenerationId
import org.gradle.internal.declarativedsl.analysis.analyzeEverything
import org.gradle.internal.declarativedsl.dom.DeclarativeDocument
import org.gradle.internal.declarativedsl.dom.DocumentResolution
import org.gradle.internal.declarativedsl.dom.DomTestUtil
import org.gradle.internal.declarativedsl.dom.data.collectToMap
import org.gradle.internal.declarativedsl.dom.operations.overlay.DocumentOverlay.overlayResolvedDocuments
import org.gradle.internal.declarativedsl.dom.resolution.DocumentWithResolution
import org.gradle.internal.declarativedsl.dom.resolution.documentWithResolution
import org.gradle.internal.declarativedsl.fakeListAugmentationProvider
import org.gradle.internal.declarativedsl.parsing.ParseTestUtil
import org.gradle.internal.declarativedsl.schemaBuilder.schemaFromTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.Test


class DocumentOverlayTest {
    @Test
    fun `properties are combined in the result`() {
        val underlay = resolvedDocument(
            """
            x = 1
            y = 2
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            y = 3
            """.trimIndent()
        )

        overlayResolvedDocuments(underlay, overlay).document.assertMergeResult(
            """
            property(x, literal(1))
                literal(1)
            property(y, literal(3))
                literal(3)

            """.trimIndent(),
        )
    }

    @Test
    fun `underlay content of configuring block gets merged into the overlay one`() {
        val underlay = resolvedDocument(
            """
            configuring {
                a = 1
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuring {
                b = myInt()
            }
            """.trimIndent()
        )

        overlayResolvedDocuments(underlay, overlay).document.assertMergeResult(
            """
            element(configuring, [], content.size = 2)
                property(a, literal(1))
                    literal(1)
                property(b, valueFactory(myInt, []))
                    valueFactory(myInt, [])

            """.trimIndent(),
        )
    }


    @Test
    fun `underlay adding blocks appear before overlay ones`() {
        val underlay = resolvedDocument(
            """
            adding(1) {
                a = 1
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            adding(1) {
                b = 2
            }
            """.trimIndent()
        )

        overlayResolvedDocuments(underlay, overlay).document.assertMergeResult(
            """
            element(adding, [literal(1)], content.size = 1)
                literal(1)
                property(a, literal(1))
                    literal(1)
            element(adding, [literal(1)], content.size = 1)
                literal(1)
                property(b, literal(2))
                    literal(2)

            """.trimIndent(),
        )
    }


    @Test
    fun `configuring blocks get merged recursively`() {
        val underlay = resolvedDocument(
            """
            configuring {
                a = 1

                addingNested(1) {
                    a = 2
                }

                configuringNested {
                    a = 3

                    configuringNested {
                        a = 4
                    }
                }
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuring {
                b = 5

                addingNested(1) {
                    b = 6
                }

                configuringNested {
                    b = 7

                    configuringNested {
                        b = 8
                    }
                }
            }
            """.trimIndent()
        )

        overlayResolvedDocuments(underlay, overlay).document.assertMergeResult(
            """
            element(configuring, [], content.size = 5)
                property(a, literal(1))
                    literal(1)
                element(addingNested, [literal(1)], content.size = 1)
                    literal(1)
                    property(a, literal(2))
                        literal(2)
                property(b, literal(5))
                    literal(5)
                element(addingNested, [literal(1)], content.size = 1)
                    literal(1)
                    property(b, literal(6))
                        literal(6)
                element(configuringNested, [], content.size = 3)
                    property(a, literal(3))
                        literal(3)
                    property(b, literal(7))
                        literal(7)
                    element(configuringNested, [], content.size = 2)
                        property(a, literal(4))
                            literal(4)
                        property(b, literal(8))
                            literal(8)

            """.trimIndent(),
        )
    }

    @Test
    fun `configuring blocks with identity keys get merged based on the key`() {
        val underlay = resolvedDocument(
            """
            configuringById(2) {
                a = 2
            }
            configuringById(1) {
                a = 1
            }
            configuringById(3) {
                a = 3
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuringById(1) {
                b = 1
            }
            configuringById(2) {
                b = 2
            }
            configuringById(4) {
                b = 4
            }
            """.trimIndent()
        )

        // TODO: this test asserts the order of the result element that follows from the rule:
        //       "the elements missing in the overlay go first, then the merged ones".
        //       However, a more natural result order might be with keys ordered as: 2, 1, 3, 4
        overlayResolvedDocuments(underlay, overlay).document.assertMergeResult(
            """
            element(configuringById, [literal(3)], content.size = 1)
                literal(3)
                property(a, literal(3))
                    literal(3)
            element(configuringById, [literal(1)], content.size = 2)
                literal(1)
                property(a, literal(1))
                    literal(1)
                property(b, literal(1))
                    literal(1)
            element(configuringById, [literal(2)], content.size = 2)
                literal(2)
                property(a, literal(2))
                    literal(2)
                property(b, literal(2))
                    literal(2)
            element(configuringById, [literal(4)], content.size = 1)
                literal(4)
                property(b, literal(4))
                    literal(4)

            """.trimIndent(),
        )
    }


    @Test
    fun `the overlay shows where the elements come from`() {
        val underlay = resolvedDocument(
            """
            x = 1
            adding(1) {
                a = 2
                unresolved1()
                errorExample(namedArgs = "are not supported")
            }
            configuring {
                a = 3
                unresolved2()
                !syntax!error!example!
            }
            unresolved3()
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            y = 4
            adding(1) {
                b = 5
                unresolved4()
            }
            configuring {
                a = 33
                b = 6
                unresolved5()
                errorExample = unsupported.propertyAccess
            }
            unresolved6()
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        val overlayOriginDump = dumpDocumentWithOverlayData(result)

        result.overlayNodeOriginContainer.collectToMap(result.document).entries.joinToString("\n") { "${it.key} -> ${it.value}" }

        assertEquals(
            """
            * property(x, literal(1)) -> FromUnderlay(documentNode=property(x, literal(1)))
                - literal(1) -> FromUnderlay(documentNode=property(x, literal(1)))
            * element(adding, [literal(1)], content.size = 3) -> FromUnderlay(documentNode=element(adding, [literal(1)], content.size = 3))
                - literal(1) -> FromUnderlay(documentNode=element(adding, [literal(1)], content.size = 3))
                * property(a, literal(2)) -> FromUnderlay(documentNode=property(a, literal(2)))
                    - literal(2) -> FromUnderlay(documentNode=property(a, literal(2)))
                * element(unresolved1, [], content.size = 0) -> FromUnderlay(documentNode=element(unresolved1, [], content.size = 0))
                * error(UnsupportedSyntax(cause=ElementArgumentFormat)) -> FromUnderlay(documentNode=error(UnsupportedSyntax(cause=ElementArgumentFormat)))
            * element(unresolved3, [], content.size = 0) -> FromUnderlay(documentNode=element(unresolved3, [], content.size = 0))
            * property(y, literal(4)) -> FromOverlay(documentNode=property(y, literal(4)))
                - literal(4) -> FromOverlay(documentNode=property(y, literal(4)))
            * element(adding, [literal(1)], content.size = 2) -> FromOverlay(documentNode=element(adding, [literal(1)], content.size = 2))
                - literal(1) -> FromOverlay(documentNode=element(adding, [literal(1)], content.size = 2))
                * property(b, literal(5)) -> FromOverlay(documentNode=property(b, literal(5)))
                    - literal(5) -> FromOverlay(documentNode=property(b, literal(5)))
                * element(unresolved4, [], content.size = 0) -> FromOverlay(documentNode=element(unresolved4, [], content.size = 0))
            * element(configuring, [], content.size = 7) -> MergedElements(underlayElement=element(configuring, [], content.size = 4), overlayElement=element(configuring, [], content.size = 4))
                * element(unresolved2, [], content.size = 0) -> FromUnderlay(documentNode=element(unresolved2, [], content.size = 0))
                * error(SyntaxError(parsingError=ParsingError(potentialElementSource=LightTreeSourceData(test:164..170), erroneousSource=LightTreeSourceData(test:164..170), message=Unsupported operation in unary expression: !))) -> FromUnderlay(documentNode=error(SyntaxError(parsingError=ParsingError(potentialElementSource=LightTreeSourceData(test:164..170), erroneousSource=LightTreeSourceData(test:164..170), message=Unsupported operation in unary expression: !))))
                * error(SyntaxError(parsingError=ParsingError(potentialElementSource=LightTreeSourceData(test:171..185), erroneousSource=LightTreeSourceData(test:171..185), message=Unexpected tokens (use ';' to separate expressions on the same line)))) -> FromUnderlay(documentNode=error(SyntaxError(parsingError=ParsingError(potentialElementSource=LightTreeSourceData(test:171..185), erroneousSource=LightTreeSourceData(test:171..185), message=Unexpected tokens (use ';' to separate expressions on the same line)))))
                * property(a, literal(33)) -> MergedProperties(shadowedPropertiesFromUnderlay=[property(a, literal(3))], effectivePropertiesFromUnderlay=[], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(a, literal(33))])
                    - literal(33) -> FromOverlay(documentNode=property(a, literal(33)))
                * property(b, literal(6)) -> FromOverlay(documentNode=property(b, literal(6)))
                    - literal(6) -> FromOverlay(documentNode=property(b, literal(6)))
                * element(unresolved5, [], content.size = 0) -> FromOverlay(documentNode=element(unresolved5, [], content.size = 0))
                * error(UnsupportedSyntax(cause=NamedReferenceWithExplicitReceiver)) -> FromOverlay(documentNode=error(UnsupportedSyntax(cause=NamedReferenceWithExplicitReceiver)))
            * element(unresolved6, [], content.size = 0) -> FromOverlay(documentNode=element(unresolved6, [], content.size = 0))

            """.trimIndent(),
            overlayOriginDump
        )
    }

    @Test
    fun `the overlay has nodes for the same property merged and put in place of the last one`() {
        val underlay = resolvedDocument(
            """
            configuringById(123) {
                b = 1 // this is shadowed by b = 2 below
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            y = 1 // this is shadowed by y = 3
            y = 2 // this is shadowed by y = 3
            configuringById(123) {
                b = 2
            }
            adding(1234) {
                b = 1 // as the `adding` block is not merged but copied from overlay, this is not shadowed because the block content is kept intact
                b = 2
            }
            y = 3
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        assertEquals(
            """
            * element(configuringById, [literal(123)], content.size = 1) -> MergedElements(underlayElement=element(configuringById, [literal(123)], content.size = 1), overlayElement=element(configuringById, [literal(123)], content.size = 1))
                - literal(123) -> FromOverlay(documentNode=element(configuringById, [literal(123)], content.size = 1))
                * property(b, literal(2)) -> MergedProperties(shadowedPropertiesFromUnderlay=[property(b, literal(1))], effectivePropertiesFromUnderlay=[], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(b, literal(2))])
                    - literal(2) -> FromOverlay(documentNode=property(b, literal(2)))
            * element(adding, [literal(1234)], content.size = 2) -> FromOverlay(documentNode=element(adding, [literal(1234)], content.size = 2))
                - literal(1234) -> FromOverlay(documentNode=element(adding, [literal(1234)], content.size = 2))
                * property(b, literal(1)) -> FromOverlay(documentNode=property(b, literal(1)))
                    - literal(1) -> FromOverlay(documentNode=property(b, literal(1)))
                * property(b, literal(2)) -> FromOverlay(documentNode=property(b, literal(2)))
                    - literal(2) -> FromOverlay(documentNode=property(b, literal(2)))
            * property(y, literal(3)) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[], shadowedPropertiesFromOverlay=[property(y, literal(1)), property(y, literal(2))], effectivePropertiesFromOverlay=[property(y, literal(3))])
                - literal(3) -> FromOverlay(documentNode=property(y, literal(3)))

            """.trimIndent(), dumpDocumentWithOverlayData(result)
        )
    }

    @Test
    fun `augmented assignments are kept as effective in the results`() {
        val underlay = resolvedDocument(
            """
            configuringById(123) {
                s = myStringList("one")
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuringById(123) {
                s += myStringList("two", "three")
            }
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        assertEquals(
            """
            * element(configuringById, [literal(123)], content.size = 2) -> MergedElements(underlayElement=element(configuringById, [literal(123)], content.size = 1), overlayElement=element(configuringById, [literal(123)], content.size = 1))
                - literal(123) -> FromOverlay(documentNode=element(configuringById, [literal(123)], content.size = 1))
                * property(s, valueFactory(myStringList, [literal(one)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, valueFactory(myStringList, [literal(one)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(two), literal(three)]))])
                    - valueFactory(myStringList, [literal(one)]) -> FromUnderlay(documentNode=property(s, valueFactory(myStringList, [literal(one)])))
                * property(s, += valueFactory(myStringList, [literal(two), literal(three)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, valueFactory(myStringList, [literal(one)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(two), literal(three)]))])
                    - valueFactory(myStringList, [literal(two), literal(three)]) -> FromOverlay(documentNode=property(s, += valueFactory(myStringList, [literal(two), literal(three)])))

            """.trimIndent(), dumpDocumentWithOverlayData(result)
        )
    }

    @Test
    fun `augmentation from underlay is shadowed by a reassignment in the overlay`() {
        val underlay = resolvedDocument(
            """
            configuringById(123) {
                s += myStringList("one")
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuringById(123) {
                s = myStringList("two", "three")
            }
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        assertEquals(
            """
            * element(configuringById, [literal(123)], content.size = 1) -> MergedElements(underlayElement=element(configuringById, [literal(123)], content.size = 1), overlayElement=element(configuringById, [literal(123)], content.size = 1))
                - literal(123) -> FromOverlay(documentNode=element(configuringById, [literal(123)], content.size = 1))
                * property(s, valueFactory(myStringList, [literal(two), literal(three)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[property(s, += valueFactory(myStringList, [literal(one)]))], effectivePropertiesFromUnderlay=[], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, valueFactory(myStringList, [literal(two), literal(three)]))])
                    - valueFactory(myStringList, [literal(two), literal(three)]) -> FromOverlay(documentNode=property(s, valueFactory(myStringList, [literal(two), literal(three)])))

            """.trimIndent(), dumpDocumentWithOverlayData(result)
        )
    }

    @Test
    fun `a series of overlays keeps augmentation`() {
        val docs = listOf(
            resolvedDocument("""configuring { s += myStringList("one")"""),
            resolvedDocument("""configuring { s += myStringList("two")"""),
            resolvedDocument("""configuring { s += myStringList("three")"""),
            resolvedDocument("""configuring { s += myStringList("four")"""),
        )

        val result = docs.fold(overlayResolvedDocuments(resolvedDocument(""), resolvedDocument(""))) { acc, it ->
            overlayResolvedDocuments(acc.result, it)
        }

        assertEquals(
            """
            * element(configuring, [], content.size = 4) -> MergedElements(underlayElement=element(configuring, [], content.size = 3), overlayElement=element(configuring, [], content.size = 1))
                * property(s, += valueFactory(myStringList, [literal(one)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, += valueFactory(myStringList, [literal(one)])), property(s, += valueFactory(myStringList, [literal(two)])), property(s, += valueFactory(myStringList, [literal(three)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(four)]))])
                    - valueFactory(myStringList, [literal(one)]) -> FromUnderlay(documentNode=property(s, += valueFactory(myStringList, [literal(one)])))
                * property(s, += valueFactory(myStringList, [literal(two)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, += valueFactory(myStringList, [literal(one)])), property(s, += valueFactory(myStringList, [literal(two)])), property(s, += valueFactory(myStringList, [literal(three)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(four)]))])
                    - valueFactory(myStringList, [literal(two)]) -> FromUnderlay(documentNode=property(s, += valueFactory(myStringList, [literal(two)])))
                * property(s, += valueFactory(myStringList, [literal(three)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, += valueFactory(myStringList, [literal(one)])), property(s, += valueFactory(myStringList, [literal(two)])), property(s, += valueFactory(myStringList, [literal(three)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(four)]))])
                    - valueFactory(myStringList, [literal(three)]) -> FromUnderlay(documentNode=property(s, += valueFactory(myStringList, [literal(three)])))
                * property(s, += valueFactory(myStringList, [literal(four)])) -> MergedProperties(shadowedPropertiesFromUnderlay=[], effectivePropertiesFromUnderlay=[property(s, += valueFactory(myStringList, [literal(one)])), property(s, += valueFactory(myStringList, [literal(two)])), property(s, += valueFactory(myStringList, [literal(three)]))], shadowedPropertiesFromOverlay=[], effectivePropertiesFromOverlay=[property(s, += valueFactory(myStringList, [literal(four)]))])
                    - valueFactory(myStringList, [literal(four)]) -> FromOverlay(documentNode=property(s, += valueFactory(myStringList, [literal(four)])))

            """.trimIndent(),
            dumpDocumentWithOverlayData(result)
        )
    }

    @Test
    fun `multiple property nodes from underlay are kept as-is if the overlay has nothing to merge with them`() {
        val underlay = resolvedDocument(
            """
            configuringById(1) {
                s = myStringList("one")
                s += myStringList("two")
            }
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            configuringById(1) {
                b = 1
            }
            configuringById(2) {
                s = myStringList("unrelated")
            }
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        assertEquals(
            """
            * element(configuringById, [literal(1)], content.size = 3) -> MergedElements(underlayElement=element(configuringById, [literal(1)], content.size = 2), overlayElement=element(configuringById, [literal(1)], content.size = 1))
                - literal(1) -> FromOverlay(documentNode=element(configuringById, [literal(1)], content.size = 1))
                * property(s, valueFactory(myStringList, [literal(one)])) -> FromUnderlay(documentNode=property(s, valueFactory(myStringList, [literal(one)])))
                    - valueFactory(myStringList, [literal(one)]) -> FromUnderlay(documentNode=property(s, valueFactory(myStringList, [literal(one)])))
                * property(s, += valueFactory(myStringList, [literal(two)])) -> FromUnderlay(documentNode=property(s, += valueFactory(myStringList, [literal(two)])))
                    - valueFactory(myStringList, [literal(two)]) -> FromUnderlay(documentNode=property(s, += valueFactory(myStringList, [literal(two)])))
                * property(b, literal(1)) -> FromOverlay(documentNode=property(b, literal(1)))
                    - literal(1) -> FromOverlay(documentNode=property(b, literal(1)))
            * element(configuringById, [literal(2)], content.size = 1) -> FromOverlay(documentNode=element(configuringById, [literal(2)], content.size = 1))
                - literal(2) -> FromOverlay(documentNode=element(configuringById, [literal(2)], content.size = 1))
                * property(s, valueFactory(myStringList, [literal(unrelated)])) -> FromOverlay(documentNode=property(s, valueFactory(myStringList, [literal(unrelated)])))
                    - valueFactory(myStringList, [literal(unrelated)]) -> FromOverlay(documentNode=property(s, valueFactory(myStringList, [literal(unrelated)])))

        """.trimIndent(), dumpDocumentWithOverlayData(result)
        )
    }

    @Test
    fun `the overlay result can be resolved with the merged container`() {
        val underlay = resolvedDocument(
            """
            x = 1
            adding(1) {
                a = 2
                unresolved1()
            }
            configuring {
                a = 3
                unresolved2()
                s = myStringList("one")
            }
            unresolved3()
            """.trimIndent()
        )

        val overlay = resolvedDocument(
            """
            y = 4
            adding(1) {
                b = myInt()
                unresolved4()
            }
            configuring {
                b = myInt()
                unresolved5()
                s += myStringList("two", "three")
            }
            unresolved6()
            """.trimIndent()
        )

        val result = overlayResolvedDocuments(underlay, overlay)

        val resolutionDump = dumpDocumentWithResolution(DocumentWithResolution(result.document, result.overlayResolutionContainer))

        assertEquals(
            """
            * property(x, literal(1)) -> property(Int)
                - literal(1) -> literal
            * element(adding, [literal(1)], content.size = 2) -> element(NestedReceiver)
                - literal(1) -> literal
                * property(a, literal(2)) -> property(Int)
                    - literal(2) -> literal
                * element(unresolved1, [], content.size = 0) -> notResolved(UnresolvedSignature)
            * element(unresolved3, [], content.size = 0) -> notResolved(UnresolvedSignature)
            * property(y, literal(4)) -> property(Int)
                - literal(4) -> literal
            * element(adding, [literal(1)], content.size = 2) -> element(NestedReceiver)
                - literal(1) -> literal
                * property(b, valueFactory(myInt, [])) -> property(Int)
                    - valueFactory(myInt, []) -> valueFactory
                * element(unresolved4, [], content.size = 0) -> notResolved(UnresolvedSignature)
            * element(configuring, [], content.size = 6) -> configuring(NestedReceiver)
                * property(a, literal(3)) -> property(Int)
                    - literal(3) -> literal
                * element(unresolved2, [], content.size = 0) -> notResolved(UnresolvedSignature)
                * property(b, valueFactory(myInt, [])) -> property(Int)
                    - valueFactory(myInt, []) -> valueFactory
                * element(unresolved5, [], content.size = 0) -> notResolved(UnresolvedSignature)
                * property(s, valueFactory(myStringList, [literal(one)])) -> property(List<String>)
                    - valueFactory(myStringList, [literal(one)]) -> valueFactory
                * property(s, += valueFactory(myStringList, [literal(two), literal(three)])) -> property(List<String>)
                    - valueFactory(myStringList, [literal(two), literal(three)]) -> valueFactory
            * element(unresolved6, [], content.size = 0) -> notResolved(UnresolvedSignature)

            """.trimIndent(),
            resolutionDump
        )
    }

    @Test
    fun `can use the merge result as an input`() {
        val docs = listOf(
            resolvedDocument("x = 1"),
            resolvedDocument("y = 2"),
            resolvedDocument("configuring { a = 3 }"),
            resolvedDocument("configuring { b = 4 }")
        )

        val result = docs.reduce { acc, it ->
            val overlayResult = overlayResolvedDocuments(acc, it)
            DocumentWithResolution(overlayResult.document, overlayResult.overlayResolutionContainer)
        }

        assertEquals(
            """
            * property(x, literal(1)) -> property(Int)
                - literal(1) -> literal
            * property(y, literal(2)) -> property(Int)
                - literal(2) -> literal
            * element(configuring, [], content.size = 2) -> configuring(NestedReceiver)
                * property(a, literal(3)) -> property(Int)
                    - literal(3) -> literal
                * property(b, literal(4)) -> property(Int)
                    - literal(4) -> literal

            """.trimIndent(),
            dumpDocumentWithResolution(result)
        )
    }

    private
    fun dumpDocumentWithResolution(documentWithResolution: DocumentWithResolution) =
        DomTestUtil.printDomByTraversal(
            documentWithResolution.document,
            { "* $it -> ${prettyPrintResolution(documentWithResolution.resolutionContainer.data(it))}" },
            { "- $it -> ${prettyPrintResolution(documentWithResolution.resolutionContainer.data(it))}" },
        )

    private fun dumpDocumentWithOverlayData(result: DocumentOverlayResult): String = DomTestUtil.printDomByTraversal(
        result.document,
        { "* $it -> ${result.overlayNodeOriginContainer.data(it)}" },
        { "- $it -> ${result.overlayNodeOriginContainer.data(it)}" },
    )

    private
    val schema = schemaFromTypes(TopLevelReceiver::class, listOf(TopLevelReceiver::class, NestedReceiver::class), augmentationsProvider = fakeListAugmentationProvider())

    private
    fun DeclarativeDocument.assertMergeResult(expectedDomContent: String) {
        assertEquals(expectedDomContent, DomTestUtil.printDomByTraversal(this, Any::toString, Any::toString))
    }

    private
    fun resolvedDocument(code: String) =
        documentWithResolution(schema, ParseTestUtil.parse(code), DefaultOperationGenerationId.finalEvaluation, analyzeEverything)

    interface TopLevelReceiver {
        @get:Restricted
        var x: Int

        @get: Restricted
        var y: Int

        @Configuring
        fun configuring(configure: NestedReceiver.() -> Unit)

        @Configuring
        fun configuringById(id: Int, configure: NestedReceiver.() -> Unit)

        @Adding
        fun adding(someValue: Int, configure: NestedReceiver.() -> Unit): NestedReceiver

        @Restricted
        fun myInt(): Int

        @Restricted
        fun myStringList(vararg strings: String): List<String>
    }

    interface NestedReceiver {
        @get:Restricted
        var a: Int

        @get:Restricted
        var b: Int

        @get:Restricted
        var s: List<String>

        @Configuring
        fun configuringNested(nestedReceiver: NestedReceiver.() -> Unit)

        @Adding
        fun addingNested(someValue: Int, nestedReceiver: NestedReceiver.() -> Unit): NestedReceiver
    }

    private
    fun prettyPrintResolution(documentResolution: DocumentResolution): String = when (documentResolution) {
        is DocumentResolution.ElementResolution.ElementNotResolved -> "notResolved(${documentResolution.reasons.joinToString()})"
        DocumentResolution.ErrorResolution -> "errorResolution"
        is DocumentResolution.ElementResolution.SuccessfulElementResolution.ConfiguringElementResolved -> "configuring(${documentResolution.elementType})"
        is DocumentResolution.ElementResolution.SuccessfulElementResolution.ContainerElementResolved -> "element(${documentResolution.elementType})"
        is DocumentResolution.PropertyResolution.PropertyAssignmentResolved -> "property(${documentResolution.property.valueType})"
        is DocumentResolution.PropertyResolution.PropertyNotAssigned -> "notAssigned(${documentResolution.reasons.joinToString()})"
        is DocumentResolution.ValueNodeResolution.LiteralValueResolved -> "literal"
        is DocumentResolution.ValueNodeResolution.ValueFactoryResolution.ValueFactoryResolved -> "valueFactory"
        is DocumentResolution.ValueNodeResolution.ValueFactoryResolution.ValueFactoryNotResolved -> "valueFactoryNotResolved"
        is DocumentResolution.ValueNodeResolution.NamedReferenceResolution.NamedReferenceResolved -> "namedReference"
        is DocumentResolution.ValueNodeResolution.NamedReferenceResolution.NamedReferenceNotResolved -> "namedReferenceNotResolved"
    }
}
