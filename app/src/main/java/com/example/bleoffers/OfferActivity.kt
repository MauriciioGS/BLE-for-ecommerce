package com.example.bleoffers

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.bleoffers.databinding.ActivityOfferBinding


class OfferActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfferBinding

    private var offerUrl: String? = null
    private var codeOffer: String? = null

    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                val intent = Intent(this@OfferActivity, MainActivity::class.java)
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        intent.extras?.let {
            offerUrl = it.getString("urlOf", "")
            codeOffer = it.getString("codeOf", "")
        }

        initUI()
        initWebView()
    }

    private fun initUI() {
        if (!offerUrl.isNullOrBlank() && !codeOffer.isNullOrBlank()) {
            binding.tvTitle.text = getString(R.string.txt_offer)
            binding.chipCodeOf.text = codeOffer
            initWebView()
        }

        binding.chipCodeOf.setOnLongClickListener(View.OnLongClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("code", binding.chipCodeOf.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.txt_copied), Toast.LENGTH_SHORT).show()
            true
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() { }

        binding.webView.webViewClient = object : WebViewClient() { }

        binding.webView.settings.javaScriptEnabled = true

        binding.webView.loadUrl(offerUrl!!)
    }
}