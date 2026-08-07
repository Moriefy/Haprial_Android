package com.haprial.app.ui.friends

import android.widget.Toast
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
import coil.compose.AsyncImage
import com.haprial.app.data.model.Friend
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.UnstableSaltApi
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.UnstableSaltApi
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableSaltApi::class)
@Composable
fun FriendsScreen(vm: FriendsViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<Friend?>(null) }
    var deleteTarget by remember { mutableStateOf<Friend?>(null) }

    LaunchedEffect(state.error) { state.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.clearMessages() } }
    LaunchedEffect(state.successMsg) { state.successMsg?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.clearMessages() } }

    // ── 添加/编辑对话框 ──
    if (showAddDialog || editingFriend != null) {
        FriendDialog(
            friend = editingFriend,
            onDismiss = { showAddDialog = false; editingFriend = null },
            onSave = { name, url, avatar, desc ->
                if (editingFriend != null) {
                    vm.updateFriend(editingFriend!!.id, name, url, avatar, desc)
                } else {
                    vm.addFriend(name, url, avatar, desc)
                }
                showAddDialog = false; editingFriend = null
            }
        )
    }

    // ── 删除确认 ──
    deleteTarget?.let { friend ->
        YesNoDialog(
            onDismissRequest = { deleteTarget = null },
            onConfirm = { deleteTarget = null; vm.deleteFriend(friend.id) },
            title = "删除友链",
            content = "确定删除「${friend.name}」？"
        )
    }

    Surface(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
        Column(Modifier.fillMaxSize()) {
            StandardTitleBar(title = "友链") {
                TitleBarButton(onClick = { showAddDialog = true }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Add), contentDescription = "添加友链")
                }
                TitleBarButton(onClick = { vm.loadFriends() }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh), contentDescription = "刷新")
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.friends.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.People), contentDescription = null, modifier = Modifier.size(48.dp), tint = SaltTheme.colors.subText.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text("暂无友链", color = SaltTheme.colors.subText)
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.friends, key = { it.id }) { friend ->
                        FriendCard(friend, onEdit = { editingFriend = friend }, onDelete = { deleteTarget = friend })
                    }
                }
            }
        }
    }
}

@OptIn(UnstableSaltApi::class)
@Composable
private fun FriendCard(friend: Friend, onEdit: () -> Unit, onDelete: () -> Unit) {
    RoundedColumn(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // 头像
            if (!friend.avatar.isNullOrEmpty()) {
                AsyncImage(model = friend.avatar, contentDescription = friend.name, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(SaltTheme.colors.highlight.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person), contentDescription = null, modifier = Modifier.size(24.dp), tint = SaltTheme.colors.highlight)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!friend.desc.isNullOrEmpty()) {
                    Text(friend.desc, fontSize = 12.sp, color = SaltTheme.colors.subText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(friend.url, fontSize = 11.sp, color = SaltTheme.colors.highlight.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Edit), contentDescription = "编辑", modifier = Modifier.size(18.dp), tint = SaltTheme.colors.subText)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Delete), contentDescription = "删除", modifier = Modifier.size(18.dp), tint = Color(0xFFE53935))
            }
        }
    }
}

@Composable
private fun FriendDialog(friend: Friend?, onDismiss: () -> Unit, onSave: (name: String, url: String, avatar: String, desc: String) -> Unit) {
    var name by remember { mutableStateOf(friend?.name ?: "") }
    var url by remember { mutableStateOf(friend?.url ?: "") }
    var avatar by remember { mutableStateOf(friend?.avatar ?: "") }
    var desc by remember { mutableStateOf(friend?.desc ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (friend != null) "编辑友链" else "添加友链", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogField("名称", name, { name = it }, "网站名称")
                DialogField("链接", url, { url = it }, "https://example.com")
                DialogField("头像", avatar, { avatar = it }, "头像 URL（可选）")
                DialogField("描述", desc, { desc = it }, "简短描述（可选）")
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && url.isNotBlank()) onSave(name, url, avatar, desc) }, enabled = name.isNotBlank() && url.isNotBlank()) {
                Text(if (friend != null) "保存" else "添加")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DialogField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    Column {
        Text(label, fontSize = 12.sp, color = SaltTheme.colors.subText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SaltTheme.colors.background).border(0.5f.dp, SaltTheme.colors.stroke, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (value.isEmpty()) Text(placeholder, color = SaltTheme.colors.subText.copy(alpha = 0.4f), fontSize = 14.sp)
            androidx.compose.foundation.text.BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SaltTheme.colors.text), modifier = Modifier.fillMaxWidth())
        }
    }
}
