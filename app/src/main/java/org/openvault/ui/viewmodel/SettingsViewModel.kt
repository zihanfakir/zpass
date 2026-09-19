package org.openvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openvault.core.crypto.KeystoreManager
import org.openvault.core.crypto.zeroize
import org.openvault.core.model.AutoLockTimeout
import org.openvault.core.model.ClipboardClearTimeout
import org.openvault.core.model.VaultSettings
import org.openvault.data.importexport.VaultExporter
import org.openvault.data.importexport.VaultImporter
import org.openvault.data.repository.SettingsRepository
import org.openvault.data.repository.VaultRepository
import javax.crypto.SecretKey

data class SettingsUiState(
    val settings: VaultSettings = VaultSettings(),
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isPinEnabled: Boolean = false,
    val operationSuccess: String? = null,
    val operationError: String? = null
)

class SettingsViewModel(
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            settings = settingsRepository.settings.value,
            isBiometricAvailable = KeystoreManager.isKeystoreAvailable(),
            isBiometricEnabled = vaultRepository.isBiometricEnabled(),
            isPinEnabled = vaultRepository.isPinEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.value = _uiState.value.copy(settings = s)
            }
        }
    }

    fun updateAutoLock(timeout: AutoLockTimeout) {
        settingsRepository.setAutoLockTimeout(timeout)
    }

    fun updateClipboardClear(timeout: ClipboardClearTimeout) {
        settingsRepository.setClipboardClearTimeout(timeout)
    }

    fun toggleScreenshotProtection(enabled: Boolean) {
        settingsRepository.setScreenshotProtection(enabled)
    }

    fun enablePin(pin: String) {
        if (pin.length < 4) {
            _uiState.value = _uiState.value.copy(operationError = "PIN must be at least 4 digits")
            return
        }
        viewModelScope.launch {
            val chars = pin.toCharArray()
            try {
                vaultRepository.enablePin(chars)
                _uiState.value = _uiState.value.copy(
                    isPinEnabled = true,
                    operationSuccess = "PIN unlock successfully enabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operationError = "Failed to enable PIN: ${e.message}")
            } finally {
                chars.zeroize()
            }
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            vaultRepository.disablePin()
            _uiState.value = _uiState.value.copy(
                isPinEnabled = false,
                operationSuccess = "PIN unlock disabled"
            )
        }
    }

    fun enableBiometric(secretKey: SecretKey) {
        viewModelScope.launch {
            try {
                vaultRepository.enableBiometric(secretKey)
                _uiState.value = _uiState.value.copy(
                    isBiometricEnabled = true,
                    operationSuccess = "Biometric unlock enabled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operationError = "Biometric setup failed: ${e.message}")
            }
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            vaultRepository.disableBiometric()
            _uiState.value = _uiState.value.copy(
                isBiometricEnabled = false,
                operationSuccess = "Biometric unlock disabled"
            )
        }
    }

    fun exportEncryptedBackup(backupPassword: String): String? {
        if (backupPassword.length < 8) {
            _uiState.value = _uiState.value.copy(operationError = "Backup password must be at least 8 characters")
            return null
        }

        val chars = backupPassword.toCharArray()
        return try {
            val items = vaultRepository.itemsFlow.value
            val categories = vaultRepository.categoriesFlow.value
            val exportJson = VaultExporter.exportEncryptedBackup(items, categories, chars)
            _uiState.value = _uiState.value.copy(operationSuccess = "Encrypted backup created successfully")
            exportJson
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(operationError = "Export failed: ${e.message}")
            null
        } finally {
            chars.zeroize()
        }
    }

    fun exportPlaintextCsv(): String {
        val items = vaultRepository.itemsFlow.value
        return VaultExporter.exportPlaintextCsv(items)
    }

    fun importBackup(jsonString: String, password: String) {
        val chars = password.toCharArray()
        viewModelScope.launch {
            try {
                val data = VaultImporter.importEncryptedBackup(jsonString, chars)
                for (item in data.items) {
                    vaultRepository.saveItem(item)
                }
                _uiState.value = _uiState.value.copy(operationSuccess = "Successfully imported ${data.items.size} items")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operationError = "Import failed: incorrect password or invalid file")
            } finally {
                chars.zeroize()
            }
        }
    }

    fun importBitwardenJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val items = VaultImporter.importBitwardenJson(jsonString)
                for (item in items) {
                    vaultRepository.saveItem(item)
                }
                _uiState.value = _uiState.value.copy(operationSuccess = "Imported ${items.size} items from Bitwarden export")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operationError = "Failed to parse Bitwarden JSON: ${e.message}")
            }
        }
    }

    fun importCsv(csvString: String) {
        viewModelScope.launch {
            try {
                val items = VaultImporter.importCsv(csvString)
                for (item in items) {
                    vaultRepository.saveItem(item)
                }
                _uiState.value = _uiState.value.copy(operationSuccess = "Imported ${items.size} items from CSV")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(operationError = "CSV import error: ${e.message}")
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(operationSuccess = null, operationError = null)
    }
}
