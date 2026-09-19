package org.openvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openvault.core.crypto.PasswordGenerator
import org.openvault.core.model.StrengthRating

@Composable
fun PasswordStrengthMeter(
    password: CharSequence,
    modifier: Modifier = Modifier
) {
    val strength = PasswordGenerator.calculateStrength(password.toString())

    val barColor by animateColorAsState(
        targetValue = when (strength.rating) {
            StrengthRating.VERY_WEAK -> MaterialTheme.colorScheme.error
            StrengthRating.WEAK -> Color(0xFFEF4444)
            StrengthRating.FAIR -> Color(0xFFF59E0B)
            StrengthRating.STRONG -> Color(0xFF10B981)
            StrengthRating.VERY_STRONG -> Color(0xFF059669)
        },
        label = "StrengthColor"
    )

    val progressFraction by animateFloatAsState(
        targetValue = when (strength.rating) {
            StrengthRating.VERY_WEAK -> 0.2f
            StrengthRating.WEAK -> 0.4f
            StrengthRating.FAIR -> 0.6f
            StrengthRating.STRONG -> 0.8f
            StrengthRating.VERY_STRONG -> 1.0f
        },
        label = "StrengthProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength: ${strength.rating.label}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
            Text(
                text = "Crack time: ${strength.estimatedCrackTime}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress segments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (step in 1..5) {
                val isActive = progressFraction >= (step * 0.2f - 0.05f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isActive) barColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                )
            }
        }

        if (strength.suggestions.isNotEmpty() && password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tip: " + strength.suggestions.first(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
