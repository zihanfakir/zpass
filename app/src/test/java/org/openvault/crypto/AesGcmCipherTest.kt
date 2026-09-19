package org.openvault.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openvault.core.crypto.AesGcmCipher
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class AesGcmCipherTest {

    private val secureRandom = SecureRandom()

    private fun generateKey(): ByteArray {
        val key = ByteArray(32)
        secureRandom.nextBytes(key)
        return key
    }

    @Test
    fun testEncryptionDecryptionRoundTrip() {
        val key = generateKey()
        val originalPlaintext = "OpenVault: Zero-Knowledge Privacy-First Password Manager".toByteArray(Charsets.UTF_8)

        val output = AesGcmCipher.encrypt(originalPlaintext, key)
        assertNotNull(output.ciphertext)
        assertEquals(12, output.iv.size)

        val decrypted = AesGcmCipher.decrypt(output.ciphertext, key, output.iv)
        assertArrayEquals(originalPlaintext, decrypted)
        assertEquals("OpenVault: Zero-Knowledge Privacy-First Password Manager", String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun testTamperedCiphertextFailsAuthentication() {
        val key = generateKey()
        val plaintext = "Sensitive Credentials".toByteArray(Charsets.UTF_8)

        val output = AesGcmCipher.encrypt(plaintext, key)

        // Flip one bit in ciphertext
        val tamperedCiphertext = output.ciphertext.clone()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0x01).toByte()

        assertThrows(AEADBadTagException::class.java) {
            AesGcmCipher.decrypt(tamperedCiphertext, key, output.iv)
        }
    }

    @Test
    fun testTamperedIvFailsAuthentication() {
        val key = generateKey()
        val plaintext = "Sensitive Credentials".toByteArray(Charsets.UTF_8)

        val output = AesGcmCipher.encrypt(plaintext, key)

        // Flip one bit in IV
        val tamperedIv = output.iv.clone()
        tamperedIv[0] = (tamperedIv[0].toInt() xor 0x01).toByte()

        assertThrows(AEADBadTagException::class.java) {
            AesGcmCipher.decrypt(output.ciphertext, key, tamperedIv)
        }
    }

    @Test
    fun testAadBindingPreventsSubstitution() {
        val key = generateKey()
        val plaintext = "Account Password".toByteArray(Charsets.UTF_8)
        val aad1 = "item-id-001".toByteArray(Charsets.UTF_8)
        val aad2 = "item-id-002".toByteArray(Charsets.UTF_8)

        val output = AesGcmCipher.encrypt(plaintext, key, aad = aad1)

        // Decrypting with matching AAD succeeds
        val decrypted = AesGcmCipher.decrypt(output.ciphertext, key, output.iv, aad = aad1)
        assertArrayEquals(plaintext, decrypted)

        // Decrypting with mismatched AAD fails tag verification
        assertThrows(AEADBadTagException::class.java) {
            AesGcmCipher.decrypt(output.ciphertext, key, output.iv, aad = aad2)
        }
    }

    @Test
    fun testKeyLengthValidation() {
        val invalidKey = ByteArray(16) // Only 128-bit
        val plaintext = "Test".toByteArray(Charsets.UTF_8)

        assertThrows(IllegalArgumentException::class.java) {
            AesGcmCipher.encrypt(plaintext, invalidKey)
        }
    }
}
