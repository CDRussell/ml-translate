package com.cdrussell.mltranslate

import android.app.Application
import android.content.Context
import logcat.AndroidLogcatLogger
import logcat.LogPriority

class MLTranslateApplication : Application() {

    lateinit var translator: TextTranslator
    lateinit var languageDetector: LanguageDetector

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        translator = MachineLearningTranslator()
        languageDetector = MachineLearningLanguageDetector()
    }
}

fun Context.app(): MLTranslateApplication = applicationContext as MLTranslateApplication
fun Context.languageDetector(): LanguageDetector = app().languageDetector
fun Context.textTranslator(): TextTranslator = app().translator