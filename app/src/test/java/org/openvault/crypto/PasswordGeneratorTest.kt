package org.openvault.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openvault.core.crypto.PasswordGenerator
import org.openvault.core.model.PasswordOptions
import org.openvault.core.model.StrengthRating

class PasswordGeneratorTest {

    @Test
    fun testPasswordLengthExact() {
        for (len in listOf(8, 12, 16, 24, 32, 64)) {
            val options = PasswordOptions(length = len)
            val password = PasswordGenerator.generate(options)
            assertEquals(len, password.length)
        }
    }

    @Test
    fun testAllCharacterClassesIncluded() {
        val options = PasswordOptions(
            length = 32,
            includeUppercase = true,
            includeLowercase = true,
            includeDigits = true,
            includeSymbols = true
        )
        val password = PasswordGenerator.generate(options)

        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun testAvoidAmbiguousCharacters() {
        val ambiguous = setOf('O', '0', 'I', 'l', '1', '|', '`', '\'', '"', '~')
        val options = PasswordOptions(
            length = 50,
            avoidAmbiguous = true
        )

        for (i in 0 until 10) {
            val password = PasswordGenerator.generate(options)
            for (c in password) {
                assertFalse("Password should not contain ambiguous character $c", c in ambiguous)
            }
        }
    }

    @Test
    fun testDicewarePassphraseGeneration() {
        val options = PasswordOptions(
            isPassphrase = true,
            wordCount = 5,
            separator = "-"
        )
        val passphrase = PasswordGenerator.generate(options)
        val words = passphrase.split("-")

        assertEquals(5, words.size)
        for (w in words) {
            assertTrue(w.isNotBlank())
            assertTrue(w[0].isUpperCase())
        }
    }

    @Test
    fun testEntropyAndStrengthCalculation() {
        val veryWeak = PasswordGenerator.calculateStrength("123456")
        assertEquals(StrengthRating.VERY_WEAK, veryWeak.rating)

        val weak = PasswordGenerator.calculateStrength("password")
        assertEquals(StrengthRating.WEAK, weak.rating)

        val strong = PasswordGenerator.calculateStrength("Kx9#mQ2!vL8@pZ4$")
        assertTrue(strong.rating == StrengthRating.STRONG || strong.rating == StrengthRating.VERY_STRONG)
        assertTrue(strong.entropyBits > 70.0)
    }
}
