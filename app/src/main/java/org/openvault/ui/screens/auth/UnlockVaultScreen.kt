package org.openvault.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openvault.ui.components.SecureField
import org.openvault.ui.viewmodel.AuthViewModel

@Composable
fun UnlockVaultScreen(
    viewModel: AuthViewModel,
    onUnlockSuccess: () -> Unit,
    onTriggerBiometricPrompt: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputPassword by remember { mutableStateOf("") }
    var inputPin by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Vault Locked",
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OpenVault Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (uiState.usePinMode) "Enter your quick PIN to unlock" else "Enter your Master Password to decrypt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (uiState.usePinMode) {
                SecureField(
                    value = inputPin,
                    onValueChange = {
                        inputPin = it
                        viewModel.clearError()
                    },
                    label = "Quick PIN",
                    showCopy = false
                )
            } else {
                SecureField(
                    value = inputPassword,
                    onValueChange = {
                        inputPassword = it
                        viewModel.clearError()
                    },
                    label = "Master Password",
                    showCopy = false
                )
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (uiState.usePinMode) {
                        viewModel.unlockWithPin(inputPin, onUnlockSuccess)
                    } else {
                        viewModel.unlockWithPassword(inputPassword, onUnlockSuccess)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading && (if (uiState.usePinMode) inputPin.isNotEmpty() else inputPassword.isNotEmpty()),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Unlock Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isPinEnrolled) {
                    TextButton(onClick = { viewModel.togglePinMode() }) {
                        Icon(
                            imageVector = if (uiState.usePinMode) Icons.Default.Lock else Icons.Default.Pin,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (uiState.usePinMode) "Use Master Password" else "Use Quick PIN")
                    }
                }

                if (uiState.isBiometricEnrolled && onTriggerBiometricPrompt != null) {
                    Spacer(modifier = Modifier.size(12.dp))
                    OutlinedButton(
                        onClick = onTriggerBiometricPrompt,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Biometric")
                    }
                }
            }
        }
    }
}
