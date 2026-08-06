package com.haprial.app.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.ImageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImageState(val images: List<ImageItem> = emptyList(), val folders: List<String> = emptyList(), val currentFolder: String = "", val isLoading: Boolean = true)

class ImageManagerViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(ImageState())
    val state: StateFlow<ImageState> = _state
    init { loadImages() }
    fun loadImages(folder: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, currentFolder = folder)
            try { val r = api.getImages(folder.ifEmpty { null }); if (r.isSuccessful) _state.value = _state.value.copy(r.body()?.images ?: emptyList(), r.body()?.folders ?: emptyList(), isLoading = false) }
            catch (_: Exception) { _state.value = _state.value.copy(isLoading = false) }
        }
    }
    fun enterFolder(name: String) { loadImages(if (_state.value.currentFolder.isEmpty()) name else "${_state.value.currentFolder}/$name") }
    fun goBack() { loadImages(_state.value.currentFolder.split("/").dropLast(1).joinToString("/")) }
    fun deleteImage(path: String) { viewModelScope.launch { try { api.deleteImage(path); loadImages(_state.value.currentFolder) } catch (_: Exception) {} } }
}
