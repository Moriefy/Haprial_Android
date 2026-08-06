package com.haprial.app.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haprial.app.data.model.Comment
import com.moriafly.salt.ui.*
import org.koin.androidx.compose.koinViewModel

fun stripHtml(html: String): String = html.replace(Regex("<[^>]*>"), "").trim()

data class CommentNode(val comment: Comment, val children: List<CommentNode>)

fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
    val byParent = comments.groupBy { it.parentId }
    fun buildChildren(parentId: Int): List<CommentNode> {
        return (byParent[parentId] ?: emptyList()).map { child ->
            CommentNode(child, buildChildren(child.id))
        }
    }
    return buildChildren(0)
}

@OptIn(UnstableSaltUiApi::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    // Reply dialog
    state.replyingTo?.let { parent ->
        YesNoDialog(
            onDismissRequest = { vm.cancelReply() },
            onConfirm = { vm.submitReply() },
            title = "回复 ${parent.nickname}",
            content = "",
            drawContent = {
                Column {
                    ItemEdit(
                        text = state.replyNickname,
                        onChange = { vm.updateReplyNickname(it) },
                        hint = "昵称"
                    )
                    Spacer(Modifier.height(8.dp))
                    ItemEdit(
                        text = state.replyContent,
                        onChange = { vm.updateReplyContent(it) },
                        hint = "回复内容"
                    )
                }
            },
            cancelText = "取消",
            confirmText = "发送"
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(onBack = {}, text = "评论", showBackBtn = false)

            Row(Modifier.fillMaxSize()) {
                // Left panel: article titles
                LazyColumn(Modifier.weight(0.35f)) {
                    items(state.pages, key = { it }) { page ->
                        val title = vm.getPageTitle(page)
                        Item(
                            onClick = { vm.loadComments(page) },
                            text = title,
                            arrowType = ItemArrowType.None
                        )
                    }
                }
                ItemDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                // Right panel: comment tree
                if (state.isLoading) {
                    Box(Modifier.weight(0.65f), Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else if (state.comments.isEmpty()) {
                    Box(Modifier.weight(0.65f), Alignment.Center) {
                        Text("暂无评论", color = SaltTheme.colors.subText)
                    }
                } else {
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
}

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
    node.children.forEach { child ->
        renderCommentNode(child, depth + 1, vm)
    }
}

@OptIn(UnstableSaltUiApi::class)
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

    RoundedColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 2.dp, bottom = 2.dp, end = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.nickname, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, modifier = Modifier.weight(1f))
                if (comment.isAdmin == 1) {
                    androidx.compose.material3.SuggestionChip(onClick = {}, label = { Text("博主") }, modifier = Modifier.height(24.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (comment.pinned == 1) {
                    androidx.compose.material3.SuggestionChip(onClick = {}, label = { Text("置顶") }, modifier = Modifier.height(24.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stripHtml(comment.contentHtml), style = SaltTheme.textStyles.main, maxLines = 10, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.createdAt, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = Modifier.weight(1f))
                // Like
                androidx.compose.material3.IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Favorite), contentDescription = "点赞", modifier = Modifier.size(16.dp), tint = if (comment.liked > 0) SaltTheme.colors.error else SaltTheme.colors.subText)
                }
                if (comment.liked > 0) {
                    Text("${comment.liked}", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.error)
                }
                if (depth < 2) {
                    androidx.compose.material3.IconButton(onClick = onReply, modifier = Modifier.size(32.dp)) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Reply), contentDescription = "回复", modifier = Modifier.size(16.dp))
                    }
                }
                androidx.compose.material3.IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.PushPin), contentDescription = "置顶", modifier = Modifier.size(16.dp))
                }
                androidx.compose.material3.IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = "删除", modifier = Modifier.size(16.dp), tint = SaltTheme.colors.error)
                }
            }
        }
    }
}
