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
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("") }
    var yearFilter by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10

    // Filter articles
    val filteredArticles = remember(state.articles, searchQuery, statusFilter, categoryFilter, yearFilter) {
        state.articles.filter { a ->
            (searchQuery.isBlank() || a.title.contains(searchQuery, ignoreCase = true) || a.content.orEmpty().contains(searchQuery, ignoreCase = true)) &&
            (statusFilter == "all" || a.status == statusFilter) &&
            (categoryFilter.isBlank() || a.category == categoryFilter) &&
            (yearFilter.isBlank() || a.date.startsWith(yearFilter))
        }
    }

    // Pagination
    val totalPages = ((filteredArticles.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagedArticles = filteredArticles.drop(currentPage * pageSize).take(pageSize)

    // Get unique categories and years for filters
    val categories = remember(state.articles) { state.articles.map { it.category }.distinct().sorted() }
    val years = remember(state.articles) { state.articles.map { it.date.take(4) }.distinct().sortedDescending() }

    // Reset page on filter change
    LaunchedEffect(searchQuery, statusFilter, categoryFilter, yearFilter) { currentPage = 0 }

    Scaffold(
        topBar = { TopAppBar(title = { Text("文章") }, actions = { IconButton(onClick = { vm.loadArticles() }) { Icon(Icons.Default.Refresh, "刷新") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewArticle, icon = { Icon(Icons.Default.Edit, null) }, text = { Text("写文章") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜索文章...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "清除") }
                }
            )

            // Filter row
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status filter
                var statusExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(statusExpanded, { statusExpanded = it }) {
                    OutlinedTextField(
                        value = when (statusFilter) { "all" -> "全部状态"; "published" -> "已发布"; "draft" -> "草稿"; else -> statusFilter },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("状态") },
                        modifier = Modifier.weight(1f).menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) }
                    )
                    ExposedDropdownMenu(statusExpanded, { statusExpanded = false }) {
                        listOf("all" to "全部状态", "published" to "已发布", "draft" to "草稿").forEach { (v, l) ->
                            DropdownMenuItem(text = { Text(l) }, onClick = { statusFilter = v; statusExpanded = false })
                        }
                    }
                }

                // Category filter
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(catExpanded, { catExpanded = it }) {
                    OutlinedTextField(
                        value = categoryFilter.ifBlank { "全部分类" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        modifier = Modifier.weight(1f).menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }
                    )
                    ExposedDropdownMenu(catExpanded, { catExpanded = false }) {
                        DropdownMenuItem(text = { Text("全部分类") }, onClick = { categoryFilter = ""; catExpanded = false })
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { categoryFilter = cat; catExpanded = false })
                        }
                    }
                }

                // Year filter
                var yearExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(yearExpanded, { yearExpanded = it }) {
                    OutlinedTextField(
                        value = yearFilter.ifBlank { "全部年份" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("年份") },
                        modifier = Modifier.weight(1f).menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) }
                    )
                    ExposedDropdownMenu(yearExpanded, { yearExpanded = false }) {
                        DropdownMenuItem(text = { Text("全部年份") }, onClick = { yearFilter = ""; yearExpanded = false })
                        years.forEach { yr ->
                            DropdownMenuItem(text = { Text(yr) }, onClick = { yearFilter = yr; yearExpanded = false })
                        }
                    }
                }
            }

            Text(
                "共 ${filteredArticles.size} 篇",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error!!) }
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pagedArticles, key = { it.id }) { a ->
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

                    // Pagination controls
                    if (totalPages > 1) {
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                                Icon(Icons.Default.KeyboardArrowLeft, "上一页")
                            }
                            Text(
                                "${currentPage + 1} / $totalPages",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1) {
                                Icon(Icons.Default.KeyboardArrowRight, "下一页")
                            }
                        }
                    }
                }
            }
        }
    }
}
