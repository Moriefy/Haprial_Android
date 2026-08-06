package com.haprial.app.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.Comment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CommentState(val comments: List<Comment> = emptyList(), val pages: List<String> = emptyList(), val selectedPage: String = "", val isLoading: Boolean = true)

class CommentListViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(CommentState())
    val state: StateFlow<CommentState> = _state
    init { loadPages() }
    private fun loadPages() {
        viewModelScope.launch {
            try {
                val resp = api.getComments(limit = 1)
                if (resp.isSuccessful) { _state.value = _state.value.copy(pages = resp.body()?.pages ?: emptyList()); if (_state.value.pages.isNotEmpty()) loadComments(_state.value.pages[0]) }
            } catch (_: Exception) {}
        }
    }
    fun loadComments(page: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedPage = page, isLoading = true)
            try { val r = api.getComments(pageSlug = page); if (r.isSuccessful) _state.value = _state.value.copy(r.body()?.comments ?: emptyList(), isLoading = false) }
            catch (_: Exception) { _state.value = _state.value.copy(isLoading = false) }
        }
    }
    fun deleteComment(id: Int) { viewModelScope.launch { try { api.deleteComment(id); loadComments(_state.value.selectedPage) } catch (_: Exception) {} } }
    fun pinComment(id: Int) { viewModelScope.launch { try { api.pinComment(id); loadComments(_state.value.selectedPage) } catch (_: Exception) {} } }
}
