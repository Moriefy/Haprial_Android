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
    val error: String? = null,
    val successMsg: String? = null
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
                    _state.value = FriendsState(friends = resp.body()?.friends ?: emptyList(), isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "加载失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }

    fun addFriend(name: String, url: String, avatar: String, desc: String) {
        viewModelScope.launch {
            try {
                val body = mapOf("name" to name, "url" to url, "avatar" to avatar, "desc" to desc)
                val resp = api.createFriend(body)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(successMsg = "已添加")
                    loadFriends()
                } else {
                    _state.value = _state.value.copy(error = "添加失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "添加失败: ${e.message}")
            }
        }
    }

    fun updateFriend(id: Int, name: String, url: String, avatar: String, desc: String) {
        viewModelScope.launch {
            try {
                val body = mapOf("name" to name, "url" to url, "avatar" to avatar, "desc" to desc)
                val resp = api.updateFriend(id, body)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(successMsg = "已更新")
                    loadFriends()
                } else {
                    _state.value = _state.value.copy(error = "更新失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "更新失败: ${e.message}")
            }
        }
    }

    fun deleteFriend(id: Int) {
        viewModelScope.launch {
            try {
                val resp = api.deleteFriend(id)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(successMsg = "已删除")
                    loadFriends()
                } else {
                    _state.value = _state.value.copy(error = "删除失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun clearMessages() { _state.value = _state.value.copy(error = null, successMsg = null) }
}
