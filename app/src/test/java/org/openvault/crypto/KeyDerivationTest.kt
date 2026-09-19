package org.openvault.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openvault.core.crypto.KeyDerivation
import org.openvault.core.crypto.zeroize

class KeyDerivationTest {

    @Test
    fun testDeterministicDerivationWithSameInputs() {
        val password = "StrongMasterPassword123!".toCharArray()
        val salt = byteArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
        )

        // Using 1,000 rounds for fast unit test
        val key1 = KeyDerivation.deriveMasterKey(password, salt, iterations = 1000)
        val key2 = KeyDerivation.deriveMasterKey(password, salt, iterations = 1000)

        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)
    }

    @Test
    fun testDistinctSaltsProduceDistinctKeys() {
        val password = "MySecretPassword".toCharArray()
        val salt1 = KeyDerivation.generateSalt()
        val salt2 = KeyDerivation.generateSalt()

        val key1 = KeyDerivation.deriveMasterKey(password, salt1, iterations = 1000)
        val key2 = KeyDerivation.deriveMasterKey(password, salt2, iterations = 1000)

        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun testZeroizeOverwritesSensitiveMemory() {
        val buffer = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45)
        buffer.zeroize()

        for (b in buffer) {
            assertEquals(0.toByte(), b)
        }

        val chars = charArrayOf('p', 'a', 's', 's', 'w', 'o', 'r', 'd')
        chars.zeroize()

        for (c in chars) {
            assertEquals('\u0000', c)
        }
    }
}
