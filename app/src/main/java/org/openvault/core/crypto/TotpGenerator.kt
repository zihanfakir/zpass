package org.openvault.core.crypto

import java.net.URLDecoder
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class TotpConfig(
    val secret: String,
    val issuer: String = "",
    val accountName: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val periodSeconds: Long = 30
)

data class TotpResult(
    val code: String,
    val secondsRemaining: Int,
    val progress: Float
)

object TotpGenerator {

    /**
     * Generates the current TOTP token and remaining seconds for a given Base32 secret.
     */
    fun generateToken(
        secretBase32: String,
        timeEpochSeconds: Long = System.currentTimeMillis() / 1000L,
        periodSeconds: Long = 30L,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): TotpResult {
        val cleanSecret = secretBase32.trim().replace(" ", "").uppercase()
        val keyBytes = decodeBase32(cleanSecret)
        val timeStep = timeEpochSeconds / periodSeconds

        val codeInt = generateHotpValue(keyBytes, timeStep, digits, algorithm)
        val codeString = codeInt.toString().padStart(digits, '0')

        val elapsed = (timeEpochSeconds % periodSeconds).toInt()
        val remaining = (periodSeconds - elapsed).toInt()
        val progress = remaining.toFloat() / periodSeconds.toFloat()

        return TotpResult(code = codeString, secondsRemaining = remaining, progress = progress)
    }

    /**
     * RFC 4226 / RFC 6238 HOTP value generator.
     */
    fun generateHotpValue(
        keyBytes: ByteArray,
        counter: Long,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): Int {
        val macAlgorithm = when (algorithm.uppercase()) {
            "SHA256", "HMACSHA256" -> "HmacSHA256"
            "SHA512", "HMACSHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(macAlgorithm)
        mac.init(SecretKeySpec(keyBytes, "RAW"))
        val hash = mac.doFinal(counterBytes)

        // Dynamic truncation (RFC 4226 section 5.4)
        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulus = 10.0.pow(digits.toDouble()).toInt()
        return binary % modulus
    }

    /**
     * Parses an otpauth:// URL into TotpConfig.
     */
    fun parseOtpAuthUri(uriString: String): TotpConfig? {
        if (!uriString.startsWith("otpauth://totp/", ignoreCase = true)) return null

        return try {
            val afterPrefix = uriString.substring("otpauth://totp/".length)
            val parts = afterPrefix.split("?", limit = 2)
            val label = URLDecoder.decode(parts[0], "UTF-8")
            val params = if (parts.size > 1) {
                parts[1].split("&").associate {
                    val kv = it.split("=", limit = 2)
                    if (kv.size == 2) kv[0].lowercase() to URLDecoder.decode(kv[1], "UTF-8")
                    else kv[0].lowercase() to ""
                }
            } else emptyMap()

            val secret = params["secret"] ?: return null
            var issuer = params["issuer"] ?: ""
            var accountName = label

            if (label.contains(":")) {
                val labelParts = label.split(":", limit = 2)
                if (issuer.isEmpty()) issuer = labelParts[0].trim()
                accountName = labelParts[1].trim()
            }

            val digits = params["digits"]?.toIntOrNull() ?: 6
            val period = params["period"]?.toLongOrNull() ?: 30L
            val algorithm = params["algorithm"] ?: "SHA1"

            TotpConfig(
                secret = secret,
                issuer = issuer,
                accountName = accountName,
                algorithm = algorithm,
                digits = digits,
                periodSeconds = period
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Base32 decoder compliant with RFC 4648.
     */
    fun decodeBase32(input: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = input.trim().replace("=", "").replace("-", "").uppercase()
        val out = mutableListOf<Byte>()

        var buffer = 0
        var bitsLeft = 0

        for (c in clean) {
            val value = base32Chars.indexOf(c)
            if (value < 0) continue // Skip invalid chars or whitespace

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }

        return out.toByteArray()
    }
}
