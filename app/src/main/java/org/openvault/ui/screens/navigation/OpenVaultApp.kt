package org.openvault.ui.screens.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.openvault.core.model.VaultLockState
import org.openvault.core.security.VaultSession
import org.openvault.ui.screens.audit.SecurityAuditScreen
import org.openvault.ui.screens.auth.SetupVaultScreen
import org.openvault.ui.screens.auth.UnlockVaultScreen
import org.openvault.ui.screens.generator.PasswordGeneratorScreen
import org.openvault.ui.screens.settings.SettingsScreen
import org.openvault.ui.screens.vault.VaultHomeScreen
import org.openvault.ui.screens.vault.VaultItemDetailScreen
import org.openvault.ui.screens.vault.VaultItemEditScreen
import org.openvault.ui.viewmodel.AuthViewModel
import org.openvault.ui.viewmodel.GeneratorViewModel
import org.openvault.ui.viewmodel.SettingsViewModel
import org.openvault.ui.viewmodel.VaultViewModel

sealed interface Screen {
    data object Home : Screen
    data class Detail(val itemId: String) : Screen
    data class Edit(val itemId: String?) : Screen
    data object Generator : Screen
    data object Audit : Screen
    data object Settings : Screen
}

@Composable
fun OpenVaultApp(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel,
    generatorViewModel: GeneratorViewModel,
    settingsViewModel: SettingsViewModel,
    onTriggerBiometricPrompt: (() -> Unit)? = null,
    onEnrollBiometric: (() -> Unit)? = null
) {
    val lockState by VaultSession.lockState.collectAsState()
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (lockState) {
            is VaultLockState.SetupRequired -> {
                SetupVaultScreen(
                    viewModel = authViewModel,
                    onSetupSuccess = {
                        // VaultSession becomes Unlocked on setup
                    }
                )
            }
            is VaultLockState.Locked -> {
                // Clear back stack on lock
                if (backStack.size > 1 || backStack.firstOrNull() != Screen.Home) {
                    backStack.clear()
                    backStack.add(Screen.Home)
                }

                UnlockVaultScreen(
                    viewModel = authViewModel,
                    onUnlockSuccess = {
                        // Handled reactively by lockState -> Unlocked
                    },
                    onTriggerBiometricPrompt = onTriggerBiometricPrompt
                )
            }
            is VaultLockState.Unlocked -> {
                val currentScreen = backStack.lastOrNull() ?: Screen.Home

                // Back navigation handling
                if (backStack.size > 1) {
                    BackHandler {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }

                when (currentScreen) {
                    is Screen.Home -> {
                        VaultHomeScreen(
                            viewModel = vaultViewModel,
                            onNavigateToDetail = { backStack.add(Screen.Detail(it)) },
                            onNavigateToAdd = { backStack.add(Screen.Edit(null)) },
                            onNavigateToGenerator = { backStack.add(Screen.Generator) },
                            onNavigateToAudit = { backStack.add(Screen.Audit) },
                            onNavigateToSettings = { backStack.add(Screen.Settings) }
                        )
                    }
                    is Screen.Detail -> {
                        VaultItemDetailScreen(
                            itemId = currentScreen.itemId,
                            viewModel = vaultViewModel,
                            onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                            onNavigateToEdit = { backStack.add(Screen.Edit(it)) }
                        )
                    }
                    is Screen.Edit -> {
                        VaultItemEditScreen(
                            itemId = currentScreen.itemId,
                            viewModel = vaultViewModel,
                            onNavigateBack = { backStack.removeAt(backStack.lastIndex) }
                        )
                    }
                    is Screen.Generator -> {
                        PasswordGeneratorScreen(
                            viewModel = generatorViewModel,
                            onNavigateBack = { backStack.removeAt(backStack.lastIndex) }
                        )
                    }
                    is Screen.Audit -> {
                        SecurityAuditScreen(
                            viewModel = vaultViewModel,
                            onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                            onNavigateToDetail = { backStack.add(Screen.Detail(it)) }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                            onEnrollBiometric = onEnrollBiometric
                        )
                    }
                }
            }
        }
    }
}
