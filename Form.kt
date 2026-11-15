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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.videotube.app.*
import java.io.File

@Composable
fun VideoFormScreen(vm: MainViewModel, nav: NavController, editId: Long?) {
    val ctx = LocalContext.current
    val videos by vm.videos.collectAsState()
    val saving by vm.saving.collectAsState()
    val editing = editId?.let { id -> videos.firstOrNull { it.id == id } }

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf<String?>(null) }
    var thumbUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember(editing?.id) { mutableStateOf(editing?.title ?: "") }
    var desc by remember(editing?.id) { mutableStateOf(editing?.description ?: "") }
    var category by remember(editing?.id) { mutableStateOf(editing?.category ?: "") }
    var isShort by remember(editing?.id) { mutableStateOf(editing?.isShort ?: false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun onVideo(uri: Uri?) {
        if (uri == null) return
        videoUri = uri
        videoName = displayName(ctx, uri)
        if (title.isBlank()) title = videoName?.substringBeforeLast('.') ?: ""
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { onVideo(it) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { onVideo(it) }
    val pickThumb = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) thumbUri = it }

    val cats = (DEFAULT_CATEGORIES + videos.map { it.category }).filter { it.isNotBlank() }.distinct()
    val canSave = !saving && (editing != null || videoUri != null)

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (editId != null) Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text("Edit video", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        } else Text("Upload video", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        if (editing == null) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
                Column(Modifier.padding(14.dp)) {
                    if (videoUri != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Red)
                        Spacer(Modifier.width(8.dp))
                        Text(videoName ?: "Video selected", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else Text("Select a video from your phone", color = Muted)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button({ pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                            Icon(Icons.Filled.VideoLibrary, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Gallery")
                        }
                        OutlinedButton({ openFile.launch(arrayOf("video/*")) }) {
                            Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Files")
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Text("Thumbnail", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(160.dp).aspectRatio(16 / 9f).clip(RoundedCornerShape(12.dp)).background(CardHigh), contentAlignment = Alignment.Center) {
                val m: Any? = thumbUri ?: editing?.thumbPath?.let { File(it) }
                if (m != null) AsyncImage(m, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Filled.PhotoLibrary, null, tint = Muted)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                OutlinedButton({ pickThumb.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (editing != null) "Change thumbnail" else "Choose thumbnail")
                }
                if (editing == null) Text("Optional – a frame is used otherwise", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true, colors = fieldColors())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 3, colors = fieldColors())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true, colors = fieldColors())
        LazyRow(contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats) { c -> FilterChip(category == c, { category = c }, { Text(c) }, colors = chipColors()) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Upload as Short", fontWeight = FontWeight.SemiBold)
                Text("Shows in the vertical Shorts feed", color = Muted, fontSize = 12.sp)
            }
            Switch(isShort, { isShort = it }, colors = SwitchDefaults.colors(checkedTrackColor = Red))
        }
        if (saving) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp), color = Red)
            Text("Copying video to app storage… large files can take a moment.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                val e = editing
                if (e != null) vm.updateVideo(e, title, desc, category, isShort, thumbUri) {
                    Toast.makeText(ctx, "Changes saved", Toast.LENGTH_SHORT).show(); nav.popBackStack()
                } else videoUri?.let { u ->
                    vm.addVideo(u, title, desc, category, thumbUri, isShort) {
                        Toast.makeText(ctx, "Video saved", Toast.LENGTH_SHORT).show()
                        videoUri = null; videoName = null; thumbUri = null; title = ""; desc = ""; category = ""; isShort = false
                        nav.navigate("home") { popUpTo("home"); launchSingleTop = true }
                    }
                }
            },
            enabled = canSave, modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = CardHigh)
        ) { Text(if (editing != null) "Save changes" else "Save video", fontWeight = FontWeight.Bold) }
        if (editing != null) {
            TextButton({ confirmDelete = true }, Modifier.fillMaxWidth()) { Text("Delete video", color = Red) }
        }
    }
    if (confirmDelete && editing != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete video?") }, text = { Text("This permanently removes the video from your device.") },
        confirmButton = { TextButton({ confirmDelete = false; nav.popBackStack(); vm.deleteVideo(editing) }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } }
    )
}
