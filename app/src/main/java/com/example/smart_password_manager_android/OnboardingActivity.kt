// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var nextBtn: Button
    private lateinit var skipBtn: Button
    private lateinit var tooltipCard: MaterialCardView
    private lateinit var titleText: TextView
    private lateinit var descText: TextView
    private lateinit var progressText: TextView

    private val steps = listOf(
        GuideStep(
            title = "🎯 Welcome to Smart Password Manager!",
            description = "This app generates secure smart passwords on the fly.\n\nYour secret phrases and smart passwords are NEVER stored anywhere!\n\n⚠️ Storage permission is required for the app to work."
        ),
        GuideStep(
            title = "📝 How to Create a Smart Password",
            description = "1. Tap the [⋮] button (three dots)\n2. Tap the green [+] button\n3. Enter description (e.g., 'Gmail')\n4. Set smart password length (12-100)\n5. Enter your secret phrase (min 12 chars)\n6. Save - only public key is stored!"
        ),
        GuideStep(
            title = "🔐 Important Security Info",
            description = "✓ Secret phrases are NEVER stored\n✓ Smart Passwords are NEVER stored\n✓ Only public keys are saved\n✓ Smart Passwords are generated on the fly\n✓ You must remember your secret phrases!\n✓ Secret phrases must be at least 12 characters"
        ),
        GuideStep(
            title = "👁️ How to Get Your Smart Password",
            description = "1. Tap the [👁️] eye icon on any entry\n2. Enter your secret phrase (min 12 chars)\n3. Smart Password is generated instantly\n4. Copy to clipboard\n\nNo storage, no tracking!"
        ),
        GuideStep(
            title = "🔍 Search, Drag & Drop",
            description = "🔍 Search: Tap the search icon in toolbar to find passwords by description\n\n↕️ Drag & Drop: Long press (0.5 sec) on any password card to enter drag mode\n   • Blue border appears + vibration\n   • Drag up/down to reorder\n   • Release to save new order\n   • Order is saved permanently!"
        ),
        GuideStep(
            title = "✏️ Edit, Delete & Export/Import",
            description = "✏️ Edit: Tap [✏️] pencil icon to change description or length only\n   (Secret phrase cannot be changed!)\n\n🗑️ Delete: Tap [🗑️] trash icon to remove entry\n\n📤 Export/Import: Use the [⋮] menu to backup or restore your smart passwords"
        ),
        GuideStep(
            title = "💡 Pro Tips",
            description = "• Use strong secret phrases (12+ chars)\n• Each service should have a UNIQUE secret phrase\n• Export your smart passwords regularly as backup\n• Keep your secret phrases safe - they cannot be recovered!\n• Same secret phrase + same length = same smart password everywhere\n• Reorder passwords by long pressing and dragging"
        ),
        GuideStep(
            title = "🎉 You're Ready!",
            description = "Start creating your secure smart passwords now!\n\nTap Finish to begin."
        )
    )

    data class GuideStep(
        val title: String,
        val description: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        playGuideMusic()
        setupViews()
        showStep(0)
    }

    private fun playGuideMusic() {
        try {
            val assetFileDescriptor = assets.openFd("music/guide.wav")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun setupViews() {
        nextBtn = findViewById(R.id.btnNext)
        skipBtn = findViewById(R.id.btnSkip)
        tooltipCard = findViewById(R.id.tooltipCard)
        titleText = findViewById(R.id.tooltipTitle)
        descText = findViewById(R.id.tooltipDescription)
        progressText = findViewById(R.id.progressText)

        nextBtn.setOnClickListener {
            if (currentStep < steps.size - 1) {
                currentStep++
                showStep(currentStep)
            } else {
                finishOnboarding()
            }
        }

        skipBtn.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun showStep(step: Int) {
        val stepData = steps[step]

        titleText.text = stepData.title
        descText.text = stepData.description
        progressText.text = "${step + 1}/${steps.size}"

        if (step == steps.size - 1) {
            nextBtn.text = "Finish"
        } else {
            nextBtn.text = "Next"
        }

        tooltipCard.animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        tooltipCard.alpha = 1f
    }

    private fun finishOnboarding() {
        stopMusic()
        startActivity(Intent(this, SmartPasswordManager::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }
}