package org.openvault.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.openvault.core.crypto.TotpGenerator
import org.openvault.core.security.ClipboardHelper

@Composable
fun TotpCountdownBadge(
    totpSecret: String,
    modifier: Modifier = Modifier,
    periodSeconds: Long = 30L
) {
    val context = LocalContext.current
    var totpResult by remember(totpSecret) {
        mutableStateOf(TotpGenerator.generateToken(totpSecret, periodSeconds = periodSeconds))
    }

    LaunchedEffect(totpSecret) {
        while (true) {
            totpResult = TotpGenerator.generateToken(totpSecret, periodSeconds = periodSeconds)
            delay(1000L)
        }
    }

    // Format code: "123 456"
    val formattedCode = if (totpResult.code.length == 6) {
        "${totpResult.code.take(3)} ${totpResult.code.takeLast(3)}"
    } else {
        totpResult.code
    }

    val warningColor = if (totpResult.secondsRemaining <= 5) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable {
                ClipboardHelper.copyToClipboard(
                    context = context,
                    label = "2FA Code",
                    text = totpResult.code,
                    isSensitive = true,
                    timeoutSeconds = 30
                )
                Toast.makeText(context, "2FA Code copied (auto-clears in 30s)", Toast.LENGTH_SHORT).show()
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TWO-FACTOR CODE (TOTP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = formattedCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { totpResult.progress },
                        modifier = Modifier.size(36.dp),
                        color = warningColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "${totpResult.secondsRemaining}s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = warningColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy 2FA Code",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
