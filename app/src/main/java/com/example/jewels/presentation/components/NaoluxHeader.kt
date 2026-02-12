package com.example.jewels.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.jewels.R

@Composable
fun NaoluxHeader(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.naolux),
            contentDescription = "Logo Naolux",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)

        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Naolux", style = MaterialTheme.typography.titleMedium)
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }
}
