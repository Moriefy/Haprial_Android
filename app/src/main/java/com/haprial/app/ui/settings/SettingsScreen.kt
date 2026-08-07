package com.haprial.app.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.auth.AuthStateManager
import com.haprial.app.ui.theme.currentThemeMode
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableSaltApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit, vm: SettingsViewModel = koinViewModel(), authManager: AuthStateManager = koinInject()) {
    val stats by vm.stats.collectAsState()
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(onBack = {}, text = "设置", showBackBtn = false)

            LazyColumn {
                // Stats card
                item {
                    stats?.let { s ->
                        RoundedColumn {
                            Column(Modifier.padding(16.dp)) {
                                Text("博客统计", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf("${s.published}" to "已发布", "${s.drafts}" to "草稿", "${s.comments}" to "评论", "${s.friends}" to "友链").forEach { (v, l) ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(v, fontSize = 24.sp, color = SaltTheme.colors.highlight, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            Text(l, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Theme toggle
                item {
                    RoundedColumn {
                        Column(Modifier.padding(16.dp)) {
                            Text("主题", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, label) ->
                                    androidx.compose.material3.FilterChip(
                                        selected = themeMode == mode,
                                        onClick = {
                                            themeMode = mode
                                            prefs.edit().putString("theme", mode).apply()
                                            currentThemeMode.value = mode
                                        },
                                        label = { Text(label) },
                                        leadingIcon = if (themeMode == mode) {{
                                            Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Check), contentDescription = null, modifier = Modifier.size(16.dp))
                                        }} else null
                                    )
                                }
                            }
                        }
                    }
                }

                // GitHub sync
                item {
                    RoundedColumn {
                        Item(
                            onClick = {},
                            text = "GitHub 同步",
                            sub = "文章操作自动同步到 GitHub",
                            arrowType = ItemArrowType.None
                        )
                    }
                }

                // Logout button
                item {
                    RoundedColumn {
                        ItemButton(
                            onClick = {
                                authManager.logout()
                                onLogout()
                            },
                            text = "退出登录",
                            primary = true,
                            iconPainter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Logout)
                        )
                    }
                }
            }
        }
    }
}

// Use Material3 LazyColumn since SaltUI doesn't have one
@Composable
private fun LazyColumn(content: LazyListScope.() -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}
