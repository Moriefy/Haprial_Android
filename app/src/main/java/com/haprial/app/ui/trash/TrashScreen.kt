package com.haprial.app.ui.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(vm: TrashViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    var showEmptyDialog by remember { mutableStateOf(false) }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("清空回收站") },
            text = { Text("确定永久删除所有已删除的文章？此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { showEmptyDialog = false; vm.emptyTrash() }) { Text("清空", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showEmptyDialog = false }) { Text("取消") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                actions = {
                    IconButton(onClick = { vm.loadTrash() }) { Icon(Icons.Default.Refresh, "刷新") }
                    IconButton(onClick = { showEmptyDialog = true }) { Icon(Icons.Default.DeleteSweep, "清空") }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text(state.error!!) }
            state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("回收站为空", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text("删除于 ${item.deletedAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.restore(item.id) }) { Icon(Icons.Default.Restore, "恢复", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Default.DeleteForever, "永久删除", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
