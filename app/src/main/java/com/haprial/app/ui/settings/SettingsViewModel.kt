package com.haprial.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haprial.app.data.api.HaprialApi
import com.haprial.app.data.model.StatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val api: HaprialApi) : ViewModel() {
    private val _stats = MutableStateFlow<StatsResponse?>(null)
    val stats: StateFlow<StatsResponse?> = _stats
    init { viewModelScope.launch { try { _stats.value = api.getStats().body() } catch (_: Exception) {} } }
}
