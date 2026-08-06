package com.haprial.app.ui.images

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.moriafly.salt.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.net.URL

@OptIn(ExperimentalFoundationApi::class, UnstableSaltUiApi::class)
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
        YesNoDialog(
            onDismissRequest = { deleteTarget = null },
            onConfirm = { deleteTarget = null; vm.deleteImage(path) },
            title = "删除图片",
            content = "确定删除此图片？"
        )
    }

    // Full screen preview dialog
    previewImage?.let { url ->
        Dialog(
            onDismissRequest = { previewImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Row(
                    Modifier.align(Alignment.TopEnd).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDownloading) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp), color = SaltTheme.colors.text)
                    } else {
                        Button(
                            onClick = {
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
                            },
                            appearance = ButtonAppearance.Subtle
                        ) {
                            Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Download), contentDescription = "下载")
                        }
                    }
                    TitleBarButton(onClick = { previewImage = null }) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Close), contentDescription = "关闭", tint = SaltTheme.colors.text)
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Title bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentFolder.isNotEmpty()) {
                    TitleBarButton(onClick = { vm.goBack() }) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack), contentDescription = "返回")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.currentFolder.isEmpty()) "图片" else state.currentFolder,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                TitleBarButton(onClick = { vm.loadImages(state.currentFolder) }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh), contentDescription = "刷新")
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.folders, key = { it }) { f ->
                        RoundedColumn(
                            modifier = Modifier.combinedClickable(onClick = { vm.enterFolder(f) })
                        ) {
                            Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Folder), contentDescription = null, modifier = Modifier.size(32.dp), tint = SaltTheme.colors.highlight)
                                Spacer(Modifier.height(4.dp))
                                Text(f, style = SaltTheme.textStyles.sub, maxLines = 1)
                            }
                        }
                    }
                    items(state.images, key = { it.sha }) { img ->
                        val fullUrl = "https://pluslogic.eu.org" + img.url
                        RoundedColumn(
                            modifier = Modifier.combinedClickable(
                                onClick = { previewImage = fullUrl },
                                onLongClick = { deleteTarget = "${state.currentFolder}/${img.name}" }
                            ),
                            paddingValues = PaddingValues(0.dp)
                        ) {
                            AsyncImage(fullUrl, img.name, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        }
    }
}
