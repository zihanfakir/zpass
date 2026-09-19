package org.openvault.ui.screens.vault

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openvault.core.crypto.PasswordGenerator
import org.openvault.core.crypto.TotpGenerator
import org.openvault.core.model.CustomField
import org.openvault.core.model.FieldType
import org.openvault.core.model.ItemType
import org.openvault.core.model.PasswordOptions
import org.openvault.core.model.VaultItem
import org.openvault.ui.components.PasswordStrengthMeter
import org.openvault.ui.components.SecureField
import org.openvault.ui.viewmodel.VaultViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemEditScreen(
    itemId: String?,
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val existingItem = remember(itemId) { items.find { it.id == itemId } }

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(existingItem?.categoryId ?: "logins") }
    var username by remember { mutableStateOf(existingItem?.username ?: "") }
    var password by remember { mutableStateOf(existingItem?.password ?: "") }
    var url by remember { mutableStateOf(existingItem?.url ?: "") }
    var notes by remember { mutableStateOf(existingItem?.notes ?: "") }
    var totpInput by remember { mutableStateOf(existingItem?.totpSecret ?: "") }

    val customFields = remember {
        mutableStateListOf<CustomField>().apply {
            existingItem?.customFields?.let { addAll(it) }
        }
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingItem != null) "Edit Item" else "New Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                // Check if user entered an otpauth URI
                                val cleanTotp = if (totpInput.startsWith("otpauth://", ignoreCase = true)) {
                                    TotpGenerator.parseOtpAuthUri(totpInput)?.secret ?: totpInput
                                } else {
                                    totpInput
                                }

                                val itemToSave = (existingItem ?: VaultItem(
                                    id = UUID.randomUUID().toString(),
                                    title = title
                                )).copy(
                                    title = title,
                                    categoryId = selectedCategoryId,
                                    type = when (selectedCategoryId) {
                                        "notes" -> ItemType.SECURE_NOTE
                                        "cards" -> ItemType.CARD
                                        "identities" -> ItemType.IDENTITY
                                        else -> ItemType.LOGIN
                                    },
                                    username = username,
                                    password = password,
                                    url = url,
                                    notes = notes,
                                    totpSecret = cleanTotp,
                                    customFields = customFields.toList(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.saveItem(itemToSave)
                                onNavigateBack()
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Item", tint = MaterialTheme.colorScheme.primary)
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
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                val selectedCatName = categories.find { it.id == selectedCategoryId }?.name ?: "Logins"
                OutlinedTextField(
                    value = selectedCatName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username / Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Password with inline generator action
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        password = PasswordGenerator.generate(PasswordOptions(length = 20))
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate Strong")
                    }
                }
                SecureField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    showCopy = false
                )
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthMeter(password = password)
                }
            }

            // TOTP Secret
            OutlinedTextField(
                value = totpInput,
                onValueChange = { totpInput = it },
                label = { Text("2FA / TOTP Secret Key or otpauth:// URI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("Base32 secret (e.g. JBSWY3DPEHPK3PXP) or otpauth:// link")
                }
            )

            // Website URL
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Website URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Secure Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Secure Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )

            // Custom Fields Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM FIELDS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = {
                                customFields.add(CustomField(name = "Field ${customFields.size + 1}", value = "", type = FieldType.TEXT))
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Text")
                            }
                            TextButton(onClick = {
                                customFields.add(CustomField(name = "Secret ${customFields.size + 1}", value = "", type = FieldType.CONCEALED))
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Secret")
                            }
                        }
                    }

                    for (i in customFields.indices) {
                        val field = customFields[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = field.name,
                                onValueChange = { customFields[i] = field.copy(name = it) },
                                label = { Text("Name") },
                                modifier = Modifier.weight(0.4f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = field.value,
                                onValueChange = { customFields[i] = field.copy(value = it) },
                                label = { Text(if (field.type == FieldType.CONCEALED) "Secret" else "Value") },
                                modifier = Modifier.weight(0.6f),
                                singleLine = true
                            )
                            IconButton(onClick = { customFields.removeAt(i) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove field", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cleanTotp = if (totpInput.startsWith("otpauth://", ignoreCase = true)) {
                            TotpGenerator.parseOtpAuthUri(totpInput)?.secret ?: totpInput
                        } else {
                            totpInput
                        }

                        val itemToSave = (existingItem ?: VaultItem(
                            id = UUID.randomUUID().toString(),
                            title = title
                        )).copy(
                            title = title,
                            categoryId = selectedCategoryId,
                            type = when (selectedCategoryId) {
                                "notes" -> ItemType.SECURE_NOTE
                                "cards" -> ItemType.CARD
                                "identities" -> ItemType.IDENTITY
                                else -> ItemType.LOGIN
                            },
                            username = username,
                            password = password,
                            url = url,
                            notes = notes,
                            totpSecret = cleanTotp,
                            customFields = customFields.toList(),
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.saveItem(itemToSave)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Save Vault Item", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
