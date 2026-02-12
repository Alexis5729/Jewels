package com.example.jewels.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.jewels.R
import androidx.compose.foundation.Image

@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "alpha"
    )

    LaunchedEffect(true) {
        startAnimation = true
        delay(2000) // ⏳ duración del splash
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.naolux), // 👈 usa tu logo real
                contentDescription = "Naolux Logo",
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Cargando experiencia...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
