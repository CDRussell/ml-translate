package com.cdrussell.mltranslate.webview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cdrussell.mltranslate.TextTranslator
import com.cdrussell.mltranslate.textTranslator

class WebViewModel(private val app: Application) : AndroidViewModel(app) {

    private val textTranslator : TextTranslator = app.textTranslator()

}