package com.haprial.app.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.*
import org.koin.androidx.compose.koinViewModel

// ── Markdown 工具栏动作定义（对齐网页版）──
private data class MdAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val prefix: String,
    val suffix: String
)

private val mdActions = listOf(
    MdAction(Icons.Filled.FormatBold, "粗体", "**", "**"),
    MdAction(Icons.Filled.FormatItalic, "斜体", "*", "*"),
    MdAction(Icons.Filled.FormatStrikethrough, "删除线", "~~", "~~"),
    MdAction(Icons.Filled.Code, "行内代码", "`", "`"),
    MdAction(Icons.Filled.Code, "代码块", "\n```\n", "\n```\n"),
    MdAction(Icons.Filled.ShortText, "H2", "## ", ""),
    MdAction(Icons.Filled.ShortText, "H3", "### ", ""),
    MdAction(Icons.Filled.Link, "链接", "[text](", ")"),
    MdAction(Icons.Filled.Image, "图片", "![alt](", ")"),
    MdAction(Icons.Filled.FormatQuote, "引用", "> ", ""),
    MdAction(Icons.Filled.FormatListBulleted, "无序列表", "- ", ""),
    MdAction(Icons.Filled.HorizontalRule, "分割线", "\n---\n", ""),
)

@OptIn(UnstableSaltApi::class)
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

    var showPreview by remember { mutableStateOf(false) }
    var showMeta by remember { mutableStateOf(articleId == 0) }

    // 保存 EditText 引用，用于工具栏直接操作
    val editTextRef = remember { mutableStateOf<EditText?>(null) }

    LaunchedEffect(articleId) { vm.loadArticle(articleId) }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(ctx, if (articleId == 0) "发布成功" else "保存成功", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    // ── 预览弹窗 ──
    if (showPreview) {
        Dialog(onDismissRequest = { showPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TitleBarButton(onClick = { showPreview = false }) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Close), contentDescription = "关闭")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("预览", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                AndroidView(
                    factory = { c ->
                        ScrollView(c).apply {
                            val tv = TextView(c).apply { textSize = 16f; setPadding(48, 24, 48, 48); setLineSpacing(8f, 1.3f) }
                            addView(tv)
                            markwon.setMarkdown(tv, vm.getContent())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // ── 主界面 ──
    Column(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {

        // ── 标题栏 ──
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TitleBarButton(onClick = onBack) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack), contentDescription = "返回")
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(if (articleId == 0) "写文章" else "编辑", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                if (state.isSaving) Text("保存中…", fontSize = 11.sp, color = SaltTheme.colors.subText)
                else if (state.isNew && articleId == 0) Text("自动保存已开启", fontSize = 11.sp, color = SaltTheme.colors.subText)
            }
            TitleBarButton(onClick = { showPreview = true }) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Visibility), contentDescription = "预览")
            }
            TitleBarButton(onClick = { vm.save("draft") }) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Save), contentDescription = "存草稿")
            }
            TitleBarButton(onClick = { vm.save("published") }) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SaltTheme.colors.highlight)
                else Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Publish), contentDescription = "发布", tint = SaltTheme.colors.highlight)
            }
        }

        // ── 元信息区 ──
        AnimatedVisibility(visible = showMeta, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorField(label = "标题", value = state.title, onChange = { vm.updateTitle(it) }, placeholder = "输入文章标题…")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField(label = "日期", value = state.date, onChange = {}, placeholder = "", readOnly = true, modifier = Modifier.weight(1f))
                    EditorField(label = "分类", value = state.category, onChange = { vm.updateCategory(it) }, placeholder = "tech", modifier = Modifier.weight(1f))
                }
                EditorField(label = "标签", value = state.tags, onChange = { vm.updateTags(it) }, placeholder = "用逗号分隔")
                EditorField(label = "摘要", value = state.excerpt, onChange = { vm.updateExcerpt(it) }, placeholder = "文章摘要（可选）", singleLine = false)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    androidx.compose.material3.TextButton(onClick = { showMeta = false }) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.ExpandLess), contentDescription = "收起", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("收起", style = SaltTheme.textStyles.sub)
                    }
                }
            }
        }
        if (!showMeta) {
            Box(Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).clickable { showMeta = true }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.title.ifBlank { "无标题" }} · ${state.category.ifBlank { "未分类" }}", style = SaltTheme.textStyles.sub, color = SaltTheme.colors.subText, modifier = Modifier.weight(1f))
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.ExpandMore), contentDescription = "展开", modifier = Modifier.size(18.dp), tint = SaltTheme.colors.subText)
                }
            }
        }

        // ── Markdown 工具栏 ──
        Row(
            Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).border((0.5f).dp, SaltTheme.colors.stroke).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            mdActions.forEach { action ->
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // 直接操作 EditText，不走 pendingInsertion
                        editTextRef.value?.let { et ->
                            val start = et.selectionStart.coerceAtLeast(0)
                            val end = et.selectionEnd.coerceAtLeast(0)
                            val selStart = minOf(start, end)
                            val selEnd = maxOf(start, end)
                            val selected = et.text.substring(selStart, selEnd)
                            val replacement = if (selected.isNotEmpty()) "${action.prefix}$selected${action.suffix}" else "${action.prefix}${action.suffix}"
                            et.text.replace(selStart, selEnd, replacement)
                            val newPos = if (selected.isNotEmpty()) selStart + replacement.length else selStart + action.prefix.length
                            et.setSelection(newPos.coerceAtMost(et.text.length))
                            // 同步到 ViewModel
                            vm.updateContent(et.text.toString())
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(action.icon), contentDescription = action.label, modifier = Modifier.size(20.dp), tint = SaltTheme.colors.text)
                }
            }
        }

        // ── 编辑器主体 ──
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { c ->
                    EditText(c).apply {
                        textSize = 16f; setPadding(40, 24, 40, 24); background = null
                        setLineSpacing(6f, 1.25f)
                        hint = "开始写作…"
                        addTextChangedListener(MarkwonEditorTextWatcher.withProcess(markwonEditor))
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) { s?.toString()?.let { vm.updateContent(it) } }
                        })
                    }
                },
                update = { et ->
                    editTextRef.value = et
                    // 只在内容真正变化且编辑器无焦点时同步（避免光标跳动）
                    if (et.text.toString() != state.content && !et.hasFocus()) {
                        et.setText(state.content)
                        et.setSelection(et.text.length.coerceAtMost(state.content.length))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 字数统计
            if (state.content.isNotEmpty()) {
                Text("${state.content.length}字", fontSize = 11.sp, color = SaltTheme.colors.subText.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
            }
        }
    }
}

/**
 * 编辑器元信息字段
 */
@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, readOnly: Boolean = false, singleLine: Boolean = true) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = SaltTheme.colors.subText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SaltTheme.colors.background).border(1.dp, SaltTheme.colors.stroke, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = if (singleLine) 10.dp else 8.dp)) {
            if (value.isEmpty()) Text(placeholder, color = SaltTheme.colors.subText.copy(alpha = 0.5f), fontSize = 14.sp)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                enabled = !readOnly,
                singleLine = singleLine,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SaltTheme.colors.text),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
