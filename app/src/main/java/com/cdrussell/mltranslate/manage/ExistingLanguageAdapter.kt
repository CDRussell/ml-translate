package com.cdrussell.mltranslate.manage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cdrussell.mltranslate.databinding.ExistingLanguageRowItemBinding
import logcat.logcat

class ExistingLanguageAdapter(
    private val onDeleteLanguage: (languageCode: String) -> Unit,
) : RecyclerView.Adapter<ExistingLanguageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ExistingLanguageRowItemBinding) : RecyclerView.ViewHolder(binding.root)

    private var items = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ExistingLanguageRowItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        logcat {"getItemCount: ${items.size}"}
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.languageName.text = item
        holder.binding.deleteLanguageButton.setOnClickListener { onDeleteLanguage(item) }
    }

    fun updateItems(items: List<String>) {
        this.items = items
        notifyDataSetChanged()
    }
}