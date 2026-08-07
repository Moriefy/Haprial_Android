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
            try {
                val resp = api.restoreTrash(id)
                if (resp.isSuccessful) loadTrash()
                else _state.value = _state.value.copy(error = "恢复失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "恢复失败: ${e.message}")
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                val resp = api.deleteTrash(id)
                if (resp.isSuccessful) loadTrash()
                else _state.value = _state.value.copy(error = "删除失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                val resp = api.emptyTrash()
                if (resp.isSuccessful) loadTrash()
                else _state.value = _state.value.copy(error = "清空失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "清空失败: ${e.message}")
            }
        }
    }
}
