package fr.hardel.asset_editor.client.compose.lib.utils

/**
 * Kotlin port of git xdiff's histogram diff (`xhistogram.c`).
 *
 * Indexes the rarest lines of A, finds the longest matching region in B around
 * each candidate, then recurses on the prefix/suffix. Produces alignments very
 * close to GitHub's web UI for source code with repeating tokens (braces, etc.).
 *
 * Output is two parallel boolean arrays (`changedA`, `changedB`) marking which
 * lines were not part of any matching region.
 */
internal object HistogramDiff {

    private const val MAX_CHAIN_LENGTH = 64

    data class Result(val changedA: BooleanArray, val changedB: BooleanArray)

    fun diff(a: List<String>, b: List<String>): Result {
        val changedA = BooleanArray(a.size)
        val changedB = BooleanArray(b.size)
        if (a.isNotEmpty() || b.isNotEmpty()) {
            histogram(a, b, 0, a.size, 0, b.size, changedA, changedB)
        }
        return Result(changedA, changedB)
    }

    /**
     * Recurses on the prefix and tail-recurses on the suffix around the best
     * matching region of `[startA, startA + countA)` against `[startB, startB + countB)`.
     */
    private fun histogram(
        a: List<String>,
        b: List<String>,
        startA: Int,
        countA: Int,
        startB: Int,
        countB: Int,
        changedA: BooleanArray,
        changedB: BooleanArray
    ) {
        var aStart = startA
        var aCount = countA
        var bStart = startB
        var bCount = countB

        while (true) {
            if (aCount == 0 && bCount == 0) return
            if (aCount == 0) {
                for (i in 0 until bCount) changedB[bStart + i] = true
                return
            }
            if (bCount == 0) {
                for (i in 0 until aCount) changedA[aStart + i] = true
                return
            }

            val region = findLcs(a, b, aStart, aCount, bStart, bCount)
            if (region == null) {
                for (i in 0 until aCount) changedA[aStart + i] = true
                for (i in 0 until bCount) changedB[bStart + i] = true
                return
            }

            histogram(
                a, b,
                aStart, region.beginA - aStart,
                bStart, region.beginB - bStart,
                changedA, changedB
            )
            val nextAStart = region.endA + 1
            val nextBStart = region.endB + 1
            aCount = (aStart + aCount) - nextAStart
            bCount = (bStart + bCount) - nextBStart
            aStart = nextAStart
            bStart = nextBStart
        }
    }

    private data class Region(var beginA: Int, var endA: Int, var beginB: Int, var endB: Int)

    /**
     * Finds the rarest matching region between two slices of `a` and `b`.
     *
     * Builds an open-addressed hash table over the lines of `a` (one record per
     * unique line content) plus, for each A-line, an intrusive linked list of
     * the previous duplicate. A is scanned end-to-start so the head of every
     * chain is the earliest occurrence in the slice. B is then walked left to
     * right; for each line we expand any matching A-occurrence in both
     * directions and keep the longest expansion whose minimum line rarity is
     * lowest. Returns `null` if any chain exceeds [MAX_CHAIN_LENGTH], in which
     * case the caller treats the whole region as a literal change.
     */
    private fun findLcs(
        a: List<String>,
        b: List<String>,
        aStart: Int,
        aCount: Int,
        bStart: Int,
        bCount: Int
    ): Region? {
        val tableBits = hashBits(aCount)
        val tableSize = 1 shl tableBits
        val tableMask = tableSize - 1

        val buckets = IntArray(tableSize) { -1 }
        val recPtr = IntArray(aCount)
        val recCnt = IntArray(aCount)
        val recNext = IntArray(aCount)
        val lineRecord = IntArray(aCount) { -1 }
        val nextOccurrence = IntArray(aCount)
        var recordCount = 0

        var overflow = false
        run scan@{
            for (offset in aCount - 1 downTo 0) {
                val lineIdx = aStart + offset
                val key = a[lineIdx].hashCode()
                val bucket = key and tableMask

                var rec = buckets[bucket]
                var chainLen = 0
                while (rec != -1) {
                    if (a[recPtr[rec]] == a[lineIdx]) {
                        nextOccurrence[offset] = (recPtr[rec] - aStart)
                        recPtr[rec] = lineIdx
                        if (recCnt[rec] < Int.MAX_VALUE) recCnt[rec]++
                        lineRecord[offset] = rec
                        break
                    }
                    rec = recNext[rec]
                    chainLen++
                }
                if (rec != -1) continue

                if (chainLen >= MAX_CHAIN_LENGTH) {
                    overflow = true
                    return@scan
                }

                val newRec = recordCount++
                recPtr[newRec] = lineIdx
                recCnt[newRec] = 1
                recNext[newRec] = buckets[bucket]
                buckets[bucket] = newRec
                lineRecord[offset] = newRec
                nextOccurrence[offset] = -1
            }
        }

        if (overflow) return null

        var best: Region? = null
        var bestSpan = 0
        var bestRarity = MAX_CHAIN_LENGTH + 1

        var bIdx = bStart
        val bEnd = bStart + bCount
        while (bIdx < bEnd) {
            val bLine = b[bIdx]
            val bucket = bLine.hashCode() and tableMask
            var rec = buckets[bucket]
            var advancedTo = bIdx + 1

            while (rec != -1) {
                if (recCnt[rec] > bestRarity || a[recPtr[rec]] != bLine) {
                    rec = recNext[rec]
                    continue
                }

                var aOffset = recPtr[rec] - aStart
                while (aOffset >= 0) {
                    var rarity = recCnt[rec]
                    var aS = aOffset
                    var bS = bIdx - bStart
                    var aE = aOffset
                    var bE = bS

                    while (aS > 0 && bS > 0 && a[aStart + aS - 1] == b[bStart + bS - 1]) {
                        aS--; bS--
                        if (rarity > 1) {
                            val r = lineRecord[aS]
                            if (r >= 0) rarity = minOf(rarity, recCnt[r])
                        }
                    }
                    while (aE + 1 < aCount && bE + 1 < bCount &&
                        a[aStart + aE + 1] == b[bStart + bE + 1]) {
                        aE++; bE++
                        if (rarity > 1) {
                            val r = lineRecord[aE]
                            if (r >= 0) rarity = minOf(rarity, recCnt[r])
                        }
                    }

                    val span = aE - aS
                    if (bStart + bE + 1 > advancedTo) advancedTo = bStart + bE + 1
                    if (span > bestSpan || rarity < bestRarity) {
                        bestSpan = span
                        bestRarity = rarity
                        best = Region(
                            beginA = aStart + aS,
                            endA = aStart + aE,
                            beginB = bStart + bS,
                            endB = bStart + bE
                        )
                    }

                    val nextOcc = nextOccurrence[aOffset]
                    if (nextOcc < 0) break
                    var probe = nextOcc
                    while (probe in 0..aE) {
                        probe = nextOccurrence[probe]
                        if (probe < 0) break
                    }
                    if (probe < 0) break
                    aOffset = probe
                }

                rec = recNext[rec]
            }
            bIdx = advancedTo
        }

        return best
    }

    private fun hashBits(size: Int): Int {
        var bits = 4
        var capacity = 1 shl bits
        while (capacity < size && bits < 30) {
            bits++
            capacity = capacity shl 1
        }
        return bits
    }
}
