package com.example

import com.example.data.security.SecurityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSecurityAnalyzer() {
        val weakResult = SecurityAnalyzer.evaluatePasswordStrength("12345")
        assertEquals("WEAK", weakResult.rating)

        val generated = SecurityAnalyzer.generateSecurePassword(24)
        assertEquals(24, generated.length)

        val strongResult = SecurityAnalyzer.evaluatePasswordStrength(generated)
        assertTrue(strongResult.score >= 70)
    }
}
