package com.haprial.app.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.db.ArticleDao
import com.haprial.app.data.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ArticleListState(val articles: List<Article> = emptyList(), val isLoading: Boolean = true, val error: String? = null)

class ArticleListViewModel(private val api: HaprialApi, private val dao: ArticleDao) : ViewModel() {
    private val _state = MutableStateFlow(ArticleListState())
    val state: StateFlow<ArticleListState> = _state
    init { loadArticles() }

    fun loadArticles() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val resp = api.getArticles()
                if (resp.isSuccessful) _state.value = ArticleListState(resp.body()?.articles ?: emptyList(), false)
                else _state.value = _state.value.copy(isLoading = false, error = "加载失败")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = e.message) }
        }
    }
    fun deleteArticle(id: Int) { viewModelScope.launch { try { api.deleteArticle(id); loadArticles() } catch (_: Exception) {} } }
    fun togglePublish(id: Int) { viewModelScope.launch { try { api.togglePublish(id); loadArticles() } catch (_: Exception) {} } }
}
