package com.haprial.app.ui.images

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageManagerScreen(vm: ImageManagerViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewImage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    deleteTarget?.let { path ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除图片") },
            text = { Text("确定删除此图片？") },
            confirmButton = { TextButton(onClick = { deleteTarget = null; vm.deleteImage(path) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }

    // Full screen preview dialog with download button
    previewImage?.let { url ->
        Dialog(
            onDismissRequest = { previewImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Top bar with close and download
                Row(
                    Modifier.align(Alignment.TopEnd).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        FilledTonalIconButton(onClick = {
                            isDownloading = true
                            scope.launch {
                                try {
                                    val bitmap = withContext(Dispatchers.IO) {
                                        val connection = URL(url).openConnection()
                                        connection.connect()
                                        val input = connection.getInputStream()
                                        BitmapFactory.decodeStream(input)
                                    }
                                    if (bitmap != null) {
                                        val fileName = url.substringAfterLast("/").ifEmpty { "image_${System.currentTimeMillis()}.jpg" }
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Haprial")
                                                put(MediaStore.Images.Media.IS_PENDING, 1)
                                            }
                                        }
                                        val resolver = ctx.contentResolver
                                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                        if (uri != null) {
                                            withContext(Dispatchers.IO) {
                                                resolver.openOutputStream(uri)?.use { out ->
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    contentValues.clear()
                                                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                                    resolver.update(uri, contentValues, null, null)
                                                }
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(ctx, "已保存到相册", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(ctx, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isDownloading = false
                            }
                        }) {
                            Icon(Icons.Default.Download, "下载")
                        }
                    }
                    IconButton(onClick = { previewImage = null }) {
                        Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

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
        else LazyVerticalGrid(columns = GridCells.Adaptive(100.dp), modifier = Modifier.padding(padding), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.folders, key = { it }) { f ->
                Card(Modifier.combinedClickable(onClick = { vm.enterFolder(f) }), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp)); Text(f, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
            items(state.images, key = { it.sha }) { img ->
                val fullUrl = "https://pluslogic.eu.org" + img.url
                Card(
                    Modifier.combinedClickable(
                        onClick = { previewImage = fullUrl },
                        onLongClick = { deleteTarget = "${state.currentFolder}/${img.name}" }
                    )
                ) {
                    AsyncImage(fullUrl, img.name, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}
