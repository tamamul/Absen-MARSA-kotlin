package com.marsa.absen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.AppSettings
import com.marsa.absen.data.local.SettingsStore
import com.marsa.absen.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore
) : ViewModel() {

    /** null = pengaturan belum terbaca (splash menunggu agar tidak ada kedipan tema). */
    val settings: StateFlow<AppSettings?> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch { store.setDynamicColor(value) }
    }

    fun setAmoled(value: Boolean) {
        viewModelScope.launch { store.setAmoled(value) }
    }
}
