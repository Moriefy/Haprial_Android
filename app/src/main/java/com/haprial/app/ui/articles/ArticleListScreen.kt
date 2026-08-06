package com.haprial.app.ui.articles

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.model.Article
import com.moriafly.salt.ui.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class, UnstableSaltUiApi::class)
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

    LaunchedEffect(searchQuery, statusFilter, categoryFilter, yearFilter, tagFilter) { currentPage = 0 }

    // Filter bottom sheet - using Material3 ModalBottomSheet as SaltUI doesn't have an equivalent
    if (showFilterSheet) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("筛选条件", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("状态", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "全部", "published" to "已发布", "draft" to "草稿").forEach { (v, l) ->
                        androidx.compose.material3.FilterChip(
                            selected = statusFilter == v,
                            onClick = { statusFilter = v },
                            label = { Text(l) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (categories.isNotEmpty()) {
                    Text("分类", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.FilterChip(selected = categoryFilter.isBlank(), onClick = { categoryFilter = "" }, label = { Text("全部") })
                        categories.forEach { cat ->
                            androidx.compose.material3.FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = cat }, label = { Text(cat) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (years.isNotEmpty()) {
                    Text("年份", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.FilterChip(selected = yearFilter.isBlank(), onClick = { yearFilter = "" }, label = { Text("全部") })
                        years.forEach { yr ->
                            androidx.compose.material3.FilterChip(selected = yearFilter == yr, onClick = { yearFilter = yr }, label = { Text(yr) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (tags.isNotEmpty()) {
                    Text("标签", color = SaltTheme.colors.subText, style = SaltTheme.textStyles.sub)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.FilterChip(selected = tagFilter.isBlank(), onClick = { tagFilter = "" }, label = { Text("全部") })
                        tags.forEach { t ->
                            androidx.compose.material3.FilterChip(selected = tagFilter == t, onClick = { tagFilter = t }, label = { Text(t) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        statusFilter = "all"; categoryFilter = ""; yearFilter = ""; tagFilter = ""
                    },
                    appearance = ButtonAppearance.Subtle
                ) { Text("重置筛选") }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Title bar
            TitleBar(
                onBack = {},
                text = "文章",
                showBackBtn = false
            )

            // Search and filter bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search using Material3 OutlinedTextField (SaltUI ItemEdit is not ideal for inline search)
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索…", style = SaltTheme.textStyles.sub) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    singleLine = true,
                    textStyle = SaltTheme.textStyles.main.copy(fontSize = 14.sp),
                    leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Search), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            androidx.compose.material3.IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Clear), contentDescription = "清除", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
                Button(
                    onClick = { showFilterSheet = true },
                    appearance = ButtonAppearance.Subtle
                ) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.FilterList), contentDescription = null, modifier = Modifier.size(18.dp))
                    if (statusFilter != "all" || categoryFilter.isNotBlank() || yearFilter.isNotBlank() || tagFilter.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text("筛选中", style = SaltTheme.textStyles.sub)
                    }
                }
            }

            // Active filter tags
            if (statusFilter != "all" || categoryFilter.isNotBlank() || yearFilter.isNotBlank() || tagFilter.isNotBlank()) {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (statusFilter != "all") {
                        androidx.compose.material3.AssistChip(
                            onClick = { statusFilter = "all" },
                            label = { Text(if(statusFilter=="published")"已发布"else"草稿", style = SaltTheme.textStyles.sub) }
                        )
                    }
                    if (categoryFilter.isNotBlank()) {
                        androidx.compose.material3.AssistChip(onClick = { categoryFilter = "" }, label = { Text(categoryFilter, style = SaltTheme.textStyles.sub) })
                    }
                    if (yearFilter.isNotBlank()) {
                        androidx.compose.material3.AssistChip(onClick = { yearFilter = "" }, label = { Text(yearFilter, style = SaltTheme.textStyles.sub) })
                    }
                    if (tagFilter.isNotBlank()) {
                        androidx.compose.material3.AssistChip(onClick = { tagFilter = "" }, label = { Text(tagFilter, style = SaltTheme.textStyles.sub) })
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error!!, color = SaltTheme.colors.error) }
                else -> {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pagedArticles, key = { it.id }) { a ->
                            RoundedColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onArticleClick(a.id) }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(a.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("${a.date} · ${a.category}", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText)
                                        }
                                        var menu by remember { mutableStateOf(false) }
                                        Box {
                                            androidx.compose.material3.IconButton(onClick = { menu = true }) {
                                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.MoreVert), contentDescription = null)
                                            }
                                            androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                                                androidx.compose.material3.DropdownMenuItem(
                                                    { Text(if (a.status == "published") "下架" else "发布") },
                                                    { menu = false; vm.togglePublish(a.id) },
                                                    leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Publish), contentDescription = null) }
                                                )
                                                androidx.compose.material3.DropdownMenuItem(
                                                    { Text("删除", color = SaltTheme.colors.error) },
                                                    { menu = false; vm.deleteArticle(a.id) },
                                                    leadingIcon = { Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = null, tint = SaltTheme.colors.error) }
                                                )
                                            }
                                        }
                                    }
                                    if (a.excerpt.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(a.excerpt, style = SaltTheme.textStyles.sub, maxLines = 2, color = SaltTheme.colors.subText)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.SuggestionChip(onClick = {}, label = { Text(if (a.status == "published") "已发布" else "草稿") })
                                        if (a.pinned == 1) androidx.compose.material3.SuggestionChip(onClick = {}, label = { Text("📌 置顶") })
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
                            androidx.compose.material3.IconButton(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.KeyboardArrowLeft), contentDescription = "上一页")
                            }
                            Text(
                                "${currentPage + 1} / $totalPages",
                                style = SaltTheme.textStyles.main,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            androidx.compose.material3.IconButton(onClick = { if (currentPage < totalPages - 1) currentPage++ }, enabled = currentPage < totalPages - 1) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.KeyboardArrowRight), contentDescription = "下一页")
                            }
                        }
                    }
                }
            }
        }
    }
}
