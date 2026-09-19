package org.openvault.ui.screens.audit

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openvault.core.model.VaultItem
import org.openvault.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val auditReport = remember { viewModel.getAuditReport() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Security Health") },
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
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val isHealthy = auditReport.weakItems.isEmpty() && auditReport.reusedItems.isEmpty()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isHealthy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isHealthy) "Vault Security is Strong" else "Security Improvements Needed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${auditReport.totalItems} total vault credentials analyzed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Weak Passwords Section
            AuditSection(
                title = "Weak Passwords (${auditReport.weakItems.size})",
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                items = auditReport.weakItems,
                emptyMessage = "No weak passwords found! All passwords meet strong entropy criteria.",
                onItemClick = onNavigateToDetail
            )

            // Reused Passwords Section
            AuditSection(
                title = "Reused Passwords (${auditReport.reusedItems.size})",
                icon = Icons.Default.Warning,
                iconColor = Color(0xFFF59E0B),
                items = auditReport.reusedItems,
                emptyMessage = "No reused passwords found! Every credential uses a unique secret.",
                onItemClick = onNavigateToDetail
            )

            // Missing 2FA Section
            AuditSection(
                title = "Missing Two-Factor 2FA (${auditReport.missing2faLogins.size})",
                icon = Icons.Default.LockClock,
                iconColor = MaterialTheme.colorScheme.primary,
                items = auditReport.missing2faLogins,
                emptyMessage = "All logins have 2FA configured or no eligible accounts exist.",
                onItemClick = onNavigateToDetail
            )
        }
    }
}

@Composable
fun AuditSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    items: List<VaultItem>,
    emptyMessage: String,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (items.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (item in items.take(10)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (item.username.isNotBlank()) item.username else item.type.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (items.size > 10) {
                        Text(
                            text = "+ ${items.size - 10} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
