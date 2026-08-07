package com.haprial.app.ui.editor

import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.haprial.app.ui.components.TitleBarButton
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Surface
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltApi
import org.koin.androidx.compose.koinViewModel

private data class MdBtn(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val prefix: String, val suffix: String)

private val mdButtons = listOf(
    MdBtn(Icons.Filled.FormatBold, "粗体", "**", "**"),
    MdBtn(Icons.Filled.FormatItalic, "斜体", "*", "*"),
    MdBtn(Icons.Filled.FormatStrikethrough, "删除线", "~~", "~~"),
    MdBtn(Icons.Filled.Code, "代码", "`", "`"),
    MdBtn(Icons.Filled.ShortText, "H2", "## ", ""),
    MdBtn(Icons.Filled.Title, "H3", "### ", ""),
    MdBtn(Icons.Filled.Link, "链接", "[text](", ")"),
    MdBtn(Icons.Filled.Image, "图片", "![alt](", ")"),
    MdBtn(Icons.Filled.FormatQuote, "引用", "> ", ""),
    MdBtn(Icons.Filled.FormatListBulleted, "列表", "- ", ""),
    MdBtn(Icons.Filled.DataObject, "代码块", "\n```\n", "\n```\n"),
    MdBtn(Icons.Filled.HorizontalRule, "分割线", "\n---\n", ""),
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

    var isPreview by remember { mutableStateOf(false) }
    var showMeta by remember { mutableStateOf(articleId == 0) }
    // 用 MutableState 存 EditText 引用，避免 recomposition 丢失
    val editTextState = remember { mutableStateOf<EditText?>(null) }
    // 标记是否正在从 ViewModel 同步到 EditText（防止循环）
    var syncingFromVm by remember { mutableStateOf(false) }

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

    Column(Modifier.fillMaxSize().background(SaltTheme.colors.background)) {

        // ═══ 标题栏 ═══
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TitleBarButton(onClick = onBack) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack), contentDescription = "返回")
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(if (articleId == 0) "写文章" else "编辑", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (state.isSaving) Text("保存中…", fontSize = 11.sp, color = SaltTheme.colors.subText)
            }
            TitleBarButton(onClick = { isPreview = !isPreview }) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(if (isPreview) Icons.Default.Edit else Icons.Default.Visibility), contentDescription = if (isPreview) "编辑" else "预览")
            }
            TitleBarButton(onClick = { vm.save("draft") }) {
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Save), contentDescription = "存草稿")
            }
            TitleBarButton(onClick = { vm.save("published") }) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SaltTheme.colors.highlight)
                else Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Publish), contentDescription = "发布", tint = SaltTheme.colors.highlight)
            }
        }

        // ═══ 元信息区 ═══
        AnimatedVisibility(visible = showMeta && !isPreview, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaField("标题", state.title, { vm.updateTitle(it) }, "文章标题")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetaField("日期", state.date, {}, "", readOnly = true, modifier = Modifier.weight(1f))
                    MetaField("分类", state.category, { vm.updateCategory(it) }, "tech", modifier = Modifier.weight(1f))
                }
                MetaField("标签", state.tags, { vm.updateTags(it) }, "逗号分隔")
                MetaField("摘要", state.excerpt, { vm.updateExcerpt(it) }, "可选", singleLine = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = { showMeta = false }, modifier = Modifier.height(32.dp)) {
                        Text("收起 ↑", fontSize = 12.sp, color = SaltTheme.colors.subText)
                    }
                }
            }
        }
        if (!showMeta && !isPreview) {
            Row(Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).clickable { showMeta = true }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.title.ifBlank { "无标题" }, fontSize = 13.sp, color = SaltTheme.colors.subText, modifier = Modifier.weight(1f), maxLines = 1)
                Text("${state.category} · ${state.date}", fontSize = 11.sp, color = SaltTheme.colors.subText.copy(alpha = 0.6f))
                Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.ExpandMore), contentDescription = null, modifier = Modifier.size(16.dp).padding(start = 4.dp), tint = SaltTheme.colors.subText)
            }
        }

        // ═══ 工具栏（仅编辑模式）═══
        AnimatedVisibility(visible = !isPreview, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(SaltTheme.colors.subBackground).border((0.5f).dp, SaltTheme.colors.stroke).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                mdButtons.forEach { btn ->
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            editTextState.value?.let { et ->
                                val s = et.selectionStart.coerceAtLeast(0)
                                val e = et.selectionEnd.coerceAtLeast(0)
                                val selStart = minOf(s, e); val selEnd = maxOf(s, e)
                                val selected = et.text.substring(selStart, selEnd)
                                val rep = if (selected.isNotEmpty()) "${btn.prefix}$selected${btn.suffix}" else "${btn.prefix}${btn.suffix}"
                                et.text.replace(selStart, selEnd, rep)
                                et.setSelection((if (selected.isNotEmpty()) selStart + rep.length else selStart + btn.prefix.length).coerceAtMost(et.text.length))
                                // 不调用 vm.updateContent，TextWatcher 会自动同步
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(btn.icon), contentDescription = btn.label, modifier = Modifier.size(18.dp), tint = SaltTheme.colors.text)
                    }
                }
            }
        }

        // ═══ 内容区 ═══
        Box(Modifier.fillMaxSize()) {
            if (isPreview) {
                // ── 预览 ──
                val previewScroll = rememberScrollState()
                Column(Modifier.fillMaxSize().verticalScroll(previewScroll).padding(horizontal = 20.dp, vertical = 16.dp)) {
                    if (state.title.isNotBlank()) {
                        Text(state.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("${state.date} · ${state.category}", fontSize = 13.sp, color = SaltTheme.colors.subText)
                        Spacer(Modifier.height(16.dp))
                    }
                    AndroidView(
                        factory = { c -> TextView(c).apply { textSize = 16f; setLineSpacing(6f, 1.3f); markwon.setMarkdown(this, state.content) } },
                        update = { tv -> if (tv.tag as? String != state.content) { tv.tag = state.content; markwon.setMarkdown(tv, state.content) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // ── 编辑 ──
                // 关键：用 key(articleId) 确保编辑器只创建一次
                key(articleId) {
                    AndroidView(
                        factory = { c ->
                            EditText(c).apply {
                                textSize = 16f; setPadding(36, 20, 36, 20); background = null
                                setLineSpacing(4f, 1.2f)
                                isVerticalScrollBarEnabled = true
                                isScrollbarFadingEnabled = true
                                // 设置初始内容
                                setText(state.content)
                                setSelection(state.content.length.coerceAtMost(text.length))
                                // TextWatcher：只在用户输入时同步到 ViewModel
                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                    override fun afterTextChanged(s: Editable?) {
                                        if (!syncingFromVm) {
                                            vm.updateContent(s?.toString() ?: "")
                                        }
                                    }
                                })
                            }
                        },
                        update = { et ->
                            editTextState.value = et
                            // 只在 ViewModel 内容被外部改变时同步（如加载文章、自动恢复）
                            val vmContent = state.content
                            if (et.text.toString() != vmContent && !et.hasFocus()) {
                                syncingFromVm = true
                                et.setText(vmContent)
                                et.setSelection(vmContent.length.coerceAtMost(et.text.length))
                                syncingFromVm = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 字数
            if (state.content.isNotEmpty()) {
                Text("${state.content.length}字", fontSize = 10.sp, color = SaltTheme.colors.subText.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
        }
    }
}

@Composable
private fun MetaField(label: String, value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, readOnly: Boolean = false, singleLine: Boolean = true) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = SaltTheme.colors.subText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SaltTheme.colors.background).border(0.5f.dp, SaltTheme.colors.stroke, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = if (singleLine) 8.dp else 6.dp)) {
            if (value.isEmpty()) Text(placeholder, color = SaltTheme.colors.subText.copy(alpha = 0.4f), fontSize = 13.sp)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onChange,
                enabled = !readOnly,
                singleLine = singleLine,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = SaltTheme.colors.text),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
