package co.median.android

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator

class InAppWebViewActivity : AppCompatActivity() {

    private lateinit var webToolbar: MaterialToolbar
    private lateinit var webProgressBar: LinearProgressIndicator
    private lateinit var webView: WebView
    private lateinit var webErrorContainer: LinearLayout
    private lateinit var webRetryBtn: Button

    private var targetUrl: String = ""
    private var jobTitle: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_webview)

        targetUrl = intent.getStringExtra("url") ?: "https://www.google.com"
        jobTitle = intent.getStringExtra("title") ?: "Job Application"

        webToolbar = findViewById(R.id.webToolbar)
        webProgressBar = findViewById(R.id.webProgressBar)
        webView = findViewById(R.id.inAppWebView)
        webErrorContainer = findViewById(R.id.webErrorContainer)
        webRetryBtn = findViewById(R.id.webRetryBtn)

        webToolbar.title = jobTitle
        try {
            val uri = Uri.parse(targetUrl)
            webToolbar.subtitle = uri.host ?: targetUrl
        } catch (_: Exception) {}

        webToolbar.setNavigationOnClickListener {
            finish()
        }

        // Configure WebView Settings securely
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowFileAccess = false
            allowContentAccess = false
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    webProgressBar.visibility = View.VISIBLE
                    webProgressBar.progress = newProgress
                } else {
                    webProgressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    webToolbar.title = title
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webErrorContainer.visibility = View.GONE
                webProgressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webProgressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    webErrorContainer.visibility = View.VISIBLE
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return handleUrlScheme(url)
            }
        }

        webRetryBtn.setOnClickListener {
            webErrorContainer.visibility = View.GONE
            webView.reload()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        loadTargetUrl()
    }

    private fun loadTargetUrl() {
        var urlToLoad = targetUrl.trim()
        if (!urlToLoad.startsWith("http://") && !urlToLoad.startsWith("https://")) {
            urlToLoad = "https://$urlToLoad"
        }
        webView.loadUrl(urlToLoad)
    }

    private fun handleUrlScheme(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: ""
            if (scheme == "http" || scheme == "https") {
                false // Allow loading inside WebView
            } else {
                // External scheme (tel, mailto, whatsapp, intent)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                true
            }
        } catch (_: Exception) {
            true
        }
    }
}
