package com.cdrussell.mltranslate.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.cdrussell.mltranslate.databinding.ActivityWebBinding
import com.cdrussell.mltranslate.languageDetector
import com.cdrussell.mltranslate.textTranslator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import logcat.LogPriority.VERBOSE
import logcat.logcat
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityWebBinding
    private lateinit var viewModel: WebViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = viewModel()

        setSupportActionBar(binding.toolbar)

        configureWebView()
        configureTranslator()
        //binding.webView.loadUrl("https://fill.dev/")
        binding.webView.loadUrl("https://www.theguardian.com/world/2023/may/05/serbia-eight-killed-in-second-mass-shooting-in-days-with-attacker-on-the-run/")
    }

    private fun configureTranslator() {
        binding.fab.setOnClickListener {
            logcat { "Translate ${binding.webView.url}" }

            lifecycleScope.launch {
                translateWebViewContent(binding.webView, "es")
            }
        }
    }

    private suspend fun translateWebViewContent(webView: WebView, targetLanguage: String) {
        val doc = parseHtml()
        val paragraphs = doc.body().select("p")

        val sourceLanguage = detectSourceLanguage(paragraphs)
        if (sourceLanguage == targetLanguage) {
            logcat { "Source and target languages are the same, nothing to do" }
            return
        }

        val replacements = buildReplacementPairs(paragraphs, sourceLanguage,targetLanguage)
        logcat { "Have ${replacements.size} replacements to make" }

        replacements.forEach { replacement ->
            binding.webView.evaluateJavascript(
                """
                        (() => {
                           const element = document.querySelector("${replacement.first.cssSelector().removePrefix("selector: ")}");
                           if (element) {
                                element.textContent = "${replacement.second}"
                                element.classList.add('animate');
                                
                                setTimeout(() => {
                                    element.classList.remove('animate');
                                }, 1000);
                           }
                        })()
                    """.trimIndent(), null
            )
        }

        Snackbar.make(binding.fab, "Translated page from $sourceLanguage to $targetLanguage", Snackbar.LENGTH_LONG)
            //.setAction("Action", null)
            .show()
    }

    private suspend fun buildReplacementPairs(
        paragraphs: Elements,
        sourceLanguage: String,
        targetLanguage: String
    ): MutableList<Pair<Element, String>> {
        val replacements = mutableListOf<Pair<Element, String>>()
        paragraphs.forEach { p ->
            val translated = translate(p.html(), sourceLanguage, targetLanguage)
            val selector = runCatching { p.cssSelector()}.getOrNull()

            selector?.let {
                logcat { "Translated: $translated for selector: $it" }
                replacements.add(Pair(p, translated))
            }
        }
        return replacements
    }

    private suspend fun detectSourceLanguage(textElements: Elements): String {
        val combined = StringBuilder()
        textElements.forEach { p ->
            combined.append(p.html())
            combined.append("\n")
        }

        val combinedProse = combined.toString()
        logcat(VERBOSE) { "Combined text on page:\n\n[$combinedProse]\n" }

        val result = languageDetector().detectLanguage(combinedProse)
        return if (result.isSuccess) {
            logcat { "Detected language: ${result.getOrNull()}" }
            return result.getOrElse { "en" }
        } else {
            logcat { "Failed to detect source language; assuming 'en'" }
            return "en"
        }
    }

    private suspend fun parseHtml(): Document {
        logcat { "Extracting HTML" }
        val html = extractHtml()
        logcat { "Got HTML" }
        val doc = Jsoup.parse(html, "UTF-8")
        logcat { "Parsed HTML" }
        return doc
    }

    private suspend fun translate(sourceHtml: String, sourceLanguage: String, targetLanguage: String): String {
        val translation = textTranslator().translate(sourceHtml, sourceLanguage, targetLanguage)
        return if (translation.isSuccess) {
            translation.getOrElse { sourceHtml }
        } else {
            logcat { "Translation failed" }
            sourceHtml
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        with(binding.webView) {
            webChromeClient = chromeClient
            webViewClient = wvClient
            settings.javaScriptEnabled = true
            addJavascriptInterface(AndroidBridge(), "Android")
        }
    }

    private val chromeClient = object : WebChromeClient() {

    }

    private class AndroidBridge {
        @JavascriptInterface
        fun html(message: String) {
            logcat { message }
        }
    }

    private val wvClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            logcat { "Page started loading: $url" }
        }

        override fun onPageFinished(webView: WebView?, url: String?) {
            super.onPageFinished(webView, url)
            webView ?: return

            logcat { "Page finished loading: $url" }
        }
    }

    private suspend fun extractHtml() = suspendCoroutine<String> { continuation ->
        binding.webView.evaluateJavascript(
            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
            ValueCallback<String?> { html ->
                //logcat { "HTML: \n\n$html\n\n"}
                val unencodedHtml = StringEscapeUtils.unescapeJava(html)
                logcat(VERBOSE) { "HTML: \n\n$unencodedHtml\n\n" }
                continuation.resume(unencodedHtml)
            }
        )

        // continuation.resume("""
        //     <html>
        //         <head>
        //             <title>Test</title>
        //         </head>
        //         <body>
        //             <p>Foo</p>
        //             <p>Bar</p>
        //         </body>
        //     </html>
        // """.trimIndent())
    }

    private fun viewModel(): WebViewModel {
        val viewModel: WebViewModel by viewModels()
        return viewModel
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, WebActivity::class.java)
        }
    }
}