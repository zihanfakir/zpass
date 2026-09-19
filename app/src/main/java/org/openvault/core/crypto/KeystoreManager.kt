package org.openvault.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS = "openvault_biometric_key"
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    fun isKeystoreAvailable(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Generates a hardware-backed AES-256 key inside AndroidKeyStore.
     */
    fun getOrCreateBiometricKey(requireUserAuth: Boolean = true): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (requireUserAuth) {
            builder.setUserAuthenticationRequired(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /**
     * Wraps the Symmetric Vault Key (SVK) using the Keystore key.
     */
    fun wrapVaultKey(vaultKey: ByteArray, secretKey: SecretKey): AesGcmOutput {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedKey = cipher.doFinal(vaultKey)
        return AesGcmOutput(ciphertext = encryptedKey, iv = iv)
    }

    /**
     * Unwraps the Symmetric Vault Key (SVK) using the Keystore key.
     */
    fun unwrapVaultKey(
        encryptedVaultKey: ByteArray,
        iv: ByteArray,
        secretKey: SecretKey
    ): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encryptedVaultKey)
    }

    /**
     * Deletes the biometric key from the Keystore.
     */
    fun deleteBiometricKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (e: Throwable) {
            // Ignored if Keystore unavailable
        }
    }
}
