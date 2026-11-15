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
import coil.compose.AsyncImage
import com.videotube.app.*
import com.videotube.app.data.VideoEntity
import java.io.File

@Composable
fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp, 24.dp).clip(RoundedCornerShape(7.dp)).background(Red), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("VideoTube", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
    }
}

@Composable
fun Thumb(path: String?, modifier: Modifier = Modifier) {
    Box(modifier.background(CardHigh), contentAlignment = Alignment.Center) {
        if (path != null) AsyncImage(File(path), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Filled.PlayCircle, null, tint = Color(0xFF666666), modifier = Modifier.size(40.dp))
    }
}

@Composable
fun Avatar(model: Any?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(CardHigh), contentAlignment = Alignment.Center) {
        if (model != null) AsyncImage(model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Filled.Person, null, tint = Muted, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun DurationBadge(ms: Long, modifier: Modifier = Modifier) {
    Text(
        formatDuration(ms), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xCC000000)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, onChange, modifier, singleLine = true, shape = RoundedCornerShape(28.dp),
        placeholder = { Text("Search by title or category") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = { if (value.isNotEmpty()) IconButton({ onChange("") }) { Icon(Icons.Filled.Close, "Clear") } },
        colors = fieldColors()
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun VideoActions(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, tint: Color = Muted) {
    var open by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton({ open = true }) { Icon(Icons.Filled.MoreVert, "More", tint = tint) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Edit") }, { open = false; onEdit() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
            DropdownMenuItem({ Text("Delete") }, { open = false; confirm = true }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete video?") },
        text = { Text("This permanently removes the video from your device.") },
        confirmButton = { TextButton({ confirm = false; onDelete() }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } }
    )
}

@Composable
fun VideoCard(v: VideoEntity, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Box {
            Thumb(v.thumbPath, Modifier.fillMaxWidth().aspectRatio(16 / 9f))
            DurationBadge(v.durationMs, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(v.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${v.category} • ${relativeDate(v.createdAt)}", color = Muted, fontSize = 13.sp)
            }
            VideoActions(onEdit, onDelete)
        }
    }
}

@Composable
fun VideoTile(
    v: VideoEntity, aspect: Float, onClick: () -> Unit, modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null
) {
    Box(modifier.aspectRatio(aspect).clip(RoundedCornerShape(12.dp)).background(Card).clickable(onClick = onClick)) {
        Thumb(v.thumbPath, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xCC000000))))
        Text(
            v.title, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
        if (onEdit != null && onDelete != null)
            VideoActions(onEdit, onDelete, Modifier.align(Alignment.TopEnd), tint = Color.White)
    }
}
