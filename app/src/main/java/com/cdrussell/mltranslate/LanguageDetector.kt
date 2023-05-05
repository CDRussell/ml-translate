package com.cdrussell.mltranslate

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import logcat.asLog
import logcat.logcat
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface LanguageDetector {
    suspend fun detectLanguage(source: String): Result<String>
}

class MachineLearningLanguageDetector : LanguageDetector {

    private val languageIdentifier = LanguageIdentification.getClient()

    override suspend fun detectLanguage(source: String) = suspendCoroutine<Result<String>> { continuation ->

        languageIdentifier.identifyLanguage(source)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    logcat { "Can't identify language" }
                    continuation.resume(failure(Exception("Can't identify language")))
                } else {
                    logcat { "Language: $languageCode" }
                    continuation.resume(success(languageCode))
                }
            }
            .addOnFailureListener {
                logcat { it.asLog() }
                continuation.resume(failure(it))
            }


    }
}