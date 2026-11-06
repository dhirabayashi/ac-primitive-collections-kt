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
    fun fillsKeyAndValueTypes() {
        assertEquals(
            "class LongIntMinHeap { val k: LongArray; val v: IntArray }",
            TemplateEngine.expand("class KTypeVTypeMinHeap { val k: KTypeArray; val v: VTypeArray }", TYPE_SPECS.getValue("Long"), VALUE_SPECS.getValue("Int")),
        )
    }

    @Test
    fun genericValuesGetTypeParametersAndAnErasedStore() {
        assertEquals(
            "class DoubleObjMinHeap<V>(val v: Array<Any?>, x: Any?) { fun f(): V = (v[0] as V); fun g(): DoubleObjMinHeap<V>; val n = arrayOfNulls<Any?>(1) }",
            TemplateEngine.expand(
                "class KTypeVTypeMinHeap/*<V>*/(val v: VTypeArray, x: VStored) { fun f(): VType = V_CAST(v[0]); fun g(): KTypeVTypeMinHeap/*<V>*/; val n = V_NEW_ARRAY(1) }",
                double,
                VALUE_SPECS.getValue("Obj"),
            ),
        )
    }

    @Test
    fun primitiveValuesDropTheCommentedTypeArguments() {
        assertEquals(
            "class IntLongMinHeap(x: Long) { val a = LongArray(1); fun f() = a[0]; fun g(o: Any?) = o is IntLongMinHeap }",
            TemplateEngine.expand(
                "class KTypeVTypeMinHeap/*<V>*/(x: VStored) { val a = V_NEW_ARRAY(1); fun f() = V_CAST(a[0]); fun g(o: Any?) = o is KTypeVTypeMinHeap/*<*>*/ }",
                int,
                VALUE_SPECS.getValue("Long"),
            ),
        )
    }

    @Test
    fun testTemplatesUseAConcreteTypeForGenericValues() {
        assertEquals(
            "class LongObjMinHeapTest { val h = LongObjMinHeap<Int>(); val v: Int = 1.toInt() }",
            TemplateEngine.expand(
                "class KTypeVTypeMinHeapTest { val h = KTypeVTypeMinHeap(); val v: VType = 1.toVType() }",
                TYPE_SPECS.getValue("Long"),
                TEST_VALUE_SPECS.getValue("Obj"),
            ),
        )
    }

    @Test
    fun testTemplatesOfAnyGenericCollectionGetTheTypeArgument() {
        assertEquals(
            "class LongObjHashMapTest { val h = LongObjHashMap<Int>(); val s = LongObjSomethingTestable<Int>() }",
            TemplateEngine.expand(
                "class KTypeVTypeHashMapTest { val h = KTypeVTypeHashMap(); val s = KTypeVTypeSomethingTestable() }",
                TYPE_SPECS.getValue("Long"),
                TEST_VALUE_SPECS.getValue("Obj"),
            ),
        )
    }

    @Test
    fun testTemplatesOfPrimitiveValuesGetNoTypeArgument() {
        assertEquals(
            "class LongIntHashMapTest { val h = LongIntHashMap() }",
            TemplateEngine.expand("class KTypeVTypeHashMapTest { val h = KTypeVTypeHashMap() }", TYPE_SPECS.getValue("Long"), TEST_VALUE_SPECS.getValue("Int")),
        )
    }

    @Test
    fun zeroIsAnOrdinaryMacroForEveryType() {
        assertEquals("0L", TemplateEngine.expand("K_ZERO", TYPE_SPECS.getValue("Long")))
        assertEquals("'\\u0000'", TemplateEngine.expand("K_ZERO", TYPE_SPECS.getValue("Char")))
    }

    @Test
    fun maxHeapsSwapTheOrderAndTheName() {
        assertEquals("class IntMaxHeap { val b = (y < x) }", TemplateEngine.expand("class KTypeMinHeap { val b = K_BEFORE(x, y) }", int, maxHeap = true))
        assertEquals("class IntMinHeap { val b = (x < y) }", TemplateEngine.expand("class KTypeMinHeap { val b = K_BEFORE(x, y) }", int))
        assertEquals("(y.compareTo(x) < 0)", TemplateEngine.expand("K_BEFORE(x, y)", double, maxHeap = true))
    }

    @Test
    fun maxHeapFlagSelectsItsBranch() {
        val template = "//#if K_MAX_HEAP\nlargest\n//#else\nsmallest\n//#endif"
        assertEquals("largest", TemplateEngine.expand(template, int, maxHeap = true))
        assertEquals("smallest", TemplateEngine.expand(template, int))
    }

    @Test
    fun objFlagSelectsItsBranch() {
        val template = "//#if V_OBJ\nerased\n//#else\nprimitive\n//#endif"
        assertEquals("erased", TemplateEngine.expand(template, int, VALUE_SPECS.getValue("Obj")))
        assertEquals("primitive", TemplateEngine.expand(template, int, VALUE_SPECS.getValue("Int")))
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
