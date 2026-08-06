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

fun stripHtml(html: String): String = html.replace(Regex("<[^>]*>"), "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    // Reply dialog
    state.replyingTo?.let { parent ->
        AlertDialog(
            onDismissRequest = { vm.cancelReply() },
            title = { Text("回复 ${parent.nickname}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.replyNickname,
                        onValueChange = { vm.updateReplyNickname(it) },
                        label = { Text("昵称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.replyContent,
                        onValueChange = { vm.updateReplyContent(it) },
                        label = { Text("回复内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.submitReply() }, enabled = state.replyContent.isNotBlank() && state.replyNickname.isNotBlank()) { Text("发送") } },
            dismissButton = { TextButton(onClick = { vm.cancelReply() }) { Text("取消") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("评论") }) }) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(0.35f)) {
                items(state.pages) { page ->
                    ListItem(
                        { Text(vm.getPageTitle(page), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        Modifier.clickable { vm.loadComments(page) },
                        colors = if (page == state.selectedPage) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
                    )
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
                            Text(stripHtml(c.contentHtml), style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                // Like button
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton({ vm.likeComment(c.id) }, Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Favorite, "点赞", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                    Text("${c.liked}", style = MaterialTheme.typography.bodySmall)
                                }
                                // Reply button
                                IconButton({ vm.startReply(c) }, Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Reply, "回复", Modifier.size(16.dp))
                                }
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
