package com.haprial.app.ui.comments

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
import com.haprial.app.data.model.Comment
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("评论") }) }) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(0.35f)) {
                items(state.pages) { page ->
                    ListItem({ Text(page.removePrefix("/posts/").removeSuffix("/"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        Modifier.clickable { vm.loadComments(page) },
                        colors = if (page == state.selectedPage) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors())
                }
            }
            VerticalDivider()
            if (state.isLoading) Box(Modifier.weight(0.65f), Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(Modifier.weight(0.65f), contentPadding = PaddingValues(8.dp)) {
                items(state.comments, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.nickname, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                if (c.isAdmin == 1) SuggestionChip(onClick = {}, label = { Text("博主") })
                                if (c.pinned == 1) SuggestionChip(onClick = {}, label = { Text("置顶") })
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(c.contentHtml, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Text(c.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                IconButton({ vm.pinComment(c.id) }, Modifier.size(32.dp)) { Icon(Icons.Default.PushPin, null, Modifier.size(16.dp)) }
                                IconButton({ vm.deleteComment(c.id) }, Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
