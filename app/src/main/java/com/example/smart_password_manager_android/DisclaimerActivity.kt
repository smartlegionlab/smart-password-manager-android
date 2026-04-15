// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DisclaimerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclaimer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "LEGAL DISCLAIMER"

        val checkAccept = findViewById<CheckBox>(R.id.checkAccept)
        val btnAccept = findViewById<Button>(R.id.btnAccept)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        checkAccept.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
        }

        btnAccept.setOnClickListener {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("disclaimer_accepted", true).apply()
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }

        btnDecline.setOnClickListener {
            finishAffinity()
        }
    }
}