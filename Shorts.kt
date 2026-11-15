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
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.videotube.app.*
import com.videotube.app.data.*
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ShortsScreen(vm: MainViewModel, nav: NavController, startId: Long) {
    val videos by vm.videos.collectAsState()
    val profile by vm.profile.collectAsState()
    val shorts = videos.filter { it.isShort }
    if (shorts.isEmpty()) Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        EmptyState("No Shorts yet\nUpload a video and switch on “Upload as Short”")
    } else ShortsPager(shorts, profile, startId)
}

@Composable
private fun ShortsPager(shorts: List<VideoEntity>, profile: ProfileEntity, startId: Long) {
    val pager = rememberPagerState(initialPage = shorts.indexOfFirst { it.id == startId }.coerceAtLeast(0)) { shorts.size }
    var muted by rememberSaveable { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) resumed = false else if (e == Lifecycle.Event.ON_RESUME) resumed = true
        }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    VerticalPager(pager, Modifier.fillMaxSize().background(Color.Black), key = { shorts[it].id }) { page ->
        ShortPage(shorts[page], profile, pager.currentPage == page && resumed, muted) { muted = !muted }
    }
}

@Composable
private fun ShortPage(v: VideoEntity, creator: ProfileEntity, active: Boolean, muted: Boolean, onMute: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember(v.id) {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(v.videoPath))))
            prepare()
        }
    }
    var shown by remember(v.id) { mutableStateOf(false) }
    var paused by remember(v.id) { mutableStateOf(false) }
    var progress by remember(v.id) { mutableStateOf(0f) }

    DisposableEffect(player) {
        val l = object : Player.Listener { override fun onRenderedFirstFrame() { shown = true } }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(active, paused) {
        if (!active) { player.seekTo(0); paused = false }
        player.playWhenReady = active && !paused
    }
    LaunchedEffect(muted, player) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(active) {
        while (active) {
            val d = player.duration
            progress = if (d > 0) (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(150)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView({ c -> (LayoutInflater.from(c).inflate(R.layout.video_texture_view, null, false) as PlayerView).also { it.player = player } }, Modifier.fillMaxSize())
        if (!shown) Thumb(v.thumbPath, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { paused = !paused })
        if (paused) Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.Center).size(88.dp))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        Text("Shorts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 80.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(creator.avatarPath?.let { File(it) }, 38.dp)
                Spacer(Modifier.width(10.dp))
                Text("@${creator.username}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(v.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onMute, Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 16.dp)) {
            Icon(if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, "Mute", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        LinearProgressIndicator(progress, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), color = Red, trackColor = Color(0x44FFFFFF))
    }
}
