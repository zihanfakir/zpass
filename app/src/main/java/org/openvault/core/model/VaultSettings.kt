package org.openvault.core.model

sealed interface VaultLockState {
    data object SetupRequired : VaultLockState
    data object Locked : VaultLockState
    data object Unlocked : VaultLockState
}

enum class AutoLockTimeout(val seconds: Long, val label: String) {
    IMMEDIATE(0, "Immediate (on background)"),
    THIRTY_SECONDS(30, "30 seconds"),
    ONE_MINUTE(60, "1 minute"),
    FIVE_MINUTES(300, "5 minutes"),
    FIFTEEN_MINUTES(900, "15 minutes"),
    NEVER(-1, "Never (Not recommended)");

    companion object {
        fun fromSeconds(sec: Long): AutoLockTimeout =
            entries.find { it.seconds == sec } ?: ONE_MINUTE
    }
}

enum class ClipboardClearTimeout(val seconds: Long, val label: String) {
    THIRTY_SECONDS(30, "30 seconds"),
    SIXTY_SECONDS(60, "60 seconds"),
    TWO_MINUTES(120, "2 minutes"),
    NEVER(0, "Never (Insecure)");

    companion object {
        fun fromSeconds(sec: Long): ClipboardClearTimeout =
            entries.find { it.seconds == sec } ?: THIRTY_SECONDS
    }
}

data class VaultSettings(
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.ONE_MINUTE,
    val clipboardClearTimeout: ClipboardClearTimeout = ClipboardClearTimeout.THIRTY_SECONDS,
    val biometricEnabled: Boolean = false,
    val pinUnlockEnabled: Boolean = false,
    val screenshotProtectionEnabled: Boolean = true,
    val kdfIterations: Int = 600_000
)
