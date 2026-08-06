package com.haprial.app.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import com.moriafly.salt.ui.*
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableSaltUiApi::class)
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

    // Preview dialog
    if (showPreview) {
        Dialog(onDismissRequest = { showPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
                Column(Modifier.fillMaxSize()) {
                    TitleBar(
                        onBack = { showPreview = false },
                        text = "预览"
                    )
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Title bar with actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                TitleBarButton(onClick = onBack) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack), contentDescription = "返回")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (articleId == 0) "写文章" else "编辑",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 17.sp
                )
                Spacer(Modifier.weight(1f))

                // Action buttons
                if (state.isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                TitleBarButton(onClick = { showPreview = true }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Visibility), contentDescription = "预览")
                }
                TitleBarButton(onClick = { vm.save("published") }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Publish), contentDescription = "发布")
                }
                TitleBarButton(onClick = { vm.save("draft") }) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Save), contentDescription = "保存")
                }
            }

            Column(Modifier.fillMaxSize()) {
                // Meta info area - collapsible
                AnimatedVisibility(visible = metaExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column {
                        RoundedColumn {
                            ItemEdit(
                                text = state.title,
                                onChange = { vm.updateTitle(it) },
                                hint = "标题"
                            )
                        }
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                RoundedColumn {
                                    ItemEdit(text = state.date, onChange = {}, hint = "日期", readOnly = true)
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                RoundedColumn {
                                    ItemEdit(text = state.category, onChange = { vm.updateCategory(it) }, hint = "分类")
                                }
                            }
                        }
                        RoundedColumn {
                            ItemEdit(text = state.tags, onChange = { vm.updateTags(it) }, hint = "标签（逗号分隔）")
                        }
                        state.error?.let {
                            Text(it, color = SaltTheme.colors.error, modifier = Modifier.padding(horizontal = 16.dp), style = SaltTheme.textStyles.sub)
                        }
                        ItemDivider()
                    }
                }

                // Collapsed state: expand button
                if (!metaExpanded) {
                    Box(Modifier.fillMaxWidth().padding(8.dp), Alignment.CenterEnd) {
                        Button(
                            onClick = { metaExpanded = true },
                            appearance = ButtonAppearance.Subtle
                        ) {
                            Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.ExpandMore), contentDescription = "展开信息", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Markdown toolbar
                MarkdownToolbar { prefix, suffix -> vm.insertMarkdown(prefix, suffix) }

                // Editor body
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
}

@Composable
fun MarkdownToolbar(onAction: (String, String) -> Unit) {
    // Using Material3 ScrollableTabRow as SaltUI doesn't have an equivalent
    androidx.compose.material3.ScrollableTabRow(selectedTabIndex = 0, edgePadding = 8.dp, divider = {}) {
        listOf(
            "B" to ("**" to "**"), "I" to ("*" to "*"), "S" to ("~~" to "~~"),
            "H2" to ("## " to ""), "H3" to ("### " to ""),
            "🔗" to ("[" to "](url)"), "📷" to ("![" to "](url)"),
            ">" to ("> " to ""), "—" to ("- " to ""),
            "</>" to ("`" to "`"), "```" to ("\n```\n" to "\n```\n"), "☑" to ("- [ ] " to "")
        ).forEach { (label, action) ->
            androidx.compose.material3.Tab(false, onClick = { onAction(action.first, action.second) }, text = { Text(label, style = SaltTheme.textStyles.sub) })
        }
    }
}
