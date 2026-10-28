package com.example.data.security

import java.security.SecureRandom
import kotlin.math.ln
import kotlin.math.roundToInt

data class PasswordStrengthResult(
    val score: Int, // 0 - 100
    val rating: String, // "WEAK", "FAIR", "GOOD", "STRONG", "ENTERPRISE"
    val entropyBits: Double,
    val feedback: List<String>
)

object SecurityAnalyzer {
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    fun evaluatePasswordStrength(password: String): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(0, "EMPTY", 0.0, listOf("Password cannot be empty"))
        }

        var poolSize = 0
        val feedback = mutableListOf<String>()

        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        if (hasUpper) poolSize += 26 else feedback.add("Add uppercase letters (A-Z)")
        if (hasLower) poolSize += 26 else feedback.add("Add lowercase letters (a-z)")
        if (hasDigit) poolSize += 10 else feedback.add("Add numbers (0-9)")
        if (hasSymbol) poolSize += 32 else feedback.add("Add special symbols (!@#$)")

        if (password.length < 8) {
            feedback.add("Increase length to at least 12 characters")
        } else if (password.length < 12) {
            feedback.add("Length is acceptable, 16+ is recommended for maximum security")
        }

        // Shannon entropy = L * log2(poolSize)
        val entropy = if (poolSize > 0) password.length * (ln(poolSize.toDouble()) / ln(2.0)) else 0.0
        val rawScore = ((entropy / 80.0) * 100.0).roundToInt().coerceIn(0, 100)

        val rating = when {
            rawScore >= 85 -> "ENTERPRISE"
            rawScore >= 70 -> "STRONG"
            rawScore >= 50 -> "GOOD"
            rawScore >= 30 -> "FAIR"
            else -> "WEAK"
        }

        return PasswordStrengthResult(
            score = rawScore,
            rating = rating,
            entropyBits = entropy,
            feedback = feedback
        )
    }

    fun generateSecurePassword(
        length: Int = 20,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val allowedChars = StringBuilder().apply {
            if (includeUpper) append(UPPERCASE)
            if (includeLower) append(LOWERCASE)
            if (includeDigits) append(NUMBERS)
            if (includeSymbols) append(SYMBOLS)
        }.toString()

        if (allowedChars.isEmpty()) return ""

        val random = SecureRandom()
        val result = CharArray(length)
        for (i in 0 until length) {
            result[i] = allowedChars[random.nextInt(allowedChars.length)]
        }
        return String(result)
    }
}
