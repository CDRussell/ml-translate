package com.cdrussell.mltranslate.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cdrussell.mltranslate.databinding.ActivityManageLanguageBinding
import kotlinx.coroutines.launch

class ManageLanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageLanguageBinding
    private lateinit var viewModel: ManageLanguageViewModel

    private val adapter = ExistingLanguageAdapter(onDeleteLanguage = {
        viewModel.deleteLanguageModel(it)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialiseViewModel()

        binding = ActivityManageLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureRecyclerView()
        observeViewModel()
        configureUiEventHandlers()
    }

    private fun configureUiEventHandlers() {
        binding.addLanguageButton.setOnClickListener {
            val input = EditText(this)

            AlertDialog.Builder(this)
                .setTitle("Add language")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    viewModel.downloadLanguage(input.text.toString())
                }
                .show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    adapter.updateItems(it.list)
                }
            }
        }
    }

    private fun initialiseViewModel() {
        val x: ManageLanguageViewModel by viewModels()
        viewModel = x
    }

    private fun configureRecyclerView() {
        binding.existingLanguageRecyclerView.adapter = adapter
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, ManageLanguageActivity::class.java)
        }
    }
}