// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "About"

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val btnRepository = findViewById<MaterialButton>(R.id.btnRepository)
        val btnAuthor = findViewById<MaterialButton>(R.id.btnAuthor)
        val btnDisclaimer = findViewById<MaterialButton>(R.id.btnDisclaimer)

        btnRepository.setOnClickListener {
            openUrl("https://github.com/smartlegionlab/smart-password-manager-android")
        }

        btnAuthor.setOnClickListener {
            openUrl("https://github.com/smartlegionlab/")
        }

        btnDisclaimer.setOnClickListener {
            openUrl("https://github.com/smartlegionlab/smart-password-manager-android/blob/master/DISCLAIMER.md")
        }

        setupExpandableCards()
    }

    private fun setupExpandableCards() {
        setupExpandableCard(
            R.id.cardPython, R.id.btnPython, R.id.expandableContentPython,
            "SmartPassLib - Python implementation",
            "Cross-platform deterministic password generator. Compatible with all other implementations.\n\n" +
                    "• SHA-256 based key derivation\n• 30 iterations for private key\n• 60 iterations for public key\n• Password length: 12-1000 characters",
            "https://github.com/smartlegionlab/smartpasslib"
        )

        setupExpandableCard(
            R.id.cardJavaScript, R.id.btnJavaScript, R.id.expandableContentJavaScript,
            "SmartPassLib - JavaScript implementation",
            "Complete JavaScript/TypeScript port with full compatibility.\n\n" +
                    "• Works in browsers and Node.js\n• Same deterministic algorithm\n• TypeScript definitions included",
            "https://github.com/smartlegionlab/smartpasslib-js"
        )

        setupExpandableCard(
            R.id.cardKotlin, R.id.btnKotlin, R.id.expandableContentKotlin,
            "SmartPassLib - Kotlin implementation",
            "Native Kotlin implementation for JVM and Android.\n\n" +
                    "• Used in this app\n• Coroutines support\n• Pure Kotlin, no dependencies",
            "https://github.com/smartlegionlab/smartpasslib-kotlin"
        )

        setupExpandableCard(
            R.id.cardGo, R.id.btnGo, R.id.expandableContentGo,
            "SmartPassLib - Go implementation",
            "High-performance Go implementation.\n\n" +
                    "• Perfect for backend services\n• Concurrent safe\n• Minimal memory footprint",
            "https://github.com/smartlegionlab/smartpasslib-go"
        )

        setupExpandableCard(
            R.id.cardWeb, R.id.btnWeb, R.id.expandableContentWeb,
            "Smart Password Manager - Web",
            "Web-based password manager using the same algorithm.\n\n" +
                    "• Works in any modern browser\n• No backend required\n• PWA support",
            "https://github.com/smartlegionlab/smart-password-manager-web"
        )

        setupExpandableCard(
            R.id.cardAndroid, R.id.btnAndroid, R.id.expandableContentAndroid,
            "Smart Password Manager - Android",
            "Native Android application with Material Design.\n\n" +
                    "• Offline-first architecture\n• Deterministic password generation\n• Export/Import support\n• Dark theme",
            "https://github.com/smartlegionlab/smart-password-manager-android"
        )

        setupExpandableCard(
            R.id.cardDesktop, R.id.btnDesktop, R.id.expandableContentDesktop,
            "Smart Password Manager - Desktop",
            "Cross-platform desktop application.\n\n" +
                    "• Built with PyQt5\n• Works on Windows, macOS, Linux\n• Same core algorithm",
            "https://github.com/smartlegionlab/smart-password-manager-desktop"
        )

        setupExpandableCard(
            R.id.cardCLI, R.id.btnCLI, R.id.expandableContentCLI,
            "CLI PassMan - Password Manager",
            "Command-line password manager for power users.\n\n" +
                    "• Store and manage your password entries\n• Generate passwords on demand\n• Export/Import functionality\n• Perfect for scripts and automation",
            "https://github.com/smartlegionlab/clipassman"
        )

        setupExpandableCard(
            R.id.cardCLIPassGen, R.id.btnCLIPassGen, R.id.expandableContentCLIPassGen,
            "CLI PassGen - Password Generator",
            "Standalone command-line password generator.\n\n" +
                    "• Generate deterministic passwords from secret phrases\n• No storage, just generation\n• Perfect for quick password needs\n• Lightweight and fast",
            "https://github.com/smartlegionlab/clipassgen"
        )

        setupExpandableCard(
            R.id.cardPaper1, R.id.btnPaper1, R.id.expandableContentPaper1,
            "Pointer-Based Security Paradigm",
            "Architectural Shift from Data Protection to Data Non-Existence\n\n" +
                    "This paper introduces a revolutionary security paradigm that shifts focus from protecting data to ensuring data never exists in the first place.\n\n" +
                    "• Zero data footprint\n• Deterministic regeneration\n• Complete privacy guarantee",
            "https://github.com/smartlegionlab/pointer-based-security-paradigm"
        )

        setupExpandableCard(
            R.id.cardPaper2, R.id.btnPaper2, R.id.expandableContentPaper2,
            "Local Data Regeneration Paradigm",
            "Ontological Shift from Data Transmission to Synchronous State Discovery\n\n" +
                    "This paper presents a paradigm where data is never transmitted but regenerated locally from shared secrets.\n\n" +
                    "• No data in transit\n• Synchronous state discovery\n• Perfect forward secrecy",
            "https://github.com/smartlegionlab/local-data-regeneration-paradigm"
        )
    }

    private fun setupExpandableCard(
        cardId: Int,
        buttonId: Int,
        contentId: Int,
        title: String,
        description: String,
        url: String
    ) {
        val expandButton = findViewById<ImageView>(buttonId)
        val expandableContent = findViewById<LinearLayout>(contentId)

        val titleView = findViewById<TextView>(getTitleIdForCard(cardId))
        titleView?.text = title

        val descView = findViewById<TextView>(getDescIdForCard(cardId))
        descView?.text = description

        val linkButton = findViewById<MaterialButton>(getLinkIdForCard(cardId))
        linkButton?.setOnClickListener {
            openUrl(url)
        }

        expandButton.setOnClickListener {
            if (expandableContent.visibility == View.GONE) {
                expandableContent.visibility = View.VISIBLE
                expandableContent.alpha = 0f
                expandableContent.scaleY = 0f

                expandableContent.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .start()

                expandButton.animate()
                    .rotation(180f)
                    .setDuration(250)
                    .start()
            } else {
                expandableContent.animate()
                    .alpha(0f)
                    .scaleY(0f)
                    .setDuration(250)
                    .withEndAction {
                        expandableContent.visibility = View.GONE
                        expandableContent.scaleY = 1f
                    }
                    .start()

                expandButton.animate()
                    .rotation(0f)
                    .setDuration(250)
                    .start()
            }
        }
    }

    private fun getTitleIdForCard(cardId: Int): Int {
        return when (cardId) {
            R.id.cardPython -> R.id.titlePython
            R.id.cardJavaScript -> R.id.titleJavaScript
            R.id.cardKotlin -> R.id.titleKotlin
            R.id.cardGo -> R.id.titleGo
            R.id.cardWeb -> R.id.titleWeb
            R.id.cardAndroid -> R.id.titleAndroid
            R.id.cardDesktop -> R.id.titleDesktop
            R.id.cardCLI -> R.id.titleCLI
            R.id.cardCLIPassGen -> R.id.titleCLIPassGen
            R.id.cardPaper1 -> R.id.titlePaper1
            R.id.cardPaper2 -> R.id.titlePaper2
            else -> 0
        }
    }

    private fun getDescIdForCard(cardId: Int): Int {
        return when (cardId) {
            R.id.cardPython -> R.id.descPython
            R.id.cardJavaScript -> R.id.descJavaScript
            R.id.cardKotlin -> R.id.descKotlin
            R.id.cardGo -> R.id.descGo
            R.id.cardWeb -> R.id.descWeb
            R.id.cardAndroid -> R.id.descAndroid
            R.id.cardDesktop -> R.id.descDesktop
            R.id.cardCLI -> R.id.descCLI
            R.id.cardCLIPassGen -> R.id.descCLIPassGen
            R.id.cardPaper1 -> R.id.descPaper1
            R.id.cardPaper2 -> R.id.descPaper2
            else -> 0
        }
    }

    private fun getLinkIdForCard(cardId: Int): Int {
        return when (cardId) {
            R.id.cardPython -> R.id.linkPython
            R.id.cardJavaScript -> R.id.linkJavaScript
            R.id.cardKotlin -> R.id.linkKotlin
            R.id.cardGo -> R.id.linkGo
            R.id.cardWeb -> R.id.linkWeb
            R.id.cardAndroid -> R.id.linkAndroid
            R.id.cardDesktop -> R.id.linkDesktop
            R.id.cardCLI -> R.id.linkCLI
            R.id.cardCLIPassGen -> R.id.linkCLIPassGen
            R.id.cardPaper1 -> R.id.linkPaper1
            R.id.cardPaper2 -> R.id.linkPaper2
            else -> 0
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}