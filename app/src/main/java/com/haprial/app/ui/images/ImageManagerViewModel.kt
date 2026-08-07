package com.haprial.app.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.ImageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImageState(
    val images: List<ImageItem> = emptyList(),
    val folders: List<String> = emptyList(),
    val currentFolder: String = "",
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val error: String? = null,
    val uploadSuccess: Boolean = false
)

class ImageManagerViewModel(private val api: HaprialApi) : ViewModel() {
    private val _state = MutableStateFlow(ImageState())
    val state: StateFlow<ImageState> = _state
    init { loadImages() }
    fun loadImages(folder: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, currentFolder = folder, error = null)
            try {
                val r = api.getImages(folder.ifEmpty { null })
                if (r.isSuccessful) _state.value = _state.value.copy(r.body()?.images ?: emptyList(), r.body()?.folders ?: emptyList(), isLoading = false)
                else _state.value = _state.value.copy(isLoading = false, error = "加载失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }
    fun enterFolder(name: String) { loadImages(if (_state.value.currentFolder.isEmpty()) name else "${_state.value.currentFolder}/$name") }
    fun goBack() { loadImages(_state.value.currentFolder.split("/").dropLast(1).joinToString("/")) }
    fun deleteImage(path: String) {
        viewModelScope.launch {
            try {
                val resp = api.deleteImage(path)
                if (resp.isSuccessful) loadImages(_state.value.currentFolder)
                else _state.value = _state.value.copy(error = "删除失败")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun uploadImage(base64Data: String, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null, uploadSuccess = false)
            try {
                val body = mapOf(
                    "data" to base64Data,
                    "filename" to fileName,
                    "folder" to _state.value.currentFolder
                )
                val resp = api.uploadImage(body)
                if (resp.isSuccessful) {
                    _state.value = _state.value.copy(isUploading = false, uploadSuccess = true)
                    loadImages(_state.value.currentFolder)
                } else {
                    _state.value = _state.value.copy(isUploading = false, error = "上传失败")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isUploading = false, error = "上传失败: ${e.message}")
            }
        }
    }

    fun clearUploadSuccess() { _state.value = _state.value.copy(uploadSuccess = false) }
    fun clearError() { _state.value = _state.value.copy(error = null) }
}
