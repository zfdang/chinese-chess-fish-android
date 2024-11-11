package com.zfdang.chess.views

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.readystatesoftware.android.sqliteassethelper.BuildConfig
import com.zfdang.chess.R

class WebviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // bind textViewVersion
        val textViewVersion: TextView = findViewById(R.id.textViewVersion)

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val datetime = java.util.Date(packageInfo.lastUpdateTime)
        // convert datetime to string with format YYYY-MM-DD HH:MM:SS
        val datetimeString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(datetime)
        textViewVersion.text = "当前版本: ${packageInfo.versionCode}\n" + "编译时间: $datetimeString"

        // bind imageButton and set click listener
        val imageButtonBack: ImageButton = findViewById(R.id.imageButtonBack)
        imageButtonBack.setOnClickListener {
            onBackPressed()
        }

        val webView: WebView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        val url = intent.getStringExtra("url") ?: "https://fish.zfdang.com"
        webView.loadUrl(url)
    }


    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webview)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}