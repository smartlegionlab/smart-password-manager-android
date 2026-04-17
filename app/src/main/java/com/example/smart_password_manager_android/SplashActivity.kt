// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_STORAGE = 100
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        playSplashMusic()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val disclaimerAccepted = prefs.getBoolean("disclaimer_accepted", false)

        if (!disclaimerAccepted) {
            stopMusicAndProceed {
                startActivity(Intent(this, DisclaimerActivity::class.java))
                finish()
            }
        } else {
            checkAndRequestStoragePermission()
        }
    }

    private fun playSplashMusic() {
        try {
            val assetFileDescriptor = assets.openFd("music/splash.wav")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                prepare()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusicAndProceed(action: () -> Unit) {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        action()
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStoragePermissionDialog()
            } else {
                proceedToNextScreen()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE)
            } else {
                proceedToNextScreen()
            }
        }
    }

    private fun showStoragePermissionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_permission, null)
        val messageText = dialogView.findViewById<TextView>(R.id.permissionMessage)
        val grantBtn = dialogView.findViewById<Button>(R.id.btnGrantPermission)
        val tempBtn = dialogView.findViewById<Button>(R.id.btnUseTemporarily)

        messageText.text = "Smart Password Manager needs storage permission to save your passwords permanently in Documents folder.\n\nThis allows your passwords to survive app updates and reinstallation."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        grantBtn.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, PERMISSION_REQUEST_STORAGE)
        }

        tempBtn.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this,
                "Storage permission is required for this app to work properly.\nThe app will now close.",
                Toast.LENGTH_LONG).show()
            stopMusicAndProceed { finishAffinity() }
        }

        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                proceedToNextScreen()
            } else {
                Toast.makeText(this,
                    "Storage permission is required.\nThe app will now close.",
                    Toast.LENGTH_LONG).show()
                stopMusicAndProceed { finishAffinity() }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show()
                    proceedToNextScreen()
                } else {
                    Toast.makeText(this,
                        "Storage access is required.\nThe app will now close.",
                        Toast.LENGTH_LONG).show()
                    stopMusicAndProceed { finishAffinity() }
                }
            }
        }
    }

    private fun proceedToNextScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

            val intent = if (isFirstLaunch) {
                prefs.edit().putBoolean("is_first_launch", false).apply()
                Intent(this, OnboardingActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }

            stopMusicAndProceed {
                startActivity(intent)
                finish()
            }
        }, 6000)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}