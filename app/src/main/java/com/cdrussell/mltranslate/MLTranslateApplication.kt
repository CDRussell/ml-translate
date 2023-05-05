package com.cdrussell.mltranslate

import android.app.Application
import android.content.Context
import logcat.AndroidLogcatLogger
import logcat.LogPriority

class MLTranslateApplication : Application() {

    lateinit var translator: TextTranslator
    lateinit var languageDetector: LanguageDetector
    lateinit var preferences: Preferences

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        preferences = SharedPreferencesPreferences(this)
        translator = MachineLearningTranslator()
        languageDetector = MachineLearningLanguageDetector()
    }
}

fun Context.app(): MLTranslateApplication = applicationContext as MLTranslateApplication
fun Context.languageDetector(): LanguageDetector = app().languageDetector
fun Context.textTranslator(): TextTranslator = app().translator
fun Context.preferences(): Preferences = app().preferences

interface Preferences {
    fun preferredOutputLanguage(): String?
    fun setPreferredOutputLanguage(language: String)
}

class SharedPreferencesPreferences(private val context: Context) : Preferences {

    private val sharedPreferences = context.getSharedPreferences("MLTranslate", Context.MODE_PRIVATE)

    override fun preferredOutputLanguage(): String? {
        return sharedPreferences.getString("preferredOutputLanguage", null)
    }

    override fun setPreferredOutputLanguage(language: String) {
        sharedPreferences.edit().putString("preferredOutputLanguage", language).apply()
    }
}