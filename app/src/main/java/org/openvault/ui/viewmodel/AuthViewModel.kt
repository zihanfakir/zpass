package org.openvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openvault.core.crypto.KeystoreManager
import org.openvault.core.crypto.zeroize
import org.openvault.core.security.VaultSession
import org.openvault.data.repository.VaultRepository
import javax.crypto.SecretKey

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnrolled: Boolean = false,
    val isPinEnrolled: Boolean = false,
    val usePinMode: Boolean = false
)

class AuthViewModel(private val repository: VaultRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isBiometricAvailable = KeystoreManager.isKeystoreAvailable(),
            isBiometricEnrolled = repository.isBiometricEnabled(),
            isPinEnrolled = repository.isPinEnabled()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun togglePinMode() {
        _uiState.value = _uiState.value.copy(
            usePinMode = !_uiState.value.usePinMode,
            error = null
        )
    }

    fun setupMasterPassword(password: String, confirm: String, onSuccess: () -> Unit) {
        if (password.length < 10) {
            _uiState.value = _uiState.value.copy(error = "Master password must be at least 10 characters")
            return
        }
        if (password != confirm) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val chars = password.toCharArray()
            try {
                repository.createVault(chars)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to create vault: ${e.message}")
            } finally {
                chars.zeroize()
            }
        }
    }

    fun unlockWithPassword(password: String, onSuccess: () -> Unit) {
        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your master password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val chars = password.toCharArray()
            try {
                val success = repository.unlockWithMasterPassword(chars)
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (success) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Incorrect master password")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Decryption error")
            } finally {
                chars.zeroize()
            }
        }
    }

    fun unlockWithPin(pin: String, onSuccess: () -> Unit) {
        if (pin.length < 4) {
            _uiState.value = _uiState.value.copy(error = "PIN must be at least 4 digits")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val chars = pin.toCharArray()
            try {
                val success = repository.unlockWithPin(chars)
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (success) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Incorrect PIN")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "PIN unlock failed")
            } finally {
                chars.zeroize()
            }
        }
    }

    fun unlockWithBiometricKey(secretKey: SecretKey, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val success = repository.unlockWithBiometric(secretKey)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (success) {
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = "Biometric unlock failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
