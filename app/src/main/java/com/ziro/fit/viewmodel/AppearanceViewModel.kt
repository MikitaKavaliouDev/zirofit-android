package com.ziro.fit.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedLanguage: String = "en"
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("appearance_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppearanceUiState())
    val uiState: StateFlow<AppearanceUiState> = _uiState.asStateFlow()

    init {
        loadPrefs()
    }

    private fun loadPrefs() {
        val themeName = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val lang = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        _uiState.update {
            it.copy(
                themeMode = try { ThemeMode.valueOf(themeName) } catch (_: Exception) { ThemeMode.SYSTEM },
                selectedLanguage = lang
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _uiState.update { it.copy(selectedLanguage = lang) }
    }

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "selected_language"
    }
}
