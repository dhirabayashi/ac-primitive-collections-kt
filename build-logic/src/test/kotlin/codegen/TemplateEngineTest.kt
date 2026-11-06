// SPDX-License-Identifier: CC0-1.0
package codegen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateEngineTest {
    private val int = TYPE_SPECS.getValue("Int")
    private val double = TYPE_SPECS.getValue("Double")

    @Test
    fun replacesTokensInsideLongerNames() {
        assertEquals(
            "class IntList { fun toIntArray(): IntArray = a }\nfun intListOf() = 0",
            TemplateEngine.expand("class KTypeList { fun toKTypeArray(): KTypeArray = a }\nfun ktypeListOf() = K_ZERO", int),
        )
    }

    @Test
    fun usesTotalOrderOperatorsForFloatingTypes() {
        assertEquals("(a.compareTo(b) == 0)", TemplateEngine.expand("K_EQ(a, b)", double))
        assertEquals("(a.compareTo(b) < 0)", TemplateEngine.expand("K_LT(a, b)", double))
        assertEquals("(a == b)", TemplateEngine.expand("K_EQ(a, b)", int))
    }

    @Test
    fun expandsNestedMacroArguments() {
        assertEquals(
            "((f(x, y) == z) == w)",
            TemplateEngine.expand("K_EQ(K_EQ(f(x, y), z), w)", int),
        )
    }

    @Test
    fun keepsOnlyTheSelectedBranch() {
        val template = "a\n//#if K_FLOATING\nb\n//#else\nc\n//#endif\nd"
        assertEquals("a\nc\nd", TemplateEngine.expand(template, int))
        assertEquals("a\nb\nd", TemplateEngine.expand(template, double))
    }

    @Test
    fun handlesNestedConditionalsInInactiveBranches() {
        val template = "//#if K_FLOATING\n//#if K_NUMERIC\nb\n//#else\nc\n//#endif\n//#else\nd\n//#endif"
        assertEquals("d", TemplateEngine.expand(template, int))
        assertEquals("b", TemplateEngine.expand(template, double))
    }

    @Test
    fun rejectsMalformedTemplates() {
        assertFailsWith<IllegalArgumentException> { TemplateEngine.expand("//#if K_NOPE\n//#endif", int) }
        assertFailsWith<IllegalArgumentException> { TemplateEngine.expand("//#if K_NUMERIC\nx", int) }
        assertFailsWith<IllegalStateException> { TemplateEngine.expand("//#else", int) }
        assertFailsWith<IllegalArgumentException> { TemplateEngine.expand("//#endif", int) }
        assertFailsWith<IllegalArgumentException> { TemplateEngine.expand("//#iff K_NUMERIC", int) }
        assertFailsWith<IllegalStateException> { TemplateEngine.expand("K_UNKNOWN(a)", int) }
        assertFailsWith<IllegalArgumentException> { TemplateEngine.expand("K_EQ(a)", int) }
        assertFailsWith<IllegalStateException> { TemplateEngine.expand("K_EQ(a, b", int) }
    }
}
