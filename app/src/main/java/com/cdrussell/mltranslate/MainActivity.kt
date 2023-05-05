package com.cdrussell.mltranslate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.TranslationError
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Loading
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Success
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.WaitingForInput
import com.cdrussell.mltranslate.databinding.ActivityMainBinding
import com.cdrussell.mltranslate.manage.ManageLanguageActivity
import com.cdrussell.mltranslate.webview.WebActivity
import kotlinx.coroutines.launch
import logcat.logcat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private suspend fun translate(source: String) {
        viewModel.identifySourceLanguage(source)
        viewModel.translate(source, binding.inputLanguageAutoComplete.text.toString(), binding.outputLanguageAutoComplete.text.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = viewModel()

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        configureViewModelObservers()
        configureUiEventHandlers()

        startActivity(WebActivity.createIntent(this))
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    private fun configureUiEventHandlers() {
        binding.inputLanguageAutoComplete.setOnItemClickListener { _, _, _, _ ->
            logcat { "Selected input language: ${binding.inputLanguageAutoComplete.text}" }
            languageSelectionChange()
        }

        binding.outputLanguageAutoComplete.setOnItemClickListener { _, _, _, _ ->
            logcat { "Selected output language: ${binding.outputLanguageAutoComplete.text}" }
            languageSelectionChange()
        }

        binding.manageLanguagesButton.setOnClickListener {
            startActivity(ManageLanguageActivity.createIntent(this))
        }

        binding.swapLanguageButton.setOnClickListener {
            val oldInput = binding.inputLanguageAutoComplete.text.toString()
            val oldOutput = binding.outputLanguageAutoComplete.text.toString()

            binding.inputLanguageAutoComplete.setText(oldOutput)
            binding.outputLanguageAutoComplete.setText(oldInput)

            languageSelectionChange()
        }

        binding.webViewButton.setOnClickListener {
            startActivity(WebActivity.createIntent(this))
        }
    }

    private fun languageSelectionChange() {
        viewModel.languageSelectionChanged(binding.inputLanguageAutoComplete.text.toString(), binding.outputLanguageAutoComplete.text.toString())

        lifecycleScope.launch {
            viewModel.translate(binding.input.text.toString(), binding.inputLanguageAutoComplete.text.toString(), binding.outputLanguageAutoComplete.text.toString())
        }
    }

    private fun configureViewModelObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { viewMode -> onTranslatedTextUpdate(viewMode) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detectedLanguageState.collect { onDetectedLanguageStateUpdate(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.languageModelsAvailableState.collect { onLanguageAvailableUpdate(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.validLanguagesSelectedState.collect { onValidityOfChosenLanguagesUpdate(it.isValid) }
            }
        }
    }

    private fun onValidityOfChosenLanguagesUpdate(isValid: Boolean) {
        logcat { "onValidityOfChosenLanguagesUpdate: $isValid" }
        if (isValid) {
            binding.input.addTextChangedListener(inputTextWatcher)
            lifecycleScope.launch {
                translate(binding.input.text.toString())
            }
        } else {
            binding.input.removeTextChangedListener(inputTextWatcher)
        }
    }

    private fun onLanguageAvailableUpdate(state: MainViewModel.LanguageModelsAvailableState) {
        logcat { "onLanguageAvailableUpdate: ${state.languages.size} available ${state.languages.joinToString()}" }

        val adapter = ArrayAdapter<String>(this, R.layout.language_selection_item, state.languages.toList())
        binding.inputLanguageAutoComplete.setAdapter(adapter)
        binding.outputLanguageAutoComplete.setAdapter(adapter)

        val enabled = state.languages.isNotEmpty()
        binding.inputLanguageSpinner.isEnabled = enabled
        binding.inputLanguageSpinner.isEnabled = enabled
    }

    private fun onDetectedLanguageStateUpdate(state: MainViewModel.DetectedLanguageUiState) {
        binding.detectedLanguage.text = when (state) {
            MainViewModel.DetectedLanguageUiState.Error -> "Can't identify language"
            MainViewModel.DetectedLanguageUiState.Processing -> "Will predict language when input changes"
            is MainViewModel.DetectedLanguageUiState.Success -> "Predicted: ${state.languageCode}"
        }
    }

    private fun onTranslatedTextUpdate(viewMode: MainViewModel.TranslatedTextUiState) {
        when (viewMode) {
            is Loading -> binding.output.text = "Loading ⏳"
            is TranslationError -> binding.output.text = "Error: ${viewMode.errorMessage}"
            is Success -> binding.output.text = viewMode.translatedText
            WaitingForInput -> binding.output.text = "Waiting for text to translate"
        }
    }

    private fun viewModel(): MainViewModel {
        val viewModel: MainViewModel by viewModels()
        return viewModel
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            logcat { "Text changed: $text" }
            val source = text.toString()
            lifecycleScope.launch {
                translate(source)
            }
        }

        override fun afterTextChanged(text: Editable?) {
        }
    }

}