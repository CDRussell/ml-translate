package com.cdrussell.mltranslate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import logcat.asLog
import logcat.logcat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface TextTranslator {
    suspend fun translate(source: String, inputLanguage: String, outputLanguage:String): Result<String>
    suspend fun getAvailableLanguages(): List<TranslateRemoteModel>
    suspend fun deleteLanguageModel(languageCode: String)
    suspend fun downloadLanguage(languageCode: String): Boolean
}

class MachineLearningTranslator : TextTranslator {

    private var translator: Translator? = null

    override suspend fun translate(source: String, inputLanguage: String, outputLanguage:String): Result<String> {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(inputLanguage)
            .setTargetLanguage(outputLanguage)
            .build()
        val translator = Translation.getClient(options)
        val modelAvailable = ensureModelAvailable(translator)

        translator.use { translator ->
            return if (modelAvailable) {
                val result = translate(source, translator)
                if (result.isSuccess) {
                    Result.success(result.getOrElse { "" })
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } else {
                return Result.failure(Exception("Model not available"))
            }
        }
    }

    override suspend fun getAvailableLanguages(): List<TranslateRemoteModel> = suspendCoroutine { continuation ->
        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener {
                continuation.resume(it.toList().sortedBy { it.language })
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }

    override suspend fun deleteLanguageModel(languageCode: String) = suspendCoroutine<Unit> { continuation ->
        val modelManager = RemoteModelManager.getInstance()
        modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(languageCode).build())
            .addOnSuccessListener {
                logcat { "Deleted model for $languageCode"}
                continuation.resume(Unit) }
            .addOnFailureListener {
                logcat { it.asLog() }
                continuation.resume(Unit)
            }
    }

    override suspend fun downloadLanguage(languageCode: String) = suspendCoroutine<Boolean> { continuation ->
        val modelManager = RemoteModelManager.getInstance()
        modelManager.download(TranslateRemoteModel.Builder(languageCode).build(), DownloadConditions.Builder().build())
            .addOnSuccessListener {
                logcat { "Downloaded model for $languageCode"}
                continuation.resume(true)
            }
            .addOnFailureListener {
                logcat { it.asLog() }
                continuation.resume(true)
            }
    }

    private suspend fun translate(source: String, translator: Translator) = suspendCoroutine<Result<String>> { continuation ->
        translator.translate(source)
            .addOnSuccessListener {
                continuation.resume(Result.success(it))
            }
            .addOnFailureListener {
                continuation.resume(Result.failure(it))
            }
    }

    private suspend fun ensureModelAvailable(translator: Translator) = suspendCoroutine<Boolean> { continuation ->
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { exception ->
                continuation.resume(false)
            }
    }
}