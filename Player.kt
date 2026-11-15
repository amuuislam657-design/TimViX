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
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.videotube.app.*
import com.videotube.app.data.VideoEntity
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PlayerScreen(vm: MainViewModel, nav: NavController, id: Long) {
    val videos by vm.videos.collectAsState()
    val v = videos.firstOrNull { it.id == id }
    if (v == null) {
        LaunchedEffect(videos.isNotEmpty()) { if (videos.isNotEmpty()) nav.popBackStack() }
        Box(Modifier.fillMaxSize().background(Bg))
    } else PlayerContent(v, vm, nav)
}

@Composable
private fun PlayerContent(v: VideoEntity, vm: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val activity = remember { ctx.findActivity() }
    val player = remember(v.id) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(v.videoPath))))
            prepare(); playWhenReady = true
        }
    }
    var playing by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(v.durationMs) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    var volume by remember { mutableStateOf(1f) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var speedMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying; if (isPlaying) started = true }
            override fun onPlaybackStateChanged(s: Int) {
                ended = s == Player.STATE_ENDED
                if (s == Player.STATE_READY && player.duration > 0) duration = player.duration
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(player) { while (true) { if (!scrubbing) position = player.currentPosition; delay(250) } }
    LaunchedEffect(controls, playing, scrubbing) { if (controls && playing && !scrubbing) { delay(3000); controls = false } }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) player.pause() }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }

    fun applySystemUi(full: Boolean) {
        val w = activity.window
        val c = WindowCompat.getInsetsController(w, w.decorView)
        if (full) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(fullscreen) { applySystemUi(fullscreen) }
    DisposableEffect(Unit) { onDispose { applySystemUi(false) } }
    BackHandler(fullscreen) { fullscreen = false }

    fun toggle() {
        if (ended) { player.seekTo(0); player.play() } else if (player.isPlaying) player.pause() else player.play()
    }
    val dur = duration.coerceAtLeast(1L)

    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(
            if (fullscreen) Modifier.fillMaxSize().background(Color.Black)
            else Modifier.fillMaxWidth().aspectRatio(16 / 9f).background(Color.Black)
        ) {
            AndroidView({ c -> (LayoutInflater.from(c).inflate(R.layout.video_texture_view, null, false) as PlayerView).also { it.player = player } }, Modifier.fillMaxSize())
            if (!started) AsyncImage(v.thumbPath?.let { File(it) }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { controls = !controls })
            if (controls) Box(Modifier.fillMaxSize().background(Color(0x66000000))) {
                Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ if (fullscreen) fullscreen = false else nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                    if (fullscreen) Text(v.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    Icon(if (volume == 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, "Volume", tint = Color.White)
                    Slider(volume, { volume = it; player.volume = it }, Modifier.width(100.dp), colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red, inactiveTrackColor = Color(0x66FFFFFF)))
                }
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) { Icon(Icons.Filled.Replay10, "Back 10s", tint = Color.White, modifier = Modifier.size(34.dp)) }
                    IconButton({ toggle() }, Modifier.size(56.dp)) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp)) }
                    IconButton({ player.seekTo((player.currentPosition + 10_000).coerceAtMost(dur)) }) { Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(34.dp)) }
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${formatDuration(if (scrubbing) scrubPos.toLong() else position)} / ${formatDuration(dur)}", color = Color.White, fontSize = 12.sp)
                    Slider(
                        if (scrubbing) scrubPos else position.toFloat().coerceIn(0f, dur.toFloat()),
                        { scrubbing = true; scrubPos = it },
                        Modifier.weight(1f).padding(horizontal = 4.dp),
                        valueRange = 0f..dur.toFloat(),
                        onValueChangeFinished = { player.seekTo(scrubPos.toLong()); position = scrubPos.toLong(); scrubbing = false },
                        colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red, inactiveTrackColor = Color(0x66FFFFFF))
                    )
                    Box {
                        TextButton({ speedMenu = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text((if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()) + "x", color = Color.White)
                        }
                        DropdownMenu(speedMenu, { speedMenu = false }) {
                            listOf(0.5f, 1f, 1.5f, 2f).forEach { s ->
                                DropdownMenuItem({ Text((if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "x", color = if (s == speed) Red else Color.Unspecified) },
                                    { speed = s; player.setPlaybackSpeed(s); speedMenu = false })
                            }
                        }
                    }
                    IconButton({ fullscreen = !fullscreen }) { Icon(if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White) }
                }
            }
        }
        if (!fullscreen) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(v.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${v.category} • ${relativeDate(v.createdAt)} • ${formatDuration(v.durationMs)}", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ nav.navigate("edit/${v.id}") }) { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                OutlinedButton({ confirmDelete = true }) { Icon(Icons.Filled.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Delete") }
            }
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                Text(v.description.ifBlank { "No description." }, Modifier.padding(14.dp), color = if (v.description.isBlank()) Muted else Color.White)
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete video?") }, text = { Text("This permanently removes the video from your device.") },
        confirmButton = { TextButton({ confirmDelete = false; nav.popBackStack(); vm.deleteVideo(v) }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } }
    )
}
