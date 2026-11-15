package com.videotube.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.videotube.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoTubeTheme { App() } }
    }
}

private data class Tab(val key: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("shorts", "Shorts", Icons.Filled.FlashOn),
    Tab("upload", "Upload", Icons.Filled.Add),
    Tab("library", "Library", Icons.Filled.VideoLibrary),
    Tab("profile", "Profile", Icons.Filled.Person)
)

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val key = entry?.destination?.route?.substringBefore("/")
    val showBar = key in tabs.map { it.key }

    val err by vm.error.collectAsState()
    LaunchedEffect(err) { err?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.error.value = null } }

    val longArg = { n: String -> navArgument(n) { type = NavType.LongType; defaultValue = -1L } }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = Color(0xFF111111)) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = key == t.key,
                        onClick = {
                            nav.navigate(if (t.key == "shorts") "shorts/-1" else t.key) {
                                popUpTo("home"); launchSingleTop = true
                            }
                        },
                        icon = {
                            if (t.key == "upload") Box(Modifier.size(38.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) {
                                Icon(t.icon, t.label, tint = Color.White)
                            } else Icon(t.icon, t.label)
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Red, selectedTextColor = Red,
                            unselectedIconColor = Muted, unselectedTextColor = Muted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, nav) }
            composable("shorts/{start}", listOf(longArg("start"))) { ShortsScreen(vm, nav, it.arguments?.getLong("start") ?: -1L) }
            composable("upload") { VideoFormScreen(vm, nav, null) }
            composable("library") { LibraryScreen(vm, nav) }
            composable("profile") { ProfileScreen(vm, nav) }
            composable("player/{id}", listOf(longArg("id"))) { PlayerScreen(vm, nav, it.arguments?.getLong("id") ?: -1L) }
            composable("edit/{id}", listOf(longArg("id"))) { VideoFormScreen(vm, nav, it.arguments?.getLong("id")) }
        }
    }
}
