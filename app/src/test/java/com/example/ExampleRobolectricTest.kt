package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DecryptedVaultSecret
import com.example.data.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SecurePlus", appName)
    }

    @Test
    fun testCryptoManagerEncryptDecryptInRobolectric() {
        val masterPin = "9876"
        val originalSecret = DecryptedVaultSecret(
            accountUsername = "admin_user",
            secretPasswordOrKey = "SuperSecret_AES256_Token#2026",
            websiteOrService = "https://backend.example.com",
            notes = "Top secret credentials"
        )

        val serialized = CryptoManager.serializeSecret(originalSecret)
        val (ciphertextB64, ivB64) = CryptoManager.encrypt(serialized, masterPin)

        assertNotNull(ciphertextB64)
        assertNotNull(ivB64)
        assertNotEquals(serialized, ciphertextB64)

        val decryptedJson = CryptoManager.decrypt(ciphertextB64, ivB64, masterPin)
        val restoredSecret = CryptoManager.deserializeSecret(decryptedJson)

        assertEquals(originalSecret.accountUsername, restoredSecret.accountUsername)
        assertEquals(originalSecret.secretPasswordOrKey, restoredSecret.secretPasswordOrKey)
        assertEquals(originalSecret.websiteOrService, restoredSecret.websiteOrService)
        assertEquals(originalSecret.notes, restoredSecret.notes)
    }
}
