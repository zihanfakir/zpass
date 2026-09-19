package org.openvault.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openvault.core.crypto.TotpGenerator

class TotpGeneratorTest {

    // RFC 6238 standard test secret: "12345678901234567890" (20 bytes ASCII)
    // Base32 representation: "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val rfcSecretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun testRfc6238TestVectorsSha1() {
        // T0 = 59s -> step 1
        val result1 = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 59L, digits = 8)
        assertEquals("94287082", result1.code)

        val result1_6digit = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 59L, digits = 6)
        assertEquals("287082", result1_6digit.code)

        // T = 1111111109s -> step 37037036
        val result2 = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 1111111109L, digits = 8)
        assertEquals("07081804", result2.code)

        // T = 1111111111s -> step 37037037
        val result3 = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 1111111111L, digits = 8)
        assertEquals("14050471", result3.code)

        // T = 1234567890s -> step 41152263
        val result4 = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 1234567890L, digits = 8)
        assertEquals("89005924", result4.code)

        // T = 2000000000s -> step 66666666
        val result5 = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 2000000000L, digits = 8)
        assertEquals("69279037", result5.code)
    }

    @Test
    fun testRemainingSecondsAndProgress() {
        // At 14 seconds into a 30-second window, remaining should be 16 seconds
        val result = TotpGenerator.generateToken(rfcSecretBase32, timeEpochSeconds = 14L, periodSeconds = 30L)
        assertEquals(16, result.secondsRemaining)
        assertTrue(result.progress in 0.0f..1.0f)
        assertEquals(16f / 30f, result.progress, 0.01f)
    }

    @Test
    fun testParseOtpAuthUri() {
        val uri = "otpauth://totp/ProtonMail:privacy%40proton.me?secret=JBSWY3DPEHPK3PXP&issuer=ProtonMail&digits=6&period=30"
        val config = TotpGenerator.parseOtpAuthUri(uri)

        assertNotNull(config)
        assertEquals("JBSWY3DPEHPK3PXP", config?.secret)
        assertEquals("ProtonMail", config?.issuer)
        assertEquals("privacy@proton.me", config?.accountName)
        assertEquals(6, config?.digits)
        assertEquals(30L, config?.periodSeconds)
    }

    @Test
    fun testBase32Decoding() {
        // "JBSWY3DPEHPK3PXP" is Base32 for "Hello!1234"
        val decoded = TotpGenerator.decodeBase32("JBSWY3DPEHPK3PXP")
        assertEquals(10, decoded.size)
        // Check "Hello!"
        assertEquals('H'.code.toByte(), decoded[0])
        assertEquals('e'.code.toByte(), decoded[1])
        assertEquals('l'.code.toByte(), decoded[2])
        assertEquals('l'.code.toByte(), decoded[3])
        assertEquals('o'.code.toByte(), decoded[4])
        assertEquals('!'.code.toByte(), decoded[5])
    }
}
