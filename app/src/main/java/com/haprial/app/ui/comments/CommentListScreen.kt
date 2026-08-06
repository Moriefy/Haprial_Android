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

fun stripHtml(html: String): String = html.replace(Regex("<[^>]*>"), "").trim()

// 树节点：评论 + 子评论
data class CommentNode(val comment: Comment, val children: List<CommentNode>)

// 将平铺评论构建为树
fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
    val byParent = comments.groupBy { it.parentId }
    fun buildChildren(parentId: Int): List<CommentNode> {
        return (byParent[parentId] ?: emptyList()).map { child ->
            CommentNode(child, buildChildren(child.id))
        }
    }
    return buildChildren(0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    // Reply dialog with admin defaults
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
            // 左栏：文章标题
            LazyColumn(Modifier.weight(0.35f)) {
                items(state.pages, key = { it }) { page ->
                    val title = vm.getPageTitle(page)
                    ListItem(
                        { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        Modifier.clickable { vm.loadComments(page) },
                        colors = if (page == state.selectedPage) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
                    )
                }
            }
            VerticalDivider()
            // 右栏：评论树
            if (state.isLoading) Box(Modifier.weight(0.65f), Alignment.Center) { CircularProgressIndicator() }
            else if (state.comments.isEmpty()) Box(Modifier.weight(0.65f), Alignment.Center) { Text("暂无评论", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else {
                val tree = remember(state.comments) { buildCommentTree(state.comments) }
                LazyColumn(Modifier.weight(0.65f), contentPadding = PaddingValues(8.dp)) {
                    tree.forEach { node ->
                        renderCommentNode(node, depth = 0, vm = vm)
                    }
                }
            }
        }
    }
}

// 递归渲染评论树节点
fun androidx.compose.foundation.lazy.LazyListScope.renderCommentNode(
    node: CommentNode,
    depth: Int,
    vm: CommentListViewModel
) {
    item(key = node.comment.id) {
        CommentItem(
            comment = node.comment,
            depth = depth,
            onLike = { vm.likeComment(node.comment.id) },
            onReply = { vm.startReply(node.comment) },
            onPin = { vm.pinComment(node.comment.id) },
            onDelete = { vm.deleteComment(node.comment.id) }
        )
    }
    // 递归渲染子评论
    node.children.forEach { child ->
        renderCommentNode(child, depth + 1, vm)
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    depth: Int,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val indent = (depth * 16).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 2.dp, end = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            // 昵称 + 标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.nickname, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (comment.isAdmin == 1) {
                    SuggestionChip(onClick = {}, label = { Text("博主") }, modifier = Modifier.height(24.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (comment.pinned == 1) {
                    SuggestionChip(onClick = {}, label = { Text("置顶") }, modifier = Modifier.height(24.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            // 内容（去除 HTML）
            Text(stripHtml(comment.contentHtml), style = MaterialTheme.typography.bodyMedium, maxLines = 10, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            // 操作栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                // 点赞
                IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Favorite, "点赞", Modifier.size(16.dp), tint = if (comment.liked > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (comment.liked > 0) {
                    Text("${comment.liked}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                // 回复（最多2层）
                if (depth < 2) {
                    IconButton(onClick = onReply, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Reply, "回复", Modifier.size(16.dp))
                    }
                }
                // 置顶
                IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PushPin, "置顶", Modifier.size(16.dp))
                }
                // 删除
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
