package org.openvault.ui.screens.generator

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openvault.core.security.ClipboardHelper
import org.openvault.ui.components.PasswordStrengthMeter
import org.openvault.ui.viewmodel.GeneratorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    viewModel: GeneratorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val options by viewModel.options.collectAsState()
    val generatedPassword by viewModel.generatedPassword.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator") },
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
            // Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = generatedPassword,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordStrengthMeter(password = generatedPassword)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { viewModel.regenerate() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            ClipboardHelper.copyToClipboard(
                                context = context,
                                label = "Generated Password",
                                text = generatedPassword,
                                isSensitive = true,
                                timeoutSeconds = 30
                            )
                            Toast.makeText(context, "Copied to clipboard (auto-clears in 30s)", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !options.isPassphrase,
                    onClick = { viewModel.setPassphraseMode(false) },
                    label = { Text("Random Password") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = options.isPassphrase,
                    onClick = { viewModel.setPassphraseMode(true) },
                    label = { Text("Diceware Passphrase") },
                    shape = RoundedCornerShape(8.dp)
                )
            }

            if (!options.isPassphrase) {
                // Random Password Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Length: ${options.length}", fontWeight = FontWeight.SemiBold)
                        }

                        Slider(
                            value = options.length.toFloat(),
                            onValueChange = { viewModel.updateLength(it.toInt()) },
                            valueRange = 8f..64f,
                            steps = 55
                        )

                        GeneratorSwitchRow(
                            title = "Uppercase Letters (A-Z)",
                            checked = options.includeUppercase,
                            onCheckedChange = { viewModel.toggleUppercase(it) }
                        )

                        GeneratorSwitchRow(
                            title = "Lowercase Letters (a-z)",
                            checked = options.includeLowercase,
                            onCheckedChange = { viewModel.toggleLowercase(it) }
                        )

                        GeneratorSwitchRow(
                            title = "Numbers (0-9)",
                            checked = options.includeDigits,
                            onCheckedChange = { viewModel.toggleDigits(it) }
                        )

                        GeneratorSwitchRow(
                            title = "Special Symbols (!@#$)",
                            checked = options.includeSymbols,
                            onCheckedChange = { viewModel.toggleSymbols(it) }
                        )

                        GeneratorSwitchRow(
                            title = "Avoid Ambiguous Characters (0/O, 1/l/I)",
                            checked = options.avoidAmbiguous,
                            onCheckedChange = { viewModel.toggleAvoidAmbiguous(it) }
                        )
                    }
                }
            } else {
                // Passphrase Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Word Count: ${options.wordCount}", fontWeight = FontWeight.SemiBold)
                        }

                        Slider(
                            value = options.wordCount.toFloat(),
                            onValueChange = { viewModel.updateWordCount(it.toInt()) },
                            valueRange = 3f..10f,
                            steps = 6
                        )

                        OutlinedTextField(
                            value = options.separator,
                            onValueChange = { viewModel.setSeparator(it) },
                            label = { Text("Word Separator (e.g. -, _, .)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Button(
                onClick = {
                    ClipboardHelper.copyToClipboard(
                        context = context,
                        label = "Generated Password",
                        text = generatedPassword,
                        isSensitive = true,
                        timeoutSeconds = 30
                    )
                    Toast.makeText(context, "Password copied to clipboard (auto-clears in 30s)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Generated Password", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun GeneratorSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
