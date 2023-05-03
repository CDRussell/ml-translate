package com.cdrussell.mltranslate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Error
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Loading
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.Success
import com.cdrussell.mltranslate.MainViewModel.TranslatedTextUiState.WaitingForInput
import com.cdrussell.mltranslate.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val viewModel: MainViewModel by viewModels()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { viewMode ->
                    when(viewMode) {
                        is Loading -> { binding.output.text = "Loading ⏳"}
                        is Error -> binding.output.text = "Error: ${viewMode.errorMessage}"
                        is Success -> binding.output.text = viewMode.translatedText
                        WaitingForInput -> binding.output.text = "Waiting for text to translate"
                    }
                }
            }
        }

        binding.translateButton.setOnClickListener {
            viewModel.translate(binding.input.text.toString())
        }
    }
}