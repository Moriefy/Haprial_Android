package com.haprial.app.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haprial.app.ui.components.StandardTitleBar
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.BottomBar
import com.moriafly.salt.ui.BottomBarItem
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import androidx.compose.material3.Text
import com.moriafly.salt.ui.dialog.YesNoDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableSaltApi::class)
@Composable
fun TrashScreen(vm: TrashViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showEmptyDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Int?>(null) }

    // 永久删除确认
    deleteTargetId?.let { id ->
        val targetItem = state.items.find { it.id == id }
        YesNoDialog(
            onDismissRequest = { deleteTargetId = null },
            onConfirm = { deleteTargetId = null; vm.delete(id) },
            title = "永久删除",
            content = "确定永久删除「${targetItem?.title ?: ""}」？此操作不可撤销。"
        )
    }

    if (showEmptyDialog) {
        YesNoDialog(
            onDismissRequest = { showEmptyDialog = false },
            onConfirm = { showEmptyDialog = false; vm.emptyTrash() },
            title = "清空回收站",
            content = "确定永久删除所有已删除的文章？此操作不可撤销。"
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // 统一标题栏
            StandardTitleBar(title = "回收站") {
                TitleBarButton(onClick = { vm.loadTrash() }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh), contentDescription = "刷新")
                }
                TitleBarButton(onClick = { showEmptyDialog = true }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.DeleteSweep), contentDescription = "清空")
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(state.error!!, color = Color(0xFFE53935))
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("回收站为空", color = SaltTheme.colors.subText)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        RoundedColumn {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                    Text("删除于 ${item.deletedAt}", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText)
                                }
                                androidx.compose.material3.IconButton(onClick = { vm.restore(item.id) }) {
                                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Restore), contentDescription = "恢复", tint = SaltTheme.colors.highlight)
                                }
                                IconButton(onClick = { deleteTargetId = item.id }) {
                                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.DeleteForever), contentDescription = "永久删除", tint = Color(0xFFE53935))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
