package com.example.jewels.presentation.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PremiumStatusChip(
    text: String,
    emphasized: Boolean,
    enabled: Boolean = true,          // ✅ NUEVO
    onClick: () -> Unit = {}          // ✅ NUEVO
) {

    val bg = if (emphasized)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)

    val border = if (emphasized)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

    Surface(
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() },   // ✅ AHORA ES BOTÓN
        color = bg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}