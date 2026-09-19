package org.openvault

import android.app.Application
import org.openvault.core.security.AutoLockManager
import org.openvault.data.repository.SettingsRepository
import org.openvault.data.repository.VaultRepository

class OpenVaultApplication : Application() {

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        vaultRepository = VaultRepository(this)
        settingsRepository = SettingsRepository(this)

        // Initialize AutoLockManager with application context and timeout
        AutoLockManager.initialize(this, settingsRepository.settings.value.autoLockTimeout)
    }
}
