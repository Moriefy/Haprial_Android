package com.haprial.app.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haprial.app.data.model.Comment
import com.moriafly.salt.ui.*
import com.moriafly.salt.ui.dialog.YesNoDialog
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

@OptIn(UnstableSaltApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showPageDropdown by remember { mutableStateOf(false) }

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
            // 统一标题栏 + 文章选择器
            TitleBar(onBack = {}, text = "评论", showBackBtn = false)

            // 文章选择下拉
            if (state.pages.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    RoundedColumn {
                        Box {
                            Item(
                                onClick = { showPageDropdown = true },
                                text = vm.getPageTitle(state.selectedPage),
                                sub = "点击切换文章",
                                arrowType = com.moriafly.salt.ui.ItemArrowType.Arrow
                            )
                            DropdownMenu(
                                expanded = showPageDropdown,
                                onDismissRequest = { showPageDropdown = false }
                            ) {
                                state.pages.forEach { page ->
                                    DropdownMenuItem(
                                        text = { Text(vm.getPageTitle(page)) },
                                        onClick = {
                                            vm.loadComments(page)
                                            showPageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 评论列表
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                state.comments.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("暂无评论", color = SaltTheme.colors.subText)
                }
                else -> {
                    val tree = remember(state.comments) { buildCommentTree(state.comments) }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

@OptIn(UnstableSaltApi::class)
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
            .padding(start = indent)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.nickname,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (comment.isAdmin == 1) {
                    SuggestionChip(onClick = {}, label = { Text("博主") }, modifier = Modifier.height(24.dp))
                    Spacer(Modifier.width(4.dp))
                }
                if (comment.pinned == 1) {
                    SuggestionChip(onClick = {}, label = { Text("置顶") }, modifier = Modifier.height(24.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stripHtml(comment.contentHtml),
                style = SaltTheme.textStyles.main,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.createdAt,
                    style = SaltTheme.textStyles.sub,
                    color = SaltTheme.colors.subText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Favorite),
                        contentDescription = "点赞",
                        modifier = Modifier.size(16.dp),
                        tint = if (comment.liked > 0) Color(0xFFE53935) else SaltTheme.colors.subText
                    )
                }
                if (comment.liked > 0) {
                    Text("${comment.liked}", style = SaltTheme.textStyles.sub, color = Color(0xFFE53935))
                }
                if (depth < 2) {
                    IconButton(onClick = onReply, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Reply),
                            contentDescription = "回复",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.PushPin),
                        contentDescription = "置顶",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete),
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}
