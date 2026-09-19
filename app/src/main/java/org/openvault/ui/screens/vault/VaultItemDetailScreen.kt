package org.openvault.ui.screens.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openvault.core.model.FieldType
import org.openvault.core.model.VaultItem
import org.openvault.core.security.ClipboardHelper
import org.openvault.ui.components.PasswordStrengthMeter
import org.openvault.ui.components.SecureField
import org.openvault.ui.components.TotpCountdownBadge
import org.openvault.ui.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemDetailScreen(
    itemId: String,
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsState()
    val item = items.find { it.id == itemId }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (item == null) {
        onNavigateBack()
        return
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(item.id) }) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onNavigateToEdit(item.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error
                        )
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
            // TOTP Authenticator section (if configured)
            if (item.hasTotp) {
                TotpCountdownBadge(totpSecret = item.totpSecret)
            }

            // Username
            if (item.username.isNotBlank()) {
                DetailCard(
                    label = "USERNAME / EMAIL",
                    value = item.username,
                    onCopy = {
                        ClipboardHelper.copyToClipboard(context, "Username", item.username, isSensitive = false)
                        Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Password
            if (item.password.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PASSWORD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SecureField(
                            value = item.password,
                            onValueChange = {},
                            label = "Password",
                            readOnly = true,
                            showCopy = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordStrengthMeter(password = item.password)
                    }
                }
            }

            // Website URL
            if (item.url.isNotBlank()) {
                DetailCard(
                    label = "WEBSITE URL",
                    value = item.url,
                    onCopy = {
                        ClipboardHelper.copyToClipboard(context, "URL", item.url, isSensitive = false)
                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Secure Notes
            if (item.notes.isNotBlank()) {
                DetailCard(
                    label = "SECURE NOTES",
                    value = item.notes,
                    onCopy = {
                        ClipboardHelper.copyToClipboard(context, "Notes", item.notes, isSensitive = true)
                        Toast.makeText(context, "Notes copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Custom Fields
            if (item.customFields.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "CUSTOM FIELDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        for (field in item.customFields) {
                            if (field.type == FieldType.CONCEALED) {
                                SecureField(
                                    value = field.value,
                                    onValueChange = {},
                                    label = field.name,
                                    readOnly = true,
                                    showCopy = true
                                )
                            } else {
                                DetailCard(
                                    label = field.name.uppercase(),
                                    value = field.value,
                                    onCopy = {
                                        ClipboardHelper.copyToClipboard(context, field.name, field.value, isSensitive = false)
                                        Toast.makeText(context, "${field.name} copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Metadata footer
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = "Created: ${dateFormatter.format(Date(item.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Last updated: ${dateFormatter.format(Date(item.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vault Item?") },
            text = { Text("Are you sure you want to delete '${item.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailCard(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
