package com.inweb.browser.privacy

/**
 * Sandbox/CI-host micro-benchmark for the tracking-protection engine —
 * the MEASURED v1 baseline referenced by docs/PHASE5-PERFORMANCE-DESIGN.md
 * (MASTER-SPEC §9: "Measure performance rather than making unsupported
 * claims").
 *
 * Not a unit test: run via scripts/benchmark_filter_engine.sh. The
 * synthetic corpus mimics EasyList-scale rule shapes; numbers are honest
 * for THIS hardware and JVM only.
 */
object FilterEngineBenchmark {

    @JvmStatic
    fun main(args: Array<String>) {
        val ruleCount = args.firstOrNull()?.toIntOrNull() ?: 25_000
        val decisionsPerWorkload = args.getOrNull(1)?.toIntOrNull() ?: 200

        val corpus = generateRules(ruleCount)
        val listText = corpus.joinToString("\n")

        // --- parse ----------------------------------------------------------
        val parseStart = System.nanoTime()
        val parsed = FilterListParser.parse(listText, "bench")
        val parseMillis = msSince(parseStart)

        val engine = TrackingProtectionEngine(listOf(parsed))

        val blockWorkload = (0 until decisionsPerWorkload).map { i ->
            request("https://b$i.ads${i % 97}.bench.com/x$i.gif")
        }
        val passWorkload = (0 until decisionsPerWorkload).map { i ->
            request("https://clean$i.cleanweb.org/page$i.html")
        }

        // --- warmup (also lazily compiles every rule regex) ------------------
        val warmupStart = System.nanoTime()
        (0 until 12).forEach { i ->
            engine.decide(blockWorkload[i % blockWorkload.size])
            engine.decide(passWorkload[i % passWorkload.size])
        }
        val warmupMillis = msSince(warmupStart)
        val heapMb = approxUsedHeapMb()

        // --- block-heavy workload -------------------------------------------
        val blockStart = System.nanoTime()
        var blocked = 0
        blockWorkload.forEach { if (engine.decide(it).action == FilterAction.BLOCK) blocked++ }
        val blockMillis = msSince(blockStart)

        // --- pass-heavy workload (worst case: every rule is scanned) ---------
        val passStart = System.nanoTime()
        var passed = 0
        passWorkload.forEach { if (engine.decide(it).action == FilterAction.PASS) passed++ }
        val passMillis = msSince(passStart)

        report(
            "rules_requested" to ruleCount.toString(),
            "rules_generated" to corpus.size.toString(),
            "network_rules_parsed" to parsed.networkRules.size.toString(),
            "unsupported_rules" to parsed.unsupportedRuleCount.toString(),
            "parse_ms" to parseMillis.toString(),
            "parse_rules_per_sec" to rate(corpus.size, parseMillis),
            "warmup_ms" to warmupMillis.toString(),
            "approx_heap_after_warmup_mb" to heapMb.toString(),
            "block_decisions" to decisionsPerWorkload.toString(),
            "block_confirmed_blocks" to blocked.toString(),
            "block_ms" to blockMillis.toString(),
            "block_us_per_decision" to microsPer(decisionsPerWorkload, blockMillis),
            "block_decisions_per_sec" to rate(decisionsPerWorkload, blockMillis),
            "pass_decisions" to decisionsPerWorkload.toString(),
            "pass_confirmed_passes" to passed.toString(),
            "pass_ms" to passMillis.toString(),
            "pass_us_per_decision" to microsPer(decisionsPerWorkload, passMillis),
            "pass_decisions_per_sec" to rate(decisionsPerWorkload, passMillis),
        )
    }

    /** Deterministic EasyList-shaped corpus (seeded; stable across runs). */
    private fun generateRules(n: Int): List<String> = (0 until n).map { i ->
        when (i % 20) {
            in 0..11 -> "||b$i.ads${i % 97}.bench.com^"
            in 12..14 -> "||cdn${i % 53}.track.bench.com^\$script,third-party"
            15, 16 -> "/banner${i % 31}x*.gif\$image"
            17 -> "|https://static.bench.com/ad$i.js"
            18 -> "@@||allowed$i.bench.com^"
            else -> "tracker${i % 71}.bench.net^"
        }
    }

    private fun request(url: String) = RequestContext(
        requestUrl = url,
        documentUrl = "https://news.example.com/story",
        resourceType = ResourceType.IMAGE,
    )

    private fun msSince(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000

    private fun rate(count: Int, millis: Long): String =
        if (millis <= 0) "n/a" else (count * 1000L / millis).toString()

    private fun microsPer(count: Int, millis: Long): String =
        if (count <= 0) "n/a" else (millis * 1000L / count).toString()

    private fun approxUsedHeapMb(): Long {
        Runtime.getRuntime().let { return (it.totalMemory() - it.freeMemory()) / (1024 * 1024) }
    }

    private fun report(vararg metrics: Pair<String, String>) {
        println("benchmark=filter-engine")
        for ((key, value) in metrics) println("$key=$value")
    }
}
