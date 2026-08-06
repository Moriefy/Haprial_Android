package com.haprial.app.ui.articles

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haprial.app.data.model.Article
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleListScreen(onArticleClick: (Int) -> Unit, onNewArticle: () -> Unit, vm: ArticleListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("") }
    var yearFilter by remember { mutableStateOf("") }
    var tagFilter by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10
    val listState = rememberLazyListState()

    // Hero collapse: track scroll direction
    var previousScrollIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    val isScrollingDown by remember {
        derivedStateOf {
            val currentIndex = listState.firstVisibleItemIndex
            val currentOffset = listState.firstVisibleItemScrollOffset
            val scrollingDown = currentIndex > previousScrollIndex ||
                (currentIndex == previousScrollIndex && currentOffset > previousScrollIndex + 50)
            previousScrollIndex = currentIndex
            previousScrollOffset = currentOffset
            scrollingDown
        }
    }
    val showHero by remember { derivedStateOf { !isScrollingDown || listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 10 } }

    // Filter articles
    val filteredArticles = remember(state.articles, searchQuery, statusFilter, categoryFilter, yearFilter, tagFilter) {
        state.articles.filter { a ->
            (searchQuery.isBlank() || a.title.contains(searchQuery, ignoreCase = true) || a.content.orEmpty().contains(searchQuery, ignoreCase = true)) &&
            (statusFilter == "all" || a.status == statusFilter) &&
            (categoryFilter.isBlank() || a.category == categoryFilter) &&
            (yearFilter.isBlank() || a.date.startsWith(yearFilter)) &&
            (tagFilter.isBlank() || a.tagList().any { it.equals(tagFilter, ignoreCase = true) })
        }
    }

    // Pagination
    val totalPages = ((filteredArticles.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagedArticles = remember(filteredArticles, currentPage) {
        filteredArticles.drop(currentPage * pageSize).take(pageSize)
    }

    val categories = remember(state.articles) { state.articles.map { it.category }.distinct().sorted() }
    val years = remember(state.articles) { state.articles.map { it.date.take(4) }.distinct().sortedDescending() }
    val tags = remember(state.articles) { state.articles.flatMap { it.tagList() }.distinct().sorted() }

    // Reset page on filter change
    LaunchedEffect(searchQuery, statusFilter, categoryFilter, yearFilter, tagFilter) { currentPage = 0 }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("筛选条件", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                // Status
                Text("状态", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "全部", "published" to "已发布", "draft" to "草稿").forEach { (v, l) ->
                        FilterChip(selected = statusFilter == v, onClick = { statusFilter = v }, label = { Text(l) })
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Category
                if (categories.isNotEmpty()) {
                    Text("分类", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = categoryFilter.isBlank(), onClick = { categoryFilter = "" }, label = { Text("全部") })
                        categories.forEach { cat ->
                            FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = cat }, label = { Text(cat) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Year
                if (years.isNotEmpty()) {
                    Text("年份", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = yearFilter.isBlank(), onClick = { yearFilter = "" }, label = { Text("全部") })
                        years.forEach { yr ->
                            FilterChip(selected = yearFilter == yr, onClick = { yearFilter = yr }, label = { Text(yr) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Tags
                if (tags.isNotEmpty()) {
                    Text("标签", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = tagFilter.isBlank(), onClick = { tagFilter = "" }, label = { Text("全部") })
                        tags.forEach { t ->
                            FilterChip(selected = tagFilter == t, onClick = { tagFilter = t }, label = { Text(t) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Reset button
                TextButton(onClick = {
                    statusFilter = "all"; categoryFilter = ""; yearFilter = ""; tagFilter = ""
                }) { Text("重置筛选") }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("文章") }, actions = { IconButton(onClick = { vm.loadArticles() }) { Icon(Icons.Default.Refresh, "刷新") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewArticle, icon = { Icon(Icons.Default.Edit, null) }, text = { Text("写文章") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search bar - full width
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

            // Filter button row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (statusFilter != "all") {
                        AssistChip(onClick = { statusFilter = "all" }, label = { Text(when(statusFilter){"published"->"已发布";"draft"->"草稿";else->statusFilter}, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
                    }
                    if (categoryFilter.isNotBlank()) {
                        AssistChip(onClick = { categoryFilter = "" }, label = { Text(categoryFilter, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
                    }
                    if (yearFilter.isNotBlank()) {
                        AssistChip(onClick = { yearFilter = "" }, label = { Text(yearFilter, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
                    }
                    if (tagFilter.isNotBlank()) {
                        AssistChip(onClick = { tagFilter = "" }, label = { Text(tagFilter, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
                    }
                }
                // Filter button
                FilledTonalButton(onClick = { showFilterSheet = true }) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("筛选")
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
                    LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Hero section - collapses on scroll down
                        item(key = "hero") {
                            AnimatedVisibility(
                                visible = showHero,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text("Moriefyの半岛铁盒", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(Modifier.height(4.dp))
                                        Text("共 ${state.articles.size} 篇文章", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }

                        items(pagedArticles, key = { it.id }) { a ->
                            Card(Modifier.fillMaxWidth().clickable { onArticleClick(a.id) }) {
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
