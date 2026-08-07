package com.haprial.app.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FriendsState(
    val friends: List<Friend> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class FriendsViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(FriendsState())
    val state: StateFlow<FriendsState> = _state

    init { loadFriends() }

    fun loadFriends() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val resp = api.getFriends()
                if (resp.isSuccessful) {
                    _state.value = FriendsState(resp.body()?.friends ?: emptyList(), isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "加载失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }
}
