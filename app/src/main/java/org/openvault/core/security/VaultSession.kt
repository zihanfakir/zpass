package org.openvault.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openvault.core.crypto.zeroize
import org.openvault.core.model.VaultLockState

object VaultSession {

    private val _lockState = MutableStateFlow<VaultLockState>(VaultLockState.SetupRequired)
    val lockState: StateFlow<VaultLockState> = _lockState.asStateFlow()

    private var activeVaultKey: ByteArray? = null

    val isUnlocked: Boolean
        get() = _lockState.value is VaultLockState.Unlocked && activeVaultKey != null

    fun setInitialState(hasVault: Boolean) {
        if (!isUnlocked) {
            _lockState.value = if (hasVault) VaultLockState.Locked else VaultLockState.SetupRequired
        }
    }

    /**
     * Unlocks the vault with the decrypted 256-bit Symmetric Vault Key.
     */
    fun unlock(vaultKey: ByteArray) {
        require(vaultKey.size == 32) { "Vault key must be 32 bytes" }
        // Copy key into internal buffer
        activeVaultKey?.zeroize()
        activeVaultKey = vaultKey.clone()
        _lockState.value = VaultLockState.Unlocked
    }

    /**
     * Immediately zeroizes the vault key from RAM and transitions state to Locked.
     */
    fun lock() {
        activeVaultKey?.zeroize()
        activeVaultKey = null
        _lockState.value = VaultLockState.Locked
    }

    /**
     * Retrieves the active vault key or throws IllegalStateException if vault is locked.
     */
    fun requireVaultKey(): ByteArray {
        val key = activeVaultKey
        check(key != null && _lockState.value is VaultLockState.Unlocked) {
            "Vault is currently locked"
        }
        return key
    }
}
