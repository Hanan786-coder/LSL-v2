package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashDone: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val rule = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
        delay(220)
        launch { title.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        delay(200)
        rule.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(450)
        onSplashDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Lsl.Night),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.lsl_logo),
                contentDescription = "Loco Street League crest",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * logo.value
                        scaleX = s; scaleY = s
                        alpha = logo.value.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Loco Street League",
                fontFamily = Scoreboard,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                color = Lsl.Chalk,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 24f
                }
            )
            Spacer(Modifier.height(10.dp))
            // A chalk line being drawn on the wall
            Box(
                Modifier
                    .width(120.dp)
                    .height(4.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(rule.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Lsl.Volt)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Street cricket, scored ball by ball",
                style = MaterialTheme.typography.bodyMedium,
                color = Lsl.Mist,
                modifier = Modifier.graphicsLayer { alpha = rule.value }
            )
        }
    }
}
