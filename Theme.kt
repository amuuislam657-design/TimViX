package com.videotube.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

val Bg = Color(0xFF0B0B0B)
val Card = Color(0xFF1A1A1A)
val CardHigh = Color(0xFF262626)
val Red = Color(0xFFFF0033)
val Muted = Color(0xFFAAAAAA)

@Composable
fun VideoTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red, onPrimary = Color.White, background = Bg, onBackground = Color.White,
            surface = Card, onSurface = Color.White, surfaceVariant = CardHigh, onSurfaceVariant = Muted
        ),
        content = content
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Card, unfocusedContainerColor = Card,
    focusedBorderColor = Red, unfocusedBorderColor = Color(0xFF3A3A3A),
    cursorColor = Red, focusedLabelColor = Red
)

@Composable
fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = CardHigh, labelColor = Color.White,
    selectedContainerColor = Red, selectedLabelColor = Color.White
)
