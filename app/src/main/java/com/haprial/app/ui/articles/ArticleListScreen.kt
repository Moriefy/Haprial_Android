package com.haprial.app.ui.articles

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haprial.app.data.model.Article
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(onArticleClick: (Int) -> Unit, onNewArticle: () -> Unit, vm: ArticleListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("文章") }, actions = { IconButton(onClick = { vm.loadArticles() }) { Icon(Icons.Default.Refresh, "刷新") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewArticle, icon = { Icon(Icons.Default.Edit, null) }, text = { Text("写文章") }) }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error!!) }
            else -> LazyColumn(Modifier.padding(padding), PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.articles, key = { it.id }) { a ->
                    Card(Modifier.fillMaxWidth().animateContentSize().clickable { onArticleClick(a.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${a.date} · ${a.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                var menu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                                    DropdownMenu(menu, { menu = false }) {
                                        DropdownMenuItem({ Text(if (a.status == "published") "下架" else "发布") }, { menu = false; vm.togglePublish(a.id) }, leadingIcon = { Icon(Icons.Default.Publish, null) })
                                        DropdownMenuItem({ Text("删除", color = MaterialTheme.colorScheme.error) }, { menu = false; vm.deleteArticle(a.id) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                                    }
                                }
                            }
                            if (a.excerpt.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(a.excerpt, style = MaterialTheme.typography.bodyMedium, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(onClick = {}, label = { Text(if (a.status == "published") "已发布" else "草稿") })
                                if (a.pinned == 1) SuggestionChip(onClick = {}, label = { Text("📌 置顶") })
                            }
                        }
                    }
                }
            }
        }
    }
}
