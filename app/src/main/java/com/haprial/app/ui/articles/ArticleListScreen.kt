package com.haprial.app.ui.articles

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.model.Article
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.YesNoDialog
import android.widget.Toast
import org.koin.androidx.compose.koinViewModel

private const val PAGE_SIZE = 10

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, UnstableSaltApi::class)
@Composable
fun ArticleListScreen(onArticleClick: (Int) -> Unit, onNewArticle: () -> Unit, vm: ArticleListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var currentPage by remember { mutableIntStateOf(0) }

    val filteredArticles = remember(state.articles, searchQuery, statusFilter) {
        state.articles.filter { a ->
            (searchQuery.isBlank() || a.title.contains(searchQuery, ignoreCase = true)) &&
            (statusFilter == "all" || a.status == statusFilter)
        }
    }
    val totalPages = ((filteredArticles.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
    val pagedArticles = remember(filteredArticles, currentPage) {
        filteredArticles.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    }
    // 筛选变化时回到第一页
    LaunchedEffect(searchQuery, statusFilter) { currentPage = 0 }

    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.clearError() }
    }

    Surface(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            // ═══ 标题栏 ═══
            StandardTitleBar(title = "文章") {
                TitleBarButton(onClick = onNewArticle) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Add), contentDescription = "新文章")
                }
            }

            // ═══ 搜索 + 筛选 ═══
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(SaltTheme.colors.subBackground).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text("搜索文章…", color = SaltTheme.colors.subText.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SaltTheme.colors.text),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 筛选按钮
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("all" to "全部", "published" to "已发布", "draft" to "草稿").forEach { (v, l) ->
                        val selected = statusFilter == v
                        Box(
                            Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) SaltTheme.colors.highlight else SaltTheme.colors.subBackground).clickable { statusFilter = v }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(l, fontSize = 12.sp, color = if (selected) Color.White else SaltTheme.colors.subText, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }

            // ═══ 文章列表 ═══
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                filteredArticles.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Article), contentDescription = null, modifier = Modifier.size(48.dp), tint = SaltTheme.colors.subText.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(if (searchQuery.isBlank()) "还没有文章" else "没有匹配的文章", color = SaltTheme.colors.subText, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        items(pagedArticles, key = { it.id }) { article ->
                            ArticleCard(article, onClick = { onArticleClick(article.id) }, onTogglePublish = { vm.togglePublish(article.id) }, onDelete = { vm.deleteArticle(article.id) })
                        }
                    }
                    // ═══ 分页控件 ═══
                    if (totalPages > 1) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0, modifier = Modifier.size(36.dp)) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.KeyboardArrowLeft), contentDescription = "上一页", modifier = Modifier.size(20.dp))
                            }
                            Text("${currentPage + 1} / $totalPages", fontSize = 13.sp, color = SaltTheme.colors.subText, modifier = Modifier.padding(horizontal = 16.dp))
                            IconButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1, modifier = Modifier.size(36.dp)) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.KeyboardArrowRight), contentDescription = "下一页", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit, onTogglePublish: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        YesNoDialog(onDismissRequest = { showDeleteDialog = false }, onConfirm = { showDeleteDialog = false; onDelete() }, title = "删除文章", content = "确定删除「${article.title}」？")
    }

    RoundedColumn(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(article.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(article.date, fontSize = 12.sp, color = SaltTheme.colors.subText)
                    Text(article.category, fontSize = 12.sp, color = SaltTheme.colors.subText)
                }
                if (article.excerpt.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(article.excerpt, fontSize = 13.sp, color = SaltTheme.colors.subText.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val statusColor = if (article.status == "published") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    Box(Modifier.height(20.dp).clip(RoundedCornerShape(4.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        Text(if (article.status == "published") "已发布" else "草稿", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Medium)
                    }
                    if (article.pinned == 1) {
                        Box(Modifier.height(20.dp).clip(RoundedCornerShape(4.dp)).background(SaltTheme.colors.highlight.copy(alpha = 0.1f)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            Text("📌 置顶", fontSize = 10.sp, color = SaltTheme.colors.highlight)
                        }
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MoreVert), contentDescription = null, modifier = Modifier.size(18.dp), tint = SaltTheme.colors.subText)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(if (article.status == "published") "下架" else "发布") }, onClick = { showMenu = false; onTogglePublish() }, leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(if (article.status == "published") Icons.Default.Unpublished else Icons.Default.Publish), contentDescription = null) })
                    DropdownMenuItem(text = { Text("删除", color = Color(0xFFE53935)) }, onClick = { showMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = null, tint = Color(0xFFE53935)) })
                }
            }
        }
    }
}
