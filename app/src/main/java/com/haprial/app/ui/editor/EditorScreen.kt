package com.haprial.app.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
        Markwon.builder(ctx)
            .usePlugin(TablePlugin.create(ctx))
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    val markwonEditor = remember { MarkwonEditor.builder(markwon).build() }

    LaunchedEffect(articleId) { vm.loadArticle(articleId) }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (articleId == 0) "写文章" else "编辑") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(24.dp))
                    IconButton(onClick = { vm.save("published") }) { Icon(Icons.Default.Publish, "发布") }
                    IconButton(onClick = { vm.save("draft") }) { Icon(Icons.Default.Save, "保存") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                state.title, { vm.updateTitle(it) },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(state.date, {}, label = { Text("日期") }, modifier = Modifier.weight(1f), readOnly = true)
                OutlinedTextField(state.category, { vm.updateCategory(it) }, label = { Text("分类") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(
                state.tags, { vm.updateTags(it) },
                label = { Text("标签（逗号分隔）") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            HorizontalDivider()
            MarkdownToolbar { prefix, suffix ->
                // Will be applied via vm when we get the EditText reference
                vm.insertMarkdown(prefix, suffix)
            }
            // Single EditText with real-time Markwon rendering
            AndroidView(
                factory = { context ->
                    EditText(context).apply {
                        textSize = 16f
                        setPadding(48, 32, 48, 32)
                        background = null
                        addTextChangedListener(MarkwonEditorTextWatcher.withProcess(markwonEditor))
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                s?.toString()?.let { vm.updateContent(it) }
                            }
                        })
                    }
                },
                update = { editText ->
                    // Only set text if it differs to avoid cursor jump
                    if (editText.text.toString() != state.content) {
                        editText.setText(state.content)
                        editText.setSelection(editText.text.length.coerceAtMost(state.content.length))
                    }
                    // Handle pending insertion
                    vm.pendingInsertion?.let { (prefix, suffix) ->
                        val start = editText.selectionStart.coerceAtLeast(0)
                        val end = editText.selectionEnd.coerceAtLeast(0)
                        val editable = editText.text
                        editable.replace(start.coerceAtMost(end), end.coerceAtMost(start), prefix + suffix)
                        editText.setSelection(start + prefix.length)
                        vm.pendingInsertion = null
                    }
                },
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
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
            "🔗" to ("[text](" to ")"), "📷" to ("![alt](" to ")"),
            ">" to ("> " to ""), "—" to ("- " to ""),
            "</>" to ("`" to "`"), "```" to ("\n```\n" to "\n```\n")
        ).forEach { (label, action) ->
            Tab(
                false,
                onClick = { onAction(action.first, action.second) },
                text = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
