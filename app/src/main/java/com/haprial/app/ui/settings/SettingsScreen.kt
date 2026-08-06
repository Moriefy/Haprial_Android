package com.haprial.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haprial.app.ui.theme.ThemeMode
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit, vm: SettingsViewModel = koinViewModel()) {
    val stats by vm.stats.collectAsState()
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            stats?.let { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("博客统计", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("${s.published}" to "已发布", "${s.drafts}" to "草稿", "${s.comments}" to "评论", "${s.friends}" to "友链").forEach { (v, l) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(v, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // Theme toggle
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("主题", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    prefs.edit().putString("theme", mode).apply()
                                },
                                label = { Text(label) },
                                leadingIcon = if (themeMode == mode) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) { ListItem({ Text("GitHub 同步") }, supportingContent = { Text("文章操作自动同步到 GitHub") }, leadingContent = { Icon(Icons.Default.Sync, null) }) }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { ctx.getSharedPreferences("haprial_auth", 0).edit().clear().apply(); onLogout() }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("退出登录")
            }
        }
    }
}
