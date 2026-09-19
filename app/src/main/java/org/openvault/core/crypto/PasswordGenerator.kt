package org.openvault.core.crypto

import org.openvault.core.model.PasswordOptions
import org.openvault.core.model.PasswordStrength
import org.openvault.core.model.StrengthRating
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    private const val AMBIGUOUS_CHARS = "O0Il1|`'\"~"

    private val secureRandom = SecureRandom()

    // Bundled curated Diceware wordlist for memorable passphrases
    private val DICEWARE_WORDLIST = listOf(
        "abide", "about", "above", "absent", "absorb", "abstract", "accent", "accept", "access", "accord",
        "account", "across", "action", "active", "actual", "adapt", "address", "admire", "admit", "adopt",
        "advance", "advice", "affair", "afford", "afraid", "after", "again", "against", "agency", "agent",
        "agree", "ahead", "airline", "airport", "alarm", "album", "alert", "alien", "alive", "alleged",
        "allow", "almost", "alone", "along", "already", "also", "alter", "always", "amazing", "amber",
        "among", "amount", "anchor", "ancient", "angel", "anger", "angle", "animal", "ankle", "announce",
        "annual", "answer", "anthem", "antique", "anxiety", "anyway", "apart", "apology", "appear", "apple",
        "apply", "appoint", "approve", "arch", "arctic", "arena", "argue", "arise", "armor", "around",
        "arrange", "arrest", "arrive", "arrow", "artery", "artist", "ascend", "ashamed", "aspect", "assault",
        "assert", "assess", "asset", "assign", "assist", "assume", "assure", "astonish", "athlete", "atlas",
        "atom", "attach", "attack", "attain", "attempt", "attend", "attract", "auction", "audit", "august",
        "author", "auto", "autumn", "avail", "avenue", "average", "avoid", "awake", "award", "aware",
        "awesome", "awkward", "axis", "baby", "bachelor", "bacon", "badge", "baggage", "baker", "balance",
        "balcony", "ballad", "ballet", "balloon", "bamboo", "banana", "banner", "barber", "bargain", "barrel",
        "barrier", "base", "basic", "basket", "battle", "beach", "beacon", "bean", "beauty", "become",
        "before", "begin", "behave", "behind", "belief", "belong", "below", "bench", "benefit", "berry",
        "beside", "betray", "better", "between", "beyond", "bicycle", "bid", "biscuit", "bishop", "bitter",
        "bizarre", "blanket", "blast", "blaze", "blend", "bless", "blind", "block", "blossom", "board",
        "boast", "body", "boiler", "bold", "bomb", "bond", "bone", "bonus", "book", "boost",
        "border", "borrow", "bother", "bottle", "bottom", "bounce", "boundary", "bow", "brace", "brain",
        "branch", "brand", "brave", "bread", "break", "breast", "breath", "breeze", "brick", "bridge",
        "brief", "bright", "brilliant", "bring", "brisk", "broad", "broker", "bronze", "brother", "brown",
        "bubble", "budget", "buffer", "build", "bulb", "bulk", "bundle", "burden", "bureau", "burn",
        "burst", "bus", "bush", "business", "busy", "cabin", "cable", "cactus", "cage", "calculate",
        "calendar", "calm", "camera", "camp", "campus", "canal", "cancel", "candle", "cannon", "canvas",
        "capable", "capital", "captain", "capture", "carbon", "card", "career", "careful", "cargo", "carpet",
        "carrier", "carrot", "carry", "cart", "cascade", "case", "cash", "casino", "castle", "casual",
        "catalog", "catch", "cater", "cattle", "cause", "caution", "cave", "cease", "ceiling", "celebrate",
        "cell", "cement", "census", "center", "century", "cereal", "certain", "chain", "chair", "chalk",
        "chamber", "champion", "chance", "change", "channel", "chaos", "chapter", "charge", "charity", "charm",
        "chart", "chase", "cheap", "check", "cheese", "cherry", "chest", "chief", "child", "chimney",
        "choice", "choose", "chronic", "church", "cider", "cigar", "cinema", "circle", "circuit", "citizen",
        "city", "civil", "claim", "clap", "clarify", "classic", "clean", "clear", "clever", "client",
        "cliff", "climate", "climb", "clinic", "clock", "clone", "close", "cloth", "cloud", "clover",
        "cluster", "coach", "coast", "cobalt", "code", "coffee", "cognac", "coin", "cold", "collar",
        "colleague", "collect", "college", "colony", "color", "column", "combat", "combine", "comfort", "command"
    )

    /**
     * Generates a password or passphrase according to PasswordOptions.
     */
    fun generate(options: PasswordOptions): String {
        return if (options.isPassphrase) {
            generatePassphrase(options.wordCount, options.separator)
        } else {
            generateRandomPassword(options)
        }
    }

    private fun generateRandomPassword(options: PasswordOptions): String {
        var charPool = buildString {
            if (options.includeUppercase) append(UPPERCASE)
            if (options.includeLowercase) append(LOWERCASE)
            if (options.includeDigits) append(DIGITS)
            if (options.includeSymbols) append(SYMBOLS)
        }

        if (options.avoidAmbiguous) {
            charPool = charPool.filter { it !in AMBIGUOUS_CHARS }
        }

        if (charPool.isEmpty()) {
            charPool = LOWERCASE + DIGITS
        }

        val requiredChars = mutableListOf<Char>()
        if (options.includeUppercase) {
            val pool = if (options.avoidAmbiguous) UPPERCASE.filter { it !in AMBIGUOUS_CHARS } else UPPERCASE
            if (pool.isNotEmpty()) requiredChars.add(pool[secureRandom.nextInt(pool.length)])
        }
        if (options.includeLowercase) {
            val pool = if (options.avoidAmbiguous) LOWERCASE.filter { it !in AMBIGUOUS_CHARS } else LOWERCASE
            if (pool.isNotEmpty()) requiredChars.add(pool[secureRandom.nextInt(pool.length)])
        }
        if (options.includeDigits) {
            val pool = if (options.avoidAmbiguous) DIGITS.filter { it !in AMBIGUOUS_CHARS } else DIGITS
            if (pool.isNotEmpty()) requiredChars.add(pool[secureRandom.nextInt(pool.length)])
        }
        if (options.includeSymbols) {
            val pool = if (options.avoidAmbiguous) SYMBOLS.filter { it !in AMBIGUOUS_CHARS } else SYMBOLS
            if (pool.isNotEmpty()) requiredChars.add(pool[secureRandom.nextInt(pool.length)])
        }

        val targetLength = options.length.coerceAtLeast(requiredChars.size)
        val passwordChars = CharArray(targetLength)

        // Fill remaining slots
        for (i in 0 until (targetLength - requiredChars.size)) {
            passwordChars[i] = charPool[secureRandom.nextInt(charPool.length)]
        }

        // Add guaranteed character classes
        for (i in requiredChars.indices) {
            passwordChars[targetLength - requiredChars.size + i] = requiredChars[i]
        }

        // Fisher-Yates shuffle using SecureRandom
        for (i in passwordChars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return String(passwordChars)
    }

    private fun generatePassphrase(wordCount: Int, separator: String): String {
        val count = wordCount.coerceIn(3, 10)
        val selectedWords = (0 until count).map {
            DICEWARE_WORDLIST[secureRandom.nextInt(DICEWARE_WORDLIST.size)]
                .replaceFirstChar { it.uppercase() }
        }
        return selectedWords.joinToString(separator)
    }

    /**
     * Calculates Shannon entropy and estimates crack time.
     */
    fun calculateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(0.0, StrengthRating.VERY_WEAK, "Instant")
        }

        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) poolSize = 10

        val entropyBits = password.length * log2(poolSize.toDouble())

        val rating = when {
            entropyBits < 30 -> StrengthRating.VERY_WEAK
            entropyBits < 50 -> StrengthRating.WEAK
            entropyBits < 70 -> StrengthRating.FAIR
            entropyBits < 90 -> StrengthRating.STRONG
            else -> StrengthRating.VERY_STRONG
        }

        val crackTime = estimateCrackTime(entropyBits)

        val suggestions = mutableListOf<String>()
        if (password.length < 12) suggestions.add("Increase length to at least 14 characters")
        if (!password.any { it.isUpperCase() }) suggestions.add("Add uppercase letters")
        if (!password.any { it.isDigit() }) suggestions.add("Add numbers")
        if (!password.any { !it.isLetterOrDigit() }) suggestions.add("Add symbols")

        return PasswordStrength(
            entropyBits = entropyBits,
            rating = rating,
            estimatedCrackTime = crackTime,
            suggestions = suggestions
        )
    }

    private fun estimateCrackTime(entropyBits: Double): String {
        // Assume 10 billion (10^10) guesses per second (high-end GPU cluster)
        val guessesPerSec = 10.0.pow(10)
        val combinations = 2.0.pow(entropyBits.coerceAtMost(128.0))
        val seconds = (combinations / 2.0) / guessesPerSec

        return when {
            seconds < 1 -> "Instant"
            seconds < 60 -> "${seconds.toInt()} seconds"
            seconds < 3600 -> "${(seconds / 60).toInt()} minutes"
            seconds < 86400 -> "${(seconds / 3600).toInt()} hours"
            seconds < 31536000 -> "${(seconds / 86400).toInt()} days"
            seconds < 3153600000 -> "${(seconds / 31536000).toInt()} years"
            seconds < 3153600000000 -> "${(seconds / 31536000000).toInt()} thousand years"
            else -> "Centuries"
        }
    }
}
