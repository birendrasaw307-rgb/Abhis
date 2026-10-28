package com.example.data.security

import com.example.data.model.DecryptedVaultSecret
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 10000
    // Deterministic app-scoped salt for PBKDF2 key derivation
    private val SALT = "SecurePlusVault_v1_CryptoSalt_DecoupledBackend".toByteArray(Charsets.UTF_8)

    fun deriveKey(masterPinOrPass: String): SecretKeySpec {
        val spec = PBEKeySpec(masterPinOrPass.toCharArray(), SALT, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretBytes, "AES")
    }

    fun encrypt(plaintext: String, masterPin: String): Pair<String, String> {
        val key = deriveKey(masterPin)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext)
        val ivB64 = Base64.getEncoder().encodeToString(iv)
        return Pair(ciphertextB64, ivB64)
    }

    fun decrypt(ciphertextB64: String, ivB64: String, masterPin: String): String {
        val key = deriveKey(masterPin)
        val iv = Base64.getDecoder().decode(ivB64)
        val ciphertext = Base64.getDecoder().decode(ciphertextB64)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun serializeSecret(secret: DecryptedVaultSecret): String {
        return JSONObject().apply {
            put("accountUsername", secret.accountUsername)
            put("secretPasswordOrKey", secret.secretPasswordOrKey)
            put("websiteOrService", secret.websiteOrService)
            put("notes", secret.notes)
        }.toString()
    }

    fun deserializeSecret(jsonStr: String): DecryptedVaultSecret {
        return try {
            val json = JSONObject(jsonStr)
            DecryptedVaultSecret(
                accountUsername = json.optString("accountUsername", ""),
                secretPasswordOrKey = json.optString("secretPasswordOrKey", ""),
                websiteOrService = json.optString("websiteOrService", ""),
                notes = json.optString("notes", "")
            )
        } catch (e: Exception) {
            // Fallback if legacy raw string
            DecryptedVaultSecret(secretPasswordOrKey = jsonStr)
        }
    }
}
