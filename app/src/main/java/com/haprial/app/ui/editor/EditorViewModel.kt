package com.haprial.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.db.ArticleDao
import com.haprial.app.data.db.DraftEntity
import com.haprial.app.data.model.ArticleCreateRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorState(
    val title: String = "",
    val content: String = "",
    val tags: String = "",
    val category: String = "tech",
    val excerpt: String = "",
    val date: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true
)

class EditorViewModel(private val api: HaprialApi, private val dao: ArticleDao) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state
    private var articleId = 0
    private var autoSaveJob: Job? = null

    // 工具栏 Markdown 插入请求
    @Volatile
    var pendingInsertion: Pair<String, String>? = null

    fun loadArticle(id: Int) {
        articleId = id
        if (id == 0) {
            _state.value = EditorState(isNew = true, date = today())
            startAutoSave()
            return
        }
        viewModelScope.launch {
            try {
                val resp = api.getArticle(id)
                if (resp.isSuccessful) {
                    val a = resp.body()?.article ?: return@launch
                    _state.value = EditorState(
                        title = a.title,
                        content = a.content ?: "",
                        tags = a.tagList().joinToString(", "),
                        category = a.category,
                        excerpt = a.excerpt,
                        date = a.date,
                        isNew = false
                    )
                }
            } catch (_: Exception) {}
            startAutoSave()
        }
    }

    fun updateTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun updateContent(v: String) { _state.value = _state.value.copy(content = v) }
    fun updateTags(v: String) { _state.value = _state.value.copy(tags = v) }
    fun updateCategory(v: String) { _state.value = _state.value.copy(category = v) }
    fun updateExcerpt(v: String) { _state.value = _state.value.copy(excerpt = v) }
    fun getContent(): String = _state.value.content

    fun insertMarkdown(prefix: String, suffix: String) {
        pendingInsertion = prefix to suffix
        // 触发 recomposition 让 AndroidView 的 update 块处理插入
        _state.value = _state.value.copy()
    }

    fun save(status: String = "draft") {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(error = "请输入标题")
            return
        }
        if (s.content.isBlank()) {
            _state.value = s.copy(error = "请输入内容")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val tags = s.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val body = ArticleCreateRequest(s.title, s.date, tags, s.category, s.excerpt, s.content, status)
                if (articleId == 0) {
                    val r = api.createArticle(body)
                    if (r.isSuccessful) {
                        articleId = r.body()?.id ?: 0
                        _state.value = _state.value.copy(isSaving = false, isSaved = true, isNew = false)
                        dao.deleteDraft(0)
                    } else {
                        _state.value = _state.value.copy(isSaving = false, error = "保存失败，请重试")
                    }
                } else {
                    val r = api.updateArticle(articleId, body)
                    if (r.isSuccessful) {
                        _state.value = _state.value.copy(isSaving = false, isSaved = true)
                        dao.deleteDraft(articleId)
                    } else {
                        _state.value = _state.value.copy(isSaving = false, error = "保存失败，请重试")
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = "网络错误: ${e.message}")
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                val s = _state.value
                if (s.title.isNotBlank() || s.content.isNotBlank()) {
                    try {
                        dao.saveDraft(DraftEntity(articleId, s.title, s.content, s.tags, s.category, s.excerpt, s.date))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCleared() {
        autoSaveJob?.cancel()
        super.onCleared()
    }
}
