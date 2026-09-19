package org.openvault.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openvault.core.model.AutoLockTimeout
import org.openvault.core.model.ClipboardClearTimeout
import org.openvault.core.security.ClipboardHelper
import org.openvault.ui.components.SecureField
import org.openvault.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onEnrollBiometric: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var autoLockExpanded by remember { mutableStateOf(false) }
    var clipboardExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var showPinDialog by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportedContent by remember { mutableStateOf<String?>(null) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }

    var showPlaintextWarningDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.operationSuccess) {
        uiState.operationSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.operationError) {
        uiState.operationError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Security & Locking
            SettingsGroup(title = "SECURITY & LOCKING") {
                // Auto Lock Timeout Dropdown
                ExposedDropdownMenuBox(
                    expanded = autoLockExpanded,
                    onExpandedChange = { autoLockExpanded = !autoLockExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.settings.autoLockTimeout.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Auto-Lock Timeout") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoLockExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = autoLockExpanded,
                        onDismissRequest = { autoLockExpanded = false }
                    ) {
                        AutoLockTimeout.entries.forEach { timeout ->
                            DropdownMenuItem(
                                text = { Text(timeout.label) },
                                onClick = {
                                    viewModel.updateAutoLock(timeout)
                                    autoLockExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clipboard Auto-Clear Dropdown
                ExposedDropdownMenuBox(
                    expanded = clipboardExpanded,
                    onExpandedChange = { clipboardExpanded = !clipboardExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.settings.clipboardClearTimeout.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Clipboard Auto-Clear") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clipboardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = clipboardExpanded,
                        onDismissRequest = { clipboardExpanded = false }
                    ) {
                        ClipboardClearTimeout.entries.forEach { timeout ->
                            DropdownMenuItem(
                                text = { Text(timeout.label) },
                                onClick = {
                                    viewModel.updateClipboardClear(timeout)
                                    clipboardExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Screenshot Protection Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Screenshot & Recents Protection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Blocks screen capture and hides app preview in task switcher",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.settings.screenshotProtectionEnabled,
                        onCheckedChange = { viewModel.toggleScreenshotProtection(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick PIN Unlock Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quick PIN Unlock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            if (uiState.isPinEnabled) "Active" else "Unlock vault with 4-8 digit numeric PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isPinEnabled) {
                        OutlinedButton(onClick = { viewModel.disablePin() }) {
                            Text("Disable PIN")
                        }
                    } else {
                        Button(onClick = { showPinDialog = true }) {
                            Text("Set Up PIN")
                        }
                    }
                }

                // Biometrics Unlock Row
                if (uiState.isBiometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "Hardware-backed fingerprint or facial recognition",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isBiometricEnabled) {
                            OutlinedButton(onClick = { viewModel.disableBiometric() }) {
                                Text("Disable")
                            }
                        } else {
                            Button(onClick = { onEnrollBiometric?.invoke() }) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }

            // Section 2: Backup & Sync
            SettingsGroup(title = "BACKUP & SYNC") {
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Encrypted Backup (.openvault)")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Backup or Bitwarden / CSV")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { showPlaintextWarningDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Plaintext CSV (Not Recommended)", color = MaterialTheme.colorScheme.error)
                }
            }

            // Section 3: Privacy & About
            SettingsGroup(title = "ABOUT & MISSION") {
                Text(
                    text = "OpenVault is a 100% free, non-profit privacy project. We believe cybersecurity is a basic human right.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showPrivacyDialog = true }) {
                        Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Privacy Policy")
                    }

                    TextButton(onClick = {
                        Toast.makeText(context, "OpenVault v1.0.0 • Apache 2.0 Open Source", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Version 1.0.0")
                    }
                }
            }
        }
    }

    // Set Up PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Up Quick PIN") },
            text = {
                Column {
                    Text("Enter a 4 to 8 digit numeric PIN. This PIN will be used to derive a hardware-protected quick unlock key.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    SecureField(
                        value = inputPin,
                        onValueChange = { if (it.all { c -> c.isDigit() }) inputPin = it },
                        label = "Numeric PIN",
                        showCopy = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enablePin(inputPin)
                        showPinDialog = false
                        inputPin = ""
                    },
                    enabled = inputPin.length >= 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportedContent = null
                exportPassword = ""
            },
            title = { Text("Export Encrypted Backup") },
            text = {
                Column {
                    if (exportedContent == null) {
                        Text("Enter a strong backup password to encrypt this file with PBKDF2 (600,000 rounds) and AES-256-GCM.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        SecureField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = "Backup Password",
                            showCopy = false
                        )
                    } else {
                        Text("Your encrypted backup is ready! Tap below to copy it to clipboard.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = exportedContent ?: "",
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (exportedContent == null) {
                    Button(
                        onClick = {
                            exportedContent = viewModel.exportEncryptedBackup(exportPassword)
                        },
                        enabled = exportPassword.length >= 8
                    ) {
                        Text("Generate Backup")
                    }
                } else {
                    Button(
                        onClick = {
                            exportedContent?.let {
                                ClipboardHelper.copyToClipboard(context, "OpenVault Backup", it, isSensitive = true, timeoutSeconds = 60)
                                Toast.makeText(context, "Encrypted backup copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            showExportDialog = false
                            exportedContent = null
                            exportPassword = ""
                        }
                    ) {
                        Text("Copy to Clipboard")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedContent = null
                    exportPassword = ""
                }) { Text("Close") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Credentials") },
            text = {
                Column {
                    Text("Paste an OpenVault encrypted backup JSON, a Bitwarden export JSON, or standard CSV text below:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Backup / JSON / CSV Text") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SecureField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = "Password (for .openvault files)",
                        showCopy = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = importText.trim()
                        if (trimmed.contains("\"openvault-backup\"")) {
                            viewModel.importBackup(trimmed, importPassword)
                        } else if (trimmed.contains("\"items\"") && trimmed.startsWith("{")) {
                            viewModel.importBitwardenJson(trimmed)
                        } else {
                            viewModel.importCsv(trimmed)
                        }
                        showImportDialog = false
                        importText = ""
                        importPassword = ""
                    },
                    enabled = importText.isNotBlank()
                ) {
                    Text("Import Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Plaintext CSV Warning Dialog
    if (showPlaintextWarningDialog) {
        AlertDialog(
            onDismissRequest = { showPlaintextWarningDialog = false },
            title = { Text("Security Warning: Plaintext Export") },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            text = {
                Text("Exporting passwords in plaintext exposes all your credentials unencrypted in device storage. Anyone or any app with file access will be able to read your passwords. Are you sure you want to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPlaintextWarningDialog = false
                        val csv = viewModel.exportPlaintextCsv()
                        ClipboardHelper.copyToClipboard(context, "Plaintext Passwords", csv, isSensitive = true, timeoutSeconds = 30)
                        Toast.makeText(context, "Plaintext CSV copied (auto-clears in 30s)", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text("I Understand, Export Plaintext", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaintextWarningDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("OpenVault Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "100% Zero-Knowledge & No-Tracking Charter:\n\n" +
                                "• OpenVault does not contain advertisements or promotional content.\n" +
                                "• OpenVault includes zero telemetry, zero analytics SDKs, and zero behavioral tracking.\n" +
                                "• No accounts, emails, or phone numbers are required.\n" +
                                "• Your credentials and encryption keys never leave your device in unencrypted form.\n" +
                                "• 100% Free and Open Source under Apache 2.0 license.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
