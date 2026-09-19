package org.openvault.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openvault.core.model.AutoLockTimeout
import org.openvault.core.model.ClipboardClearTimeout
import org.openvault.core.model.VaultSettings

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("openvault_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_LOCK = "setting_auto_lock_sec"
        private const val KEY_CLIPBOARD_CLEAR = "setting_clipboard_clear_sec"
        private const val KEY_SCREENSHOT_PROTECT = "setting_screenshot_protect"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<VaultSettings> = _settings.asStateFlow()

    private fun loadSettings(): VaultSettings {
        val autoLockSec = prefs.getLong(KEY_AUTO_LOCK, AutoLockTimeout.ONE_MINUTE.seconds)
        val clipSec = prefs.getLong(KEY_CLIPBOARD_CLEAR, ClipboardClearTimeout.THIRTY_SECONDS.seconds)
        val screenshot = prefs.getBoolean(KEY_SCREENSHOT_PROTECT, true)

        return VaultSettings(
            autoLockTimeout = AutoLockTimeout.fromSeconds(autoLockSec),
            clipboardClearTimeout = ClipboardClearTimeout.fromSeconds(clipSec),
            screenshotProtectionEnabled = screenshot
        )
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putLong(KEY_AUTO_LOCK, timeout.seconds).apply()
        _settings.value = _settings.value.copy(autoLockTimeout = timeout)
    }

    fun setClipboardClearTimeout(timeout: ClipboardClearTimeout) {
        prefs.edit().putLong(KEY_CLIPBOARD_CLEAR, timeout.seconds).apply()
        _settings.value = _settings.value.copy(clipboardClearTimeout = timeout)
    }

    fun setScreenshotProtection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECT, enabled).apply()
        _settings.value = _settings.value.copy(screenshotProtectionEnabled = enabled)
    }
}
