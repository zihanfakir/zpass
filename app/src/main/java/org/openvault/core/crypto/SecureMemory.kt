package org.openvault.core.crypto

import java.util.Arrays

object SecureMemory {
    /**
     * Overwrites all bytes in the array with zeroes to prevent memory scraping.
     */
    fun zeroize(buffer: ByteArray) {
        Arrays.fill(buffer, 0.toByte())
    }

    /**
     * Overwrites all characters in the array with null chars.
     */
    fun zeroize(buffer: CharArray) {
        Arrays.fill(buffer, '\u0000')
    }
}

fun ByteArray.zeroize() {
    SecureMemory.zeroize(this)
}

fun CharArray.zeroize() {
    SecureMemory.zeroize(this)
}
