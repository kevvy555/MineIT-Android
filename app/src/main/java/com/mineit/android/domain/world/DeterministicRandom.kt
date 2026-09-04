package com.mineit.android.domain.world

/** JavaScript-compatible FNV-1a hash and seededRandom/mulberry32 used by MineIT web. */
object DeterministicHash {
    fun hashString(value: String): UInt {
        var hash = 2_166_136_261u
        for (character in value) {
            hash = hash xor character.code.toUInt()
            hash *= 16_777_619u
        }
        return hash
    }
}

class DeterministicRandom(seed: UInt) {
    private var state: UInt = seed

    fun nextDouble(): Double {
        state += 0x6D2B79F5u
        var t = state
        t = (t xor (t shr 15)) * (t or 1u)
        t = t xor (t + ((t xor (t shr 7)) * (t or 61u)))
        val result = t xor (t shr 14)
        return result.toLong().toDouble() / UINT_RANGE
    }

    companion object {
        private const val UINT_RANGE = 4_294_967_296.0
    }
}
