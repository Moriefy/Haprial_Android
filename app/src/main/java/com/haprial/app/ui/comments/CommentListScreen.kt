package com.haprial.app.ui.comments

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haprial.app.data.model.Comment
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.dialog.YesNoDialog
import org.koin.androidx.compose.koinViewModel

fun stripHtml(html: String): String = html.replace(Regex("<[^>]*>"), "").trim()

data class CommentNode(val comment: Comment, val children: List<CommentNode>)

fun buildCommentTree(comments: List<Comment>): List<CommentNode> {
    val byParent = comments.groupBy { it.parentId }
    fun buildChildren(parentId: Int): List<CommentNode> = (byParent[parentId] ?: emptyList()).map { CommentNode(it, buildChildren(it.id)) }
    return buildChildren(0)
}

@OptIn(UnstableSaltApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(vm: CommentListViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showPageMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // 错误提示
    LaunchedEffect(state.error) {
        state.error?.let { android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show() }
    }

    // 回复对话框
    state.replyingTo?.let { parent ->
        YesNoDialog(
            onDismissRequest = { vm.cancelReply() },
            onConfirm = { vm.submitReply() },
            title = "回复 ${parent.nickname}",
            content = "",
            drawContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemEdit(text = state.replyNickname, onChange = { vm.updateReplyNickname(it) }, hint = "昵称")
                    ItemEdit(text = state.replyContent, onChange = { vm.updateReplyContent(it) }, hint = "回复内容")
                }
            },
            cancelText = "取消",
            confirmText = "发送"
        )
    }

    Surface(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            // ── 标题栏 ──
            StandardTitleBar(title = "评论") {
                TitleBarButton(onClick = { vm.refresh() }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh), contentDescription = "刷新")
                }
            }

            // ── 文章选择器 ──
            if (state.pages.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    RoundedColumn {
                        Box {
                            Row(
                                Modifier.fillMaxWidth().clickable { showPageMenu = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(vm.getPageTitle(state.selectedPage), fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${state.comments.size} 条评论", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText)
                                }
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.ArrowDropDown), contentDescription = null, modifier = Modifier.size(24.dp), tint = SaltTheme.colors.subText)
                            }
                            DropdownMenu(expanded = showPageMenu, onDismissRequest = { showPageMenu = false }) {
                                state.pages.forEach { page ->
                                    DropdownMenuItem(
                                        text = { Text(vm.getPageTitle(page), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = { vm.loadComments(page); showPageMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 评论列表 ──
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.comments.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Comment), contentDescription = null, modifier = Modifier.size(48.dp), tint = SaltTheme.colors.subText.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))
                        Text("暂无评论", color = SaltTheme.colors.subText)
                    }
                }
                else -> {
                    val tree = remember(state.comments) { buildCommentTree(state.comments) }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tree.forEach { node -> renderCommentNode(node, depth = 0, vm = vm) }
                    }
                }
            }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.renderCommentNode(node: CommentNode, depth: Int, vm: CommentListViewModel) {
    item(key = node.comment.id) {
        CommentItem(comment = node.comment, depth = depth, onLike = { vm.likeComment(node.comment.id) }, onReply = { vm.startReply(node.comment) }, onPin = { vm.pinComment(node.comment.id) }, onDelete = { vm.deleteComment(node.comment.id) })
    }
    node.children.forEach { child -> renderCommentNode(child, depth + 1, vm) }
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun CommentItem(comment: Comment, depth: Int, onLike: () -> Unit, onReply: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    val indent = (depth * 16).dp
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        YesNoDialog(onDismissRequest = { showDeleteDialog = false }, onConfirm = { showDeleteDialog = false; onDelete() }, title = "删除评论", content = "确定删除 ${comment.nickname} 的评论？")
    }

    RoundedColumn(modifier = Modifier.fillMaxWidth().padding(start = indent)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.nickname, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (comment.isAdmin == 1) { SuggestionChip(onClick = {}, label = { Text("博主", fontSize = 10.sp) }, modifier = Modifier.height(22.dp)) }
                if (comment.pinned == 1) { Spacer(Modifier.width(4.dp)); SuggestionChip(onClick = {}, label = { Text("置顶", fontSize = 10.sp) }, modifier = Modifier.height(22.dp)) }
            }
            Spacer(Modifier.height(4.dp))
            Text(stripHtml(comment.contentHtml), style = SaltTheme.textStyles.main, maxLines = 10, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.createdAt, style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = Modifier.weight(1f))
                IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Favorite), contentDescription = "点赞", modifier = Modifier.size(16.dp), tint = if (comment.liked > 0) Color(0xFFE53935) else SaltTheme.colors.subText)
                }
                if (comment.liked > 0) Text("${comment.liked}", style = SaltTheme.textStyles.sub, color = Color(0xFFE53935))
                if (depth < 2) {
                    IconButton(onClick = onReply, modifier = Modifier.size(32.dp)) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Reply), contentDescription = "回复", modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.PushPin), contentDescription = "置顶", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = "删除", modifier = Modifier.size(16.dp), tint = Color(0xFFE53935))
                }
            }
        }
    }
}
