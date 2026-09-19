package org.openvault

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.openvault.core.crypto.KeystoreManager
import org.openvault.ui.screens.navigation.OpenVaultApp
import org.openvault.ui.theme.OpenVaultTheme
import org.openvault.ui.viewmodel.AuthViewModel
import org.openvault.ui.viewmodel.GeneratorViewModel
import org.openvault.ui.viewmodel.SettingsViewModel
import org.openvault.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var vaultViewModel: VaultViewModel
    private lateinit var generatorViewModel: GeneratorViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OpenVaultApplication
        val vaultRepo = app.vaultRepository
        val settingsRepo = app.settingsRepository

        authViewModel = AuthViewModel(vaultRepo)
        vaultViewModel = VaultViewModel(vaultRepo)
        generatorViewModel = GeneratorViewModel()
        settingsViewModel = SettingsViewModel(vaultRepo, settingsRepo)

        // Observe screenshot protection setting to apply FLAG_SECURE
        lifecycleScope.launch {
            settingsRepo.settings.collect { settings ->
                if (settings.screenshotProtectionEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            OpenVaultTheme {
                OpenVaultApp(
                    authViewModel = authViewModel,
                    vaultViewModel = vaultViewModel,
                    generatorViewModel = generatorViewModel,
                    settingsViewModel = settingsViewModel,
                    onTriggerBiometricPrompt = { showBiometricUnlockPrompt() },
                    onEnrollBiometric = { enrollBiometricKey() }
                )
            }
        }
    }

    private fun showBiometricUnlockPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val secretKey = KeystoreManager.getOrCreateBiometricKey(requireUserAuth = false)
                    authViewModel.unlockWithBiometricKey(secretKey) {}
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Biometric decryption failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@MainActivity, "Biometric: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock OpenVault")
            .setSubtitle("Authenticate to decrypt your vault")
            .setNegativeButtonText("Use Password")
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun enrollBiometricKey() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val secretKey = KeystoreManager.getOrCreateBiometricKey(requireUserAuth = false)
                    settingsViewModel.enableBiometric(secretKey)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Biometric enrollment error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@MainActivity, "Enrollment cancelled: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Unlock")
            .setSubtitle("Confirm your fingerprint or face to bind vault key")
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(promptInfo)
    }
}
