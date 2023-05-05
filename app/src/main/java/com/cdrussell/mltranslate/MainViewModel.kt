package com.cdrussell.mltranslate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    // Expose screen UI state
    private val _uiState = MutableStateFlow<TranslatedTextUiState>(TranslatedTextUiState.WaitingForInput)
    val uiState: StateFlow<TranslatedTextUiState> = _uiState.asStateFlow()

    private val _detectedLanguageState = MutableStateFlow<DetectedLanguageUiState>(DetectedLanguageUiState.Processing)
    val detectedLanguageState: StateFlow<DetectedLanguageUiState> = _detectedLanguageState.asStateFlow()

    private val _languageModelsAvailableState = MutableStateFlow<LanguageModelsAvailableState>(LanguageModelsAvailableState())
    val languageModelsAvailableState: StateFlow<LanguageModelsAvailableState> = _languageModelsAvailableState.asStateFlow()

    private val _validLanguagesSelectedState = MutableStateFlow<ValidLanguagesSelected>(ValidLanguagesSelected(false))
    val validLanguagesSelectedState: StateFlow<ValidLanguagesSelected> = _validLanguagesSelectedState.asStateFlow()

    private var predictedLanguageCode: String? = null

    init {
        viewModelScope.launch {
            getAvailableLanguages()
        }
    }

    suspend fun identifySourceLanguage(source: String) {
        withContext(Dispatchers.IO) {
            _detectedLanguageState.update { DetectedLanguageUiState.Processing }

            val language = app.languageDetector().detectLanguage(source)
            predictedLanguageCode = if (language.isSuccess) {
                language.getOrNull()
            } else {
                null
            }

            _detectedLanguageState.update {
                if (predictedLanguageCode != null) DetectedLanguageUiState.Success(predictedLanguageCode!!)
                else DetectedLanguageUiState.Error
            }
        }
    }

    suspend fun translate(source: String, inputLanguage: String, outputLanguage: String) {
        withContext(Dispatchers.IO) {
            _uiState.update { TranslatedTextUiState.Loading }

            viewModelScope.launch {
                val result = app.textTranslator().translate(source, inputLanguage, outputLanguage)
                _uiState.update {
                    if (result.isSuccess) TranslatedTextUiState.Success(result.getOrNull() ?: "")
                    else TranslatedTextUiState.TranslationError(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun getAvailableLanguages() {
        withContext(Dispatchers.IO) {
            val availableLanguages = app.textTranslator().getAvailableLanguages()
                .map { it.language }
            _languageModelsAvailableState.update { LanguageModelsAvailableState(availableLanguages) }
        }
    }

    fun languageSelectionChanged(inputLanguage: String, outputLanguage: String) {
        val supportedLanguages = languageModelsAvailableState.value.languages
        logcat { "Input now: $inputLanguage, Output now: $outputLanguage. Supported languages: $supportedLanguages"}
        if(supportedLanguages.contains(inputLanguage) && supportedLanguages.contains(outputLanguage)) {
            _validLanguagesSelectedState.update { ValidLanguagesSelected(true) }
        } else {
            _validLanguagesSelectedState.update { ValidLanguagesSelected(false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            getAvailableLanguages()
        }
    }

    sealed interface TranslatedTextUiState {
        object Loading : TranslatedTextUiState
        object WaitingForInput : TranslatedTextUiState
        data class Success(val translatedText: String) : TranslatedTextUiState
        data class TranslationError(val errorMessage: String) : TranslatedTextUiState
    }

    sealed interface DetectedLanguageUiState {
        data class Success(val languageCode: String) : DetectedLanguageUiState
        object Processing : DetectedLanguageUiState
        object Error : DetectedLanguageUiState
    }

    data class LanguageModelsAvailableState(val languages: List<String> = emptyList())
    data class ValidLanguagesSelected(val isValid: Boolean)


}