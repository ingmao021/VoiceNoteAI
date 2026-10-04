package com.voicenoteai.wear.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RmsNormalizerTest {
    private val normalizer = RmsNormalizer(minDb = -2f, maxDb = 10f, alpha = 0.3f)

    @Test
    fun `output always stays between 0 and 1`() {
        listOf(-100f, -2f, 0f, 5f, 10f, 50f, -100f).forEach {
            val v = normalizer.normalize(it)
            assertTrue("$it → $v", v in 0f..1f)
        }
    }

    @Test
    fun `the first sample does not jump from silence`() {
        // Con alfa 0,3 un pico máximo avanza solo un 30 % del recorrido.
        assertEquals(0.3f, normalizer.normalize(10f), 1e-4f)
    }

    @Test
    fun `a sustained voice converges to the clipped level`() {
        repeat(50) { normalizer.normalize(10f) }
        assertEquals(1f, normalizer.normalize(10f), 1e-3f)
    }

    @Test
    fun `settles down in silence`() {
        repeat(50) { normalizer.normalize(10f) }
        repeat(50) { normalizer.normalize(-2f) }
        assertEquals(0f, normalizer.normalize(-2f), 1e-3f)
    }

    @Test
    fun `smoothing bounds the change between consecutive samples`() {
        var previous = normalizer.normalize(-2f)
        listOf(10f, -2f, 10f, -2f, 8f, 0f).forEach {
            val current = normalizer.normalize(it)
            assertTrue("jump from $previous to $current", kotlin.math.abs(current - previous) <= 0.3f + 1e-4f)
            previous = current
        }
    }

    @Test
    fun `reset returns to silence`() {
        repeat(20) { normalizer.normalize(10f) }
        normalizer.reset()
        assertEquals(0.3f, normalizer.normalize(10f), 1e-4f)
    }
}
