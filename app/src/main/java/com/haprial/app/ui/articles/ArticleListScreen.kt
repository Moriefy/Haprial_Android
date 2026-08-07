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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.model.Article
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, UnstableSaltApi::class)
@Composable
fun ArticleListScreen(onArticleClick: (Int) -> Unit, onNewArticle: () -> Unit, vm: ArticleListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // 筛选后的文章
    val filteredArticles = remember(state.articles, searchQuery, statusFilter) {
        state.articles.filter { a ->
            (searchQuery.isBlank() || a.title.contains(searchQuery, ignoreCase = true) || a.content.orEmpty().contains(searchQuery, ignoreCase = true)) &&
            (statusFilter == "all" || a.status == statusFilter)
        }
    }

    // Toast 错误
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearError()
        }
    }

    // ── 筛选底部弹窗 ──
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("筛选", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                // 状态
                Text("状态", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "全部", "published" to "已发布", "draft" to "草稿").forEach { (v, l) ->
                        FilterChip(selected = statusFilter == v, onClick = { statusFilter = v }, label = { Text(l) })
                    }
                }
                // 分类
                val categories = remember(state.articles) { state.articles.map { it.category }.distinct().sorted() }
                if (categories.isNotEmpty()) {
                    Text("分类", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { cat ->
                            FilterChip(selected = false, onClick = { /* 跳转到分类筛选 */ }, label = { Text(cat) })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Surface(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            // ── 标题栏 ──
            StandardTitleBar(title = "文章") {
                // 新文章
                TitleBarButton(onClick = onNewArticle) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Add), contentDescription = "新文章")
                }
            }

            // ── 搜索栏 ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索文章…", style = SaltTheme.textStyles.sub) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true,
                    textStyle = SaltTheme.textStyles.main.copy(fontSize = 14.sp),
                    leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Search), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Clear), contentDescription = "清除", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                // 筛选按钮
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SaltTheme.colors.subBackground).clickable { showFilterSheet = true }, contentAlignment = Alignment.Center) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.FilterList), contentDescription = "筛选", modifier = Modifier.size(22.dp), tint = if (statusFilter != "all") SaltTheme.colors.highlight else SaltTheme.colors.text)
                }
            }

            // ── 状态标签 ──
            if (statusFilter != "all") {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { statusFilter = "all" }, label = { Text(if (statusFilter == "published") "已发布" else "草稿") })
                }
            }

            // ── 文章列表 ──
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                filteredArticles.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Article), contentDescription = null, modifier = Modifier.size(48.dp), tint = SaltTheme.colors.subText.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))
                        Text(if (searchQuery.isBlank()) "还没有文章" else "没有找到匹配的文章", color = SaltTheme.colors.subText)
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onNewArticle, appearance = ButtonAppearance.Subtle) { Text("写第一篇文章") }
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredArticles, key = { it.id }) { article ->
                            ArticleCard(article, onClick = { onArticleClick(article.id) }, onTogglePublish = { vm.togglePublish(article.id) }, onDelete = { vm.deleteArticle(article.id) })
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
        Column(Modifier.padding(16.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(article.title, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text("${article.date} · ${article.category}", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText)
                }
                // 更多菜单
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MoreVert), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(if (article.status == "published") "下架" else "发布") }, onClick = { showMenu = false; onTogglePublish() }, leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Publish), contentDescription = null) })
                        DropdownMenuItem(text = { Text("删除", color = Color(0xFFE53935)) }, onClick = { showMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = null, tint = Color(0xFFE53935)) })
                    }
                }
            }
            // 摘要
            if (article.excerpt.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(article.excerpt, style = SaltTheme.textStyles.sub, maxLines = 2, overflow = TextOverflow.Ellipsis, color = SaltTheme.colors.subText)
            }
            // 标签
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(onClick = {}, label = { Text(if (article.status == "published") "已发布" else "草稿", fontSize = 11.sp) }, modifier = Modifier.height(24.dp))
                if (article.pinned == 1) SuggestionChip(onClick = {}, label = { Text("📌 置顶", fontSize = 11.sp) }, modifier = Modifier.height(24.dp))
            }
        }
    }
}
