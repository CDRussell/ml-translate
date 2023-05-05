package com.cdrussell.mltranslate.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cdrussell.mltranslate.MachineLearningTranslator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat

class ManageLanguageViewModel() : ViewModel() {

    private val translator: MachineLearningTranslator = MachineLearningTranslator()

    private val _uiState = MutableStateFlow<LanguageState>(LanguageState(listOf()))
    val uiState: StateFlow<LanguageState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchLanguageListUpdate()
        }
    }

    private suspend fun fetchLanguageListUpdate() {
        val languages = translator.getAvailableLanguages().map { it.language }
        _uiState.update { LanguageState(list = languages) }
    }

    fun deleteLanguageModel(languageCode: String) {
        logcat { "Deleting language model for $languageCode"}
        viewModelScope.launch {
            translator.deleteLanguageModel(languageCode)
            logcat { "Deleted language model for $languageCode" }
            fetchLanguageListUpdate()
        }
    }

    fun downloadLanguage(languageCode: String) {
        viewModelScope.launch {
            val success = translator.downloadLanguage(languageCode)
            if(success) {
                fetchLanguageListUpdate()
            }
        }
    }

    data class LanguageState(val list: List<String>)
}