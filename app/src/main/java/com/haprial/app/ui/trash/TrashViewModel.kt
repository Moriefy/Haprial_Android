package com.haprial.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.TrashItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TrashState(val items: List<TrashItem> = emptyList(), val isLoading: Boolean = true, val error: String? = null)

class TrashViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(TrashState())
    val state: StateFlow<TrashState> = _state

    init { loadTrash() }

    fun loadTrash() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val r = api.getTrash()
                if (r.isSuccessful) _state.value = TrashState(r.body()?.items ?: emptyList(), false)
                else _state.value = _state.value.copy(isLoading = false, error = "加载失败")
            } catch (e: Exception) { _state.value = _state.value.copy(isLoading = false, error = e.message) }
        }
    }

    fun restore(id: Int) {
        viewModelScope.launch {
            try { api.restoreTrash(id); loadTrash() } catch (_: Exception) {}
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try { api.deleteTrash(id); loadTrash() } catch (_: Exception) {}
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try { api.emptyTrash(); loadTrash() } catch (_: Exception) {}
        }
    }
}
