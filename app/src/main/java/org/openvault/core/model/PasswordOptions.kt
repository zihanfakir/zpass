package org.openvault.core.model

data class PasswordOptions(
    val length: Int = 18,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val avoidAmbiguous: Boolean = true,
    val isPassphrase: Boolean = false,
    val wordCount: Int = 4,
    val separator: String = "-"
)

enum class StrengthRating(val score: Int, val label: String) {
    VERY_WEAK(0, "Very Weak"),
    WEAK(1, "Weak"),
    FAIR(2, "Fair"),
    STRONG(3, "Strong"),
    VERY_STRONG(4, "Very Strong")
}

data class PasswordStrength(
    val entropyBits: Double,
    val rating: StrengthRating,
    val estimatedCrackTime: String,
    val suggestions: List<String> = emptyList()
)
