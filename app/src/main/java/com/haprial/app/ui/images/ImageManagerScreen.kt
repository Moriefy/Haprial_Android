package com.haprial.app.ui.images

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageManagerScreen(vm: ImageManagerViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.currentFolder.isEmpty()) "图片" else state.currentFolder) },
                navigationIcon = { if (state.currentFolder.isNotEmpty()) IconButton(onClick = { vm.goBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { vm.loadImages(state.currentFolder) }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        if (state.isLoading) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
        else LazyVerticalGrid(GridCells.Adaptive(100.dp), Modifier.padding(padding), PaddingValues(8.dp), Arrangement.spacedBy(8.dp), Arrangement.spacedBy(8.dp)) {
            items(state.folders) { f ->
                Card(Modifier.clickable { vm.enterFolder(f) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp)); Text(f, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            items(state.images, key = { it.sha }) { img ->
                var menu by remember { mutableStateOf(false) }
                Card {
                    Box {
                        AsyncImage("https://pluslogic.eu.org" + img.url, img.name, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd)) {
                            IconButton({ menu = true }, Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null, Modifier.size(16.dp)) }
                            DropdownMenu(menu, { menu = false }) { DropdownMenuItem({ Text("删除") }, { menu = false; vm.deleteImage("${state.currentFolder}/${img.name}") }) }
                        }
                    }
                }
            }
        }
    }
}
