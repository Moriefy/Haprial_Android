package com.haprial.app.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(articleId: Int, onBack: () -> Unit, vm: EditorViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val markwon = remember {
        Markwon.builder(ctx).usePlugin(TablePlugin.create(ctx)).usePlugin(LinkifyPlugin.create()).build()
    }
    val markwonEditor = remember { MarkwonEditor.builder(markwon).build() }

    var showPreview by remember { mutableStateOf(false) }
    var metaExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(articleId) { vm.loadArticle(articleId) }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    // 预览弹窗 - 只在点击时渲染，不跟随 state.content 实时更新
    if (showPreview) {
        Dialog(onDismissRequest = { showPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(title = { Text("预览") }, navigationIcon = { IconButton({ showPreview = false }) { Icon(Icons.Default.Close, "关闭") } })
                    AndroidView(
                        factory = { c ->
                            ScrollView(c).apply {
                                val tv = TextView(c).apply { textSize = 16f; setPadding(48, 32, 48, 32) }
                                addView(tv)
                                markwon.setMarkdown(tv, vm.getContent())
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (articleId == 0) "写文章" else "编辑") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(24.dp))
                    IconButton(onClick = { showPreview = true }) { Icon(Icons.Default.Visibility, "预览") }
                    IconButton(onClick = { vm.save("published") }) { Icon(Icons.Default.Publish, "发布") }
                    IconButton(onClick = { vm.save("draft") }) { Icon(Icons.Default.Save, "保存") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 元信息区域 - 可折叠
            AnimatedVisibility(visible = metaExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    OutlinedTextField(state.title, { vm.updateTitle(it) }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), singleLine = true)
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(state.date, {}, label = { Text("日期") }, modifier = Modifier.weight(1f), readOnly = true)
                        OutlinedTextField(state.category, { vm.updateCategory(it) }, label = { Text("分类") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(state.tags, { vm.updateTags(it) }, label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), singleLine = true)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                    HorizontalDivider()
                }
            }

            // 折叠状态：显示浮动小球
            if (!metaExpanded) {
                Box(Modifier.fillMaxWidth().padding(8.dp), Alignment.CenterEnd) {
                    FilledIconButton(
                        onClick = { metaExpanded = true },
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    ) {
                        Icon(Icons.Default.ExpandMore, "展开信息", Modifier.size(20.dp))
                    }
                }
            }

            // 工具栏
            MarkdownToolbar { prefix, suffix -> vm.insertMarkdown(prefix, suffix) }

            // 编辑器主体
            AndroidView(
                factory = { c ->
                    EditText(c).apply {
                        textSize = 16f; setPadding(48, 32, 48, 32); background = null
                        addTextChangedListener(MarkwonEditorTextWatcher.withProcess(markwonEditor))
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) { s?.toString()?.let { vm.updateContent(it) } }
                        })
                        // 滚动时折叠元信息区域
                        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                            if (scrollY > oldScrollY + 10 && metaExpanded) metaExpanded = false
                        }
                    }
                },
                update = { et ->
                    if (et.text.toString() != state.content) {
                        et.setText(state.content)
                        et.setSelection(et.text.length.coerceAtMost(state.content.length))
                    }
                    vm.pendingInsertion?.let { (prefix, suffix) ->
                        val start = et.selectionStart.coerceAtLeast(0)
                        val end = et.selectionEnd.coerceAtLeast(0)
                        val selStart = minOf(start, end); val selEnd = maxOf(start, end)
                        val selected = et.text.substring(selStart, selEnd)
                        val replacement = if (selected.isNotEmpty()) "$prefix$selected$suffix" else "$prefix$suffix"
                        et.text.replace(selStart, selEnd, replacement)
                        et.setSelection((if (selected.isNotEmpty()) selStart + replacement.length else selStart + prefix.length).coerceAtMost(et.text.length))
                        vm.pendingInsertion = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MarkdownToolbar(onAction: (String, String) -> Unit) {
    ScrollableTabRow(selectedTabIndex = 0, edgePadding = 8.dp, divider = {}) {
        listOf(
            "B" to ("**" to "**"), "I" to ("*" to "*"), "S" to ("~~" to "~~"),
            "H2" to ("## " to ""), "H3" to ("### " to ""),
            "🔗" to ("[" to "](url)"), "📷" to ("![" to "](url)"),
            ">" to ("> " to ""), "—" to ("- " to ""),
            "</>" to ("`" to "`"), "```" to ("\n```\n" to "\n```\n"), "☑" to ("- [ ] " to "")
        ).forEach { (label, action) ->
            Tab(false, onClick = { onAction(action.first, action.second) }, text = { Text(label, style = MaterialTheme.typography.labelMedium) })
        }
    }
}
