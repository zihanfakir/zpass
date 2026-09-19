package org.openvault.core.crypto

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeyDerivation {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    const val DEFAULT_ITERATIONS = 600_000
    const val PIN_ITERATIONS = 100_000
    const val SALT_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 32-byte (256-bit) salt.
     */
    fun generateSalt(lengthBytes: Int = SALT_LENGTH_BYTES): ByteArray {
        val salt = ByteArray(lengthBytes)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Generates a 256-bit random cryptographic key for the Symmetric Vault Key (SVK).
     */
    fun generateVaultKey(): ByteArray {
        val key = ByteArray(32)
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * Derives a 256-bit Master Encryption Key (MEK) from a master password char array and salt.
     * Overwrites PBEKeySpec memory after derivation.
     */
    fun deriveMasterKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Derives a 256-bit key from a numeric Quick PIN.
     */
    fun derivePinKey(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int = PIN_ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
