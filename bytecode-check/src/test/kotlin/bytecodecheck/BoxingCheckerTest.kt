// SPDX-License-Identifier: CC0-1.0
package bytecodecheck

import bytecodecheck.fixtures.Annotated
import bytecodecheck.fixtures.Clean
import bytecodecheck.fixtures.HasBoxedField
import bytecodecheck.fixtures.MakesPair
import bytecodecheck.fixtures.NonInlineTakesLambda
import bytecodecheck.fixtures.ReturnsNullable
import bytecodecheck.fixtures.StoresLambda
import bytecodecheck.fixtures.TakesBoxed
import bytecodecheck.fixtures.UsesList
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxingCheckerTest {
    @Test
    fun exclusionsLeaveEveryOtherClassInScope() {
        val excluded = listOf("kotlin/", "META-INF/")
        assertTrue(isChecked("MainKt.class", emptyList(), excluded))
        assertTrue(isChecked("FloatList.class", emptyList(), excluded))
        assertTrue(isChecked("ObjIntHashMap\$Cursor.class", emptyList(), excluded))
        assertFalse(isChecked("kotlin/Pair.class", emptyList(), excluded))
        assertFalse(isChecked("META-INF/versions/9/module-info.class", emptyList(), excluded))
    }

    @Test
    fun prefixesNarrowTheScopeAndExclusionsWin() {
        assertTrue(isChecked("IntList.class", listOf("Int"), emptyList()))
        assertFalse(isChecked("FloatList.class", listOf("Int"), emptyList()))
        assertFalse(isChecked("IntList.class", listOf("Int"), listOf("IntL")))
        assertTrue(isChecked("IntList.class", emptyList(), emptyList()))
    }

    private fun bytes(type: Class<*>): ByteArray =
        type.getResourceAsStream("/" + type.name.replace('.', '/') + ".class")!!.readBytes()

    private fun library(type: KClass<*>) = BoxingChecker.check(bytes(type.java), Mode.LIBRARY)
    private fun strict(type: KClass<*>) = BoxingChecker.check(bytes(type.java), Mode.STRICT)

    private fun assertFlags(rule: String, findings: List<Finding>) =
        assertTrue(findings.any { rule in it.rule }, "Expected '$rule' in $findings")

    @Test
    fun cleanClassPassesAsLibraryThanksToTheInlineExemption() {
        assertEquals(emptyList(), library(Clean::class))
    }

    @Test
    fun inlineBodyIsNotExemptInStrictMode() {
        assertFlags("Function1", strict(Clean::class))
    }

    @Test
    fun inlinedUserCodeIsClean() {
        val facade = Class.forName("bytecodecheck.fixtures.FixturesKt")
        assertEquals(emptyList(), BoxingChecker.check(bytes(facade), Mode.STRICT))
    }

    @Test
    fun flagsNullablePrimitiveReturn() {
        assertFlags("public signature uses a boxed type", library(ReturnsNullable::class))
        assertFlags("java/lang/Integer.valueOf", library(ReturnsNullable::class))
    }

    @Test
    fun flagsNullablePrimitiveParameter() {
        assertFlags("public signature uses a boxed type", library(TakesBoxed::class))
    }

    @Test
    fun flagsBoxedField() {
        assertFlags("field of boxed type", library(HasBoxedField::class))
    }

    @Test
    fun flagsBoxingInPrivateMethods() {
        assertFlags("valueOf", library(UsesList::class))
        assertFlags("Number.intValue", library(UsesList::class))
    }

    @Test
    fun flagsStoredLambda() {
        assertFlags("Function1", library(StoresLambda::class))
    }

    @Test
    fun lambdaParameterOfANonInlineFunctionIsNotExempt() {
        assertFlags("Function1", library(NonInlineTakesLambda::class))
    }

    @Test
    fun flagsPair() {
        assertFlags("kotlin/Pair", library(MakesPair::class))
    }

    @Test
    fun boxingApiIsExempt() {
        assertEquals(emptyList(), library(Annotated::class))
    }
}
