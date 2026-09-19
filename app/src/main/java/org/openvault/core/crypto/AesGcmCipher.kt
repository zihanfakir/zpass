package org.openvault.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class AesGcmOutput(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val authTag: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AesGcmOutput
        return ciphertext.contentEquals(other.ciphertext) &&
                iv.contentEquals(other.iv) &&
                authTag.contentEquals(other.authTag)
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + authTag.contentHashCode()
        return result
    }
}

object AesGcmCipher {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12 // 96 bits recommended for GCM

    private val secureRandom = SecureRandom()

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     * In standard Java JCE, the 16-byte authentication tag is appended to the ciphertext.
     */
    fun encrypt(
        plaintext: ByteArray,
        keyBytes: ByteArray,
        aad: ByteArray? = null
    ): AesGcmOutput {
        require(keyBytes.size == 32) { "AES-256 key must be 32 bytes" }

        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }

        val encryptedWithTag = cipher.doFinal(plaintext)
        return AesGcmOutput(
            ciphertext = encryptedWithTag,
            iv = iv
        )
    }

    /**
     * Decrypts AES-256-GCM ciphertext (which includes the 16-byte authentication tag).
     * Throws AEADBadTagException if ciphertext, IV, or key has been tampered with.
     */
    fun decrypt(
        ciphertextWithTag: ByteArray,
        keyBytes: ByteArray,
        iv: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        require(keyBytes.size == 32) { "AES-256 key must be 32 bytes" }
        require(iv.size == IV_LENGTH_BYTES) { "IV must be 12 bytes" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        if (aad != null && aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }

        return cipher.doFinal(ciphertextWithTag)
    }
}
