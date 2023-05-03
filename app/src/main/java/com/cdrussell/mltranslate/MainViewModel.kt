package com.cdrussell.mltranslate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Loading
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.WaitingForInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow<TranslatedTextUiState>(WaitingForInput)
    val uiState: StateFlow<TranslatedTextUiState> = _uiState.asStateFlow()

    // Handle business logic
    fun translate(source: String) {
        _uiState.update { Loading }

        viewModelScope.launch {
            _uiState.update { (TranslatedTextUiState.Success(source.length.toString())) }
        }
    }

    sealed interface TranslatedTextUiState {
        object Loading : TranslatedTextUiState
        object WaitingForInput : TranslatedTextUiState
        data class Success(val translatedText: String) : TranslatedTextUiState
        data class Error(val errorMessage: String) : TranslatedTextUiState
    }
}