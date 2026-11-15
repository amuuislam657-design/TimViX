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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import com.videotube.app.*
import com.videotube.app.data.ProfileEntity
import java.io.File

@Composable
fun ProfileScreen(vm: MainViewModel, nav: NavController) {
    val videos by vm.videos.collectAsState()
    val profile by vm.profile.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val vids = videos.filter { !it.isShort }
    val shorts = videos.filter { it.isShort }
    val list = if (tab == 0) vids else shorts

    LazyVerticalGrid(
        GridCells.Fixed(3), Modifier.fillMaxSize().background(Bg), contentPadding = PaddingValues(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(profile.avatarPath?.let { File(it) }, 96.dp)
                Spacer(Modifier.height(12.dp))
                Text("@${profile.username}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (profile.bio.isNotBlank()) Text(profile.bio, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    Stat(vids.size, "Videos"); Stat(shorts.size, "Shorts"); Stat(videos.size, "Total")
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton({ editing = true }) { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit profile") }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TabRow(tab, containerColor = Bg, contentColor = Red) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Videos") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Shorts") })
            }
        }
        if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("Nothing uploaded yet") }
        items(list, key = { it.id }) { v ->
            VideoTile(
                v, if (v.isShort) 9 / 16f else 1f,
                { nav.navigate(if (v.isShort) "shorts/${v.id}" else "player/${v.id}") },
                onEdit = { nav.navigate("edit/${v.id}") }, onDelete = { vm.deleteVideo(v) }
            )
        }
    }
    if (editing) EditProfileDialog(profile, { editing = false }) { n, b, u -> vm.saveProfile(n, b, u); editing = false }
}

@Composable
private fun Stat(n: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EditProfileDialog(p: ProfileEntity, onDismiss: () -> Unit, onSave: (String, String, Uri?) -> Unit) {
    var name by remember { mutableStateOf(p.username) }
    var bio by remember { mutableStateOf(p.bio) }
    var pic by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) pic = it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(pic ?: p.avatarPath?.let { File(it) }, 84.dp, Modifier.clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                TextButton({ pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Change picture", color = Red) }
                OutlinedTextField(name, { name = it }, label = { Text("Username") }, singleLine = true, colors = fieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(bio, { bio = it }, label = { Text("About you") }, minLines = 2, colors = fieldColors())
            }
        },
        confirmButton = { TextButton({ onSave(name, bio, pic) }, enabled = name.isNotBlank()) { Text("Save", color = Red) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
