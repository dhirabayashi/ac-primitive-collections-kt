// SPDX-License-Identifier: CC0-1.0
package bytecodecheck

import kotlin.metadata.KmFunction
import kotlin.metadata.isInline
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.signature
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.CodeSizeEvaluator

/** One rule violation, with enough context to find it. */
data class Finding(val className: String, val member: String, val rule: String) {
    override fun toString() = "$className.$member: $rule"
}

/** Which of docs/PLAN.md section 7.3's exceptions apply. */
enum class Mode {
    /** Library classes: `@BoxingApi` members and bodies of inline functions with lambda parameters are exempt. */
    LIBRARY,

    /** User code after inlining: no exceptions. */
    STRICT,
}

object BoxingChecker {
    /** HotSpot refuses to compile larger methods. See docs/PLAN.md, section 4.2. */
    const val HUGE_METHOD_LIMIT = 8000

    private val BOX_CLASSES = setOf(
        "java/lang/Integer", "java/lang/Long", "java/lang/Double", "java/lang/Float",
        "java/lang/Short", "java/lang/Byte", "java/lang/Character", "java/lang/Boolean",
    )
    private val BOX_DESCRIPTORS = BOX_CLASSES.map { "L$it;" }
    private val UNBOX_METHODS = setOf(
        "intValue", "longValue", "doubleValue", "floatValue", "shortValue", "byteValue", "charValue", "booleanValue",
    )
    private const val BOXING_API = "BoxingApi;"

    fun check(classBytes: ByteArray, mode: Mode): List<Finding> {
        val findings = mutableListOf<Finding>()
        val reader = ClassReader(classBytes)
        val className = reader.className
        val metadata = MetadataCollector()
        reader.accept(metadata, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        val exemptInline = if (mode == Mode.LIBRARY) metadata.inlineFunctionsWithLambdas() else emptySet()

        reader.accept(
            object : ClassVisitor(Opcodes.ASM9) {
                private var isPublicClass = false

                override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                    isPublicClass = access and Opcodes.ACC_PUBLIC != 0
                }

                override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
                    if (descriptor in BOX_DESCRIPTORS) findings += Finding(className, name, "field of boxed type $descriptor")
                    return null
                }

                override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
                    val member = name + descriptor
                    val isPublic = isPublicClass && access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0
                    val methodFindings = mutableListOf<Finding>()
                    if (isPublic && BOX_DESCRIPTORS.any { it in descriptor }) {
                        methodFindings += Finding(className, member, "public signature uses a boxed type")
                    }
                    var boxingApi = false
                    val checker = object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                            // Pasted code puts the annotation in the default package, so the package may be absent.
                            if (descriptor == "L$BOXING_API" || descriptor.endsWith("/$BOXING_API")) boxingApi = true
                            return null
                        }

                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                            when {
                                owner in BOX_CLASSES && name == "valueOf" -> methodFindings += Finding(className, member, "boxes via $owner.valueOf")
                                owner in BOX_CLASSES && name in UNBOX_METHODS -> methodFindings += Finding(className, member, "unboxes via $owner.$name")
                                owner == "java/lang/Number" && name in UNBOX_METHODS -> methodFindings += Finding(className, member, "unboxes via $owner.$name")
                                owner == "kotlin/jvm/internal/Boxing" || owner == "kotlin/coroutines/jvm/internal/Boxing" -> methodFindings += Finding(className, member, "uses $owner")
                                owner.startsWith("kotlin/jvm/functions/Function") -> methodFindings += Finding(className, member, "calls non-inlined $owner")
                                owner == "kotlin/Pair" -> methodFindings += Finding(className, member, "uses kotlin/Pair")
                            }
                        }

                        override fun visitTypeInsn(opcode: Int, type: String) {
                            if (type == "kotlin/Pair") methodFindings += Finding(className, member, "uses kotlin/Pair")
                        }

                        override fun visitEnd() {
                            val exempt = boxingApi || (mode == Mode.LIBRARY && member in exemptInline)
                            if (!exempt) findings += methodFindings
                        }
                    }
                    // CodeSizeEvaluator tracks the size; a second visitor reports it at the end.
                    return object : CodeSizeEvaluator(Opcodes.ASM9, checker) {
                        override fun visitEnd() {
                            if (maxSize >= HUGE_METHOD_LIMIT) {
                                findings += Finding(className, member, "method size $maxSize reaches the JIT limit of $HUGE_METHOD_LIMIT")
                            }
                            super.visitEnd()
                        }
                    }
                }
            },
            ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
        )
        return findings
    }

    /** Reads the `kotlin.Metadata` annotation, so a non-inline function cannot pass for an inline one. */
    private class MetadataCollector : ClassVisitor(Opcodes.ASM9) {
        private val values = mutableMapOf<String, Any?>()

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            if (descriptor != "Lkotlin/Metadata;") return null
            return object : AnnotationVisitor(Opcodes.ASM9) {
                override fun visit(name: String, value: Any?) {
                    values[name] = value
                }

                override fun visitArray(name: String): AnnotationVisitor = object : AnnotationVisitor(Opcodes.ASM9) {
                    private val items = mutableListOf<String>()

                    override fun visit(ignored: String?, value: Any?) {
                        items += value as String
                    }

                    override fun visitEnd() {
                        values[name] = items
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun inlineFunctionsWithLambdas(): Set<String> {
            if (values.isEmpty()) return emptySet()
            val annotation = kotlin.Metadata(
                kind = values["k"] as Int? ?: 1,
                metadataVersion = (values["mv"] as IntArray?) ?: intArrayOf(),
                data1 = ((values["d1"] as List<String>?) ?: emptyList()).toTypedArray(),
                data2 = ((values["d2"] as List<String>?) ?: emptyList()).toTypedArray(),
                extraString = values["xs"] as String? ?: "",
                packageName = values["pn"] as String? ?: "",
                extraInt = values["xi"] as Int? ?: 0,
            )
            val functions: List<KmFunction> = when (val parsed = KotlinClassMetadata.readStrict(annotation)) {
                is KotlinClassMetadata.Class -> parsed.kmClass.functions
                is KotlinClassMetadata.FileFacade -> parsed.kmPackage.functions
                is KotlinClassMetadata.MultiFileClassPart -> parsed.kmPackage.functions
                else -> emptyList()
            }
            return functions
                .filter { it.isInline && it.valueParameters.any { parameter -> isFunctionType(parameter.type.classifier.toString()) } }
                .mapNotNull { function -> function.signature?.let { it.name + it.descriptor } }
                .toSet()
        }

        private fun isFunctionType(classifier: String) = "kotlin/Function" in classifier
    }
}
