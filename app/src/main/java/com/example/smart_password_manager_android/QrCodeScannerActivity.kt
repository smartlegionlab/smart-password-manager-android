// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class QrCodeScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var statusText: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var isProcessing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback = BarcodeCallback { result: BarcodeResult ->
        if (!isProcessing) {
            isProcessing = true
            mainHandler.post {
                onQrCodeScanned(result.text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.barcodeView)
        statusText = findViewById(R.id.statusText)

        hidePromptText()

        val btnClose = findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            finish()
        }

        try {
            barcodeView.decodeContinuous(callback)
            barcodeView.resume()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun hidePromptText() {
        try {
            val textViews = findAllTextViews(barcodeView)
            for (textView in textViews) {
                if ((textView.text?.length ?: 0) < 100) {
                    textView.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun findAllTextViews(view: View): MutableList<TextView> {
        val result = mutableListOf<TextView>()
        if (view is TextView) {
            result.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                result.addAll(findAllTextViews(view.getChildAt(i)))
            }
        }
        return result
    }

    @SuppressLint("SetTextI18n")
    private fun onQrCodeScanned(qrText: String) {
        playSuccessSound()

        try {
            val qrData = QrCodeData.fromJson(qrText)
            if (qrData == null) {
                playErrorSound()
                statusText.text = "❌ Invalid QR code format"
                statusText.visibility = View.VISIBLE
                Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show()

                mainHandler.postDelayed({
                    statusText.visibility = View.GONE
                    isProcessing = false
                    barcodeView.resume()
                }, 2000)
                return
            }

            barcodeView.pause()
            showSecretPhraseDialog(qrData)
        } catch (e: Exception) {
            e.printStackTrace()
            playErrorSound()
            Toast.makeText(this, "Error processing QR code: ${e.message}", Toast.LENGTH_SHORT).show()
            isProcessing = false
            barcodeView.resume()
        }
    }

    private fun setupCharCounter(
        editText: TextInputEditText,
        counterText: TextView,
        maxLength: Int = 255
    ) {
        @SuppressLint("SetTextI18n")
        fun updateCounter() {
            val length = editText.text?.length ?: 0
            counterText.text = "$length/$maxLength"

            counterText.setTextColor(when {
                length == 0 -> Color.WHITE
                length < 200 -> Color.parseColor("#4CAF50")
                length < maxLength -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#F44336")
            })
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCounter()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        updateCounter()
    }

    @SuppressLint("SetTextI18n")
    private fun showSecretPhraseDialog(qrData: QrCodeData) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_qr_secret, null)
            val secretInput = dialogView.findViewById<TextInputEditText>(R.id.secretPhrase)
            val secretLayout = dialogView.findViewById<TextInputLayout>(R.id.secretLayout)
            val lengthInfo = dialogView.findViewById<TextView>(R.id.lengthInfo)
            val keyPreview = dialogView.findViewById<TextView>(R.id.keyPreview)
            val btnVerify = dialogView.findViewById<Button>(R.id.btnVerify)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            lengthInfo.text = "🔢 Password length: ${qrData.l} symbols"
            keyPreview.text = "🔑 Public key: ${qrData.k.take(16)}..."

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            btnCancel.setOnClickListener {
                dialog.dismiss()
                isProcessing = false
                barcodeView.resume()
            }

            btnVerify.setOnClickListener {
                val secret = secretInput.text.toString().trim()

                if (secret.length < 12) {
                    playErrorSound()
                    secretLayout.error = "Secret phrase must be at least 12 characters"
                    return@setOnClickListener
                }

                secretLayout.error = null

                try {
                    val computedPublicKey = com.smartlegionlab.smartpasslib.SmartPassLib.generatePublicKey(secret)

                    if (computedPublicKey == qrData.k) {
                        playSuccessSound()
                        dialog.dismiss()
                        showDescriptionDialog(qrData.l, secret)
                    } else {
                        playErrorSound()
                        secretLayout.error = "❌ Secret phrase doesn't match this QR code"
                        Toast.makeText(this, "❌ Invalid secret phrase! The secret phrase doesn't match this QR code.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                } catch (e: Exception) {
                    playErrorSound()
                    secretLayout.error = "Error: ${e.message}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
            isProcessing = false
            barcodeView.resume()
        }
    }

    private fun showDescriptionDialog(length: Int, secret: String) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_qr_description, null)
            val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.passwordDescription)
            val charCounter = dialogView.findViewById<TextView>(R.id.charCounter)
            val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            descriptionInput.filters = arrayOf(android.text.InputFilter.LengthFilter(255))
            setupCharCounter(descriptionInput, charCounter, 255)

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            btnCancel.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            btnAdd.setOnClickListener {
                var description = descriptionInput.text.toString().trim()

                if (description.isEmpty()) {
                    descriptionInput.error = "Enter service name"
                    playErrorSound()
                    return@setOnClickListener
                }

                if (description.length > 255) {
                    descriptionInput.error = "Description must be less than 255 characters"
                    playErrorSound()
                    return@setOnClickListener
                }

                description = sanitizeString(description)

                val publicKey = com.smartlegionlab.smartpasslib.SmartPassLib.generatePublicKey(secret)

                val storageManager = StorageManager(this)
                val existingEntries = storageManager.loadAllEntries()
                val duplicate = existingEntries.find { it.publicKey == publicKey }

                if (duplicate != null) {
                    playErrorSound()
                    Toast.makeText(this, "⚠️ Password for this secret phrase already exists (${duplicate.description})", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    finish()
                    return@setOnClickListener
                }

                val newEntry = SmartPassword(
                    description = description,
                    publicKey = publicKey,
                    length = length
                )

                if (storageManager.saveEntry(newEntry)) {
                    playSuccessSound()
                    Toast.makeText(this, "✓ Password added successfully!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save password", Toast.LENGTH_SHORT).show()
                    playErrorSound()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sanitizeString(input: String): String {
        return input
            .replace("\"", "'")
            .replace("\\", "/")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("\u0000", "")
    }

    private fun playSuccessSound() {
        try {
            val assetFileDescriptor = assets.openFd("music/notif.wav")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                prepare()
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playErrorSound() {
        try {
            val assetFileDescriptor = assets.openFd("music/disclaimer.wav")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                prepare()
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized && !isProcessing) {
            try {
                barcodeView.resume()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            try {
                barcodeView.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}