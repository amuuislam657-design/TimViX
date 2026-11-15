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
import androidx.navigation.NavController
import com.videotube.app.*
import java.io.File

@Composable
fun HomeScreen(vm: MainViewModel, nav: NavController) {
    val videos by vm.videos.collectAsState()
    val profile by vm.profile.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("All") }
    val shorts = videos.filter { it.isShort }
    val cats = listOf("All") + videos.map { it.category }.filter { it.isNotBlank() }.distinct()
    val activeCat = if (cat in cats) cat else "All"
    val list = videos.filter { !it.isShort && (activeCat == "All" || it.category == activeCat) && it.matches(query) }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Spacer(Modifier.weight(1f))
            Avatar(profile.avatarPath?.let { File(it) }, 38.dp, Modifier.clickable { nav.navigate("profile") })
        }
        SearchField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        LazyRow(contentPadding = PaddingValues(16.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats) { c -> FilterChip(activeCat == c, { cat = c }, { Text(c) }, colors = chipColors()) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (shorts.isNotEmpty() && query.isBlank() && activeCat == "All") item {
                Column(Modifier.padding(bottom = 8.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlashOn, null, tint = Red)
                        Spacer(Modifier.width(6.dp))
                        Text("Shorts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shorts, key = { it.id }) { s ->
                            VideoTile(s, 9 / 16f, { nav.navigate("shorts/${s.id}") }, Modifier.width(120.dp))
                        }
                    }
                }
            }
            items(list, key = { it.id }) { v ->
                VideoCard(
                    v, { nav.navigate("player/${v.id}") }, { nav.navigate("edit/${v.id}") }, { vm.deleteVideo(v) },
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (list.isEmpty() && (shorts.isEmpty() || query.isNotBlank() || activeCat != "All")) item {
                EmptyState(if (videos.isEmpty()) "No videos yet\nTap + to upload your first video" else "No videos found")
            }
        }
    }
}

@Composable
fun LibraryScreen(vm: MainViewModel, nav: NavController) {
    val videos by vm.videos.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var newest by rememberSaveable { mutableStateOf(true) }
    val list = videos.filter { it.isShort == (tab == 1) && it.matches(query) }
        .let { if (newest) it.sortedByDescending { v -> v.createdAt } else it.sortedBy { v -> v.createdAt } }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 8.dp))
        TabRow(tab, containerColor = Bg, contentColor = Red) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Videos (${videos.count { !it.isShort }})") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Shorts (${videos.count { it.isShort }})") })
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchField(query, { query = it }, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FilterChip(true, { newest = !newest }, { Text(if (newest) "Newest" else "Oldest") },
                leadingIcon = { Icon(Icons.Filled.Sort, null, Modifier.size(18.dp)) }, colors = chipColors())
        }
        if (list.isEmpty()) EmptyState(if (query.isBlank()) "Nothing here yet" else "No results")
        else if (tab == 0) LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(list, key = { it.id }) { v ->
                VideoCard(
                    v, { nav.navigate("player/${v.id}") }, { nav.navigate("edit/${v.id}") }, { vm.deleteVideo(v) },
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.weight(1f), contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { it.id }) { v ->
                VideoTile(v, 9 / 16f, { nav.navigate("shorts/${v.id}") }, onEdit = { nav.navigate("edit/${v.id}") }, onDelete = { vm.deleteVideo(v) })
            }
        }
    }
}
