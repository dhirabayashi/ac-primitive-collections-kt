// SPDX-License-Identifier: CC0-1.0
// Paste into AtCoder's custom test (Kotlin) to record the judge environment. See docs/PLAN.md, Phase 0.
import com.sun.management.HotSpotDiagnosticMXBean
import java.io.File
import java.lang.management.ManagementFactory
import java.util.SplittableRandom

fun vmOption(bean: HotSpotDiagnosticMXBean, name: String): String =
    try {
        bean.getVMOption(name).value
    } catch (e: IllegalArgumentException) {
        "n/a"
    }

fun readOrNull(path: String): String? =
    try {
        File(path).readText()
    } catch (e: Exception) {
        null
    }

fun printEnvironment() {
    val runtime = Runtime.getRuntime()
    val bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)
    println("kotlin=${KotlinVersion.CURRENT} java=${System.getProperty("java.version")} vm=${System.getProperty("java.vm.name")}")
    println("os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
    println("args=${ManagementFactory.getRuntimeMXBean().inputArguments}")
    println("processors=${runtime.availableProcessors()} maxMemoryMiB=${runtime.maxMemory() shr 20}")
    println("gc=${ManagementFactory.getGarbageCollectorMXBeans().map { it.name }}")
    for (name in listOf("UseAVX", "UseSSE", "MaxHeapSize", "InitialHeapSize", "ThreadStackSize", "CICompilerCount", "TieredCompilation", "DontCompileHugeMethods")) {
        println("vm.$name=${vmOption(bean, name)}")
    }
    val cpuInfo = readOrNull("/proc/cpuinfo")
    if (cpuInfo == null) {
        println("cpuinfo=unavailable")
    } else {
        val lines = cpuInfo.lineSequence()
        println("cpu=${lines.firstOrNull { it.startsWith("model name") }?.substringAfter(':')?.trim()}")
        val flags = lines.firstOrNull { it.startsWith("flags") }?.substringAfter(':')?.trim()?.split(' ')?.toSet().orEmpty()
        println("cpuFlags=${listOf("avx", "avx2", "avx512f", "avx512dq", "bmi2").filter { it in flags }}")
    }
    println("memTotal=${readOrNull("/proc/meminfo")?.lineSequence()?.firstOrNull { it.startsWith("MemTotal") }}")
    println("cgroupMemoryMax=${readOrNull("/sys/fs/cgroup/memory.max")?.trim()}")
    println("cgroupCpuMax=${readOrNull("/sys/fs/cgroup/cpu.max")?.trim()}")
}

/** LSD radix sort (8 bits, 8 passes) in signed order. Eight passes leave the result in [a]. */
fun radixSort(a: LongArray) {
    var src = a
    var dst = LongArray(a.size)
    val counts = IntArray(257)
    for (pass in 0 until 8) {
        val shift = pass * 8
        val flip = if (pass == 7) 128 else 0
        counts.fill(0)
        for (x in src) counts[(((x ushr shift).toInt() and 255) xor flip) + 1]++
        for (i in 0 until 256) counts[i + 1] += counts[i]
        for (x in src) dst[counts[((x ushr shift).toInt() and 255) xor flip]++] = x
        val t = src
        src = dst
        dst = t
    }
}

inline fun timeMs(body: () -> Unit): Long {
    val start = System.nanoTime()
    body()
    return (System.nanoTime() - start) / 1_000_000
}

fun report(label: String, n: Int, times: List<Long>) {
    println("sort label=$label n=$n firstMs=${times.first()} minMs=${times.min()}")
}

fun benchmark(n: Int, rounds: Int) {
    val r = SplittableRandom(20261105L)
    val ints = IntArray(n) { r.nextInt() }
    val longs = LongArray(n) { r.nextLong() }
    val doubles = DoubleArray(n) { r.nextDouble() }
    report("int-Arrays.sort", n, List(rounds) { val c = ints.copyOf(); timeMs { java.util.Arrays.sort(c) } })
    report("long-Arrays.sort", n, List(rounds) { val c = longs.copyOf(); timeMs { java.util.Arrays.sort(c) } })
    report("long-radix8", n, List(rounds) { val c = longs.copyOf(); timeMs { radixSort(c) } })
    report("double-Arrays.sort", n, List(rounds) { val c = doubles.copyOf(); timeMs { java.util.Arrays.sort(c) } })

    val expected = longs.copyOf().also { java.util.Arrays.sort(it) }
    val actual = longs.copyOf().also { radixSort(it) }
    check(expected.contentEquals(actual)) { "radixSort disagrees with Arrays.sort" }
}

fun main() {
    printEnvironment()
    benchmark(200_000, 5)
    benchmark(5_000_000, 3)
}
