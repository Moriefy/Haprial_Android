package com.haprial.app.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.Comment
import com.haprial.app.data.model.CommentPostRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CommentState(
    val comments: List<Comment> = emptyList(),
    val pages: List<String> = emptyList(),
    val selectedPage: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val slugTitleMap: Map<String, String> = emptyMap(),
    val replyingTo: Comment? = null,
    val replyContent: String = "",
    val replyNickname: String = ""
)

class CommentListViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(CommentState())
    val state: StateFlow<CommentState> = _state

    init {
        viewModelScope.launch {
            loadArticleTitles()
            loadPages()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadArticleTitles()
            if (_state.value.selectedPage.isNotEmpty()) loadComments(_state.value.selectedPage)
            else loadPages()
        }
    }

    private suspend fun loadArticleTitles() {
        try {
            val articles = api.getArticles().body()?.articles ?: emptyList()
            val map = articles.associate { "/posts/${it.slug}/" to it.title }
            _state.value = _state.value.copy(slugTitleMap = map)
        } catch (_: Exception) {}
    }

    private suspend fun loadPages() {
        try {
            val resp = api.getComments(limit = 1)
            if (resp.isSuccessful) {
                _state.value = _state.value.copy(pages = resp.body()?.pages ?: emptyList())
                if (_state.value.pages.isNotEmpty()) loadComments(_state.value.pages[0])
            }
        } catch (_: Exception) {}
    }

    fun loadComments(page: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedPage = page, isLoading = true)
            try {
                val r = api.getComments(pageSlug = page)
                if (r.isSuccessful) _state.value = _state.value.copy(comments = r.body()?.comments ?: emptyList(), isLoading = false)
            } catch (_: Exception) { _state.value = _state.value.copy(isLoading = false) }
        }
    }

    fun deleteComment(id: Int) {
        viewModelScope.launch {
            try {
                val resp = api.deleteComment(id)
                if (resp.isSuccessful) loadComments(_state.value.selectedPage)
                else _state.value = _state.value.copy(error = "删除失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun pinComment(id: Int) {
        viewModelScope.launch {
            try {
                val resp = api.pinComment(id)
                if (resp.isSuccessful) loadComments(_state.value.selectedPage)
                else _state.value = _state.value.copy(error = "置顶失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "置顶失败: ${e.message}")
            }
        }
    }

    fun likeComment(id: Int) {
        viewModelScope.launch {
            try {
                api.likeComment(id)
                loadComments(_state.value.selectedPage)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "点赞失败: ${e.message}")
            }
        }
    }

    fun startReply(comment: Comment) {
        _state.value = _state.value.copy(
            replyingTo = comment,
            replyContent = "",
            replyNickname = "Moriefy"
        )
    }

    fun cancelReply() {
        _state.value = _state.value.copy(replyingTo = null, replyContent = "", replyNickname = "")
    }

    fun updateReplyContent(v: String) { _state.value = _state.value.copy(replyContent = v) }
    fun updateReplyNickname(v: String) { _state.value = _state.value.copy(replyNickname = v) }

    fun submitReply() {
        val s = _state.value
        val parent = s.replyingTo ?: return
        if (s.replyContent.isBlank() || s.replyNickname.isBlank()) return
        viewModelScope.launch {
            try {
                val page = s.selectedPage
                val request = CommentPostRequest(
                    page = page,
                    parentId = parent.id,
                    depth = parent.depth + 1,
                    nickname = s.replyNickname,
                    email = "3518972914@qq.com",
                    website = "https://pluslogic.eu.org",
                    content = s.replyContent
                )
                val resp = api.postComment(request)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(replyingTo = null, replyContent = "", replyNickname = "")
                    loadComments(s.selectedPage)
                } else {
                    _state.value = _state.value.copy(error = "回复失败: ${resp.body()?.error ?: "服务器错误"}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "回复失败: ${e.message}")
            }
        }
    }

    fun getPageTitle(page: String): String {
        // 尝试多种格式匹配
        val map = _state.value.slugTitleMap
        return map[page]                              // 精确匹配 /posts/slug/
            ?: map["/posts/$page/"]                   // 补全前缀
            ?: map[page.removeSuffix("/")]            // 去掉尾部斜杠
            ?: map["${page}/"]                        // 加上尾部斜杠
            ?: page.removePrefix("/posts/").removeSuffix("/")  // 回退到 slug
    }
}
