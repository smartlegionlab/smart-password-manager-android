// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var storageManager: StorageManager
    private lateinit var adapter: PasswordAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabImport: FloatingActionButton
    private lateinit var fabExport: FloatingActionButton
    private lateinit var fabHelp: FloatingActionButton
    private lateinit var fabAbout: FloatingActionButton
    private lateinit var fabMenuContainer: LinearLayout

    private var mediaPlayer: MediaPlayer? = null

    private var isMenuOpen = false

    private lateinit var slideUpAnim: android.view.animation.Animation
    private lateinit var slideDownAnim: android.view.animation.Animation

    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let { exportToUri(it) }
        }
    }

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let { importFromUri(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideDownAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        storageManager = StorageManager(this)

        setupRecyclerView()
        setupFabs()
        loadEntries()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_help -> {
                startActivity(Intent(this, OnboardingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(
            onGetPassword = { entry -> showGetPasswordDialog(entry) },
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry -> deleteEntry(entry) }
        )
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupFabs() {
        fabMenu = findViewById(R.id.fabMenu)
        fabAdd = findViewById(R.id.fabAdd)
        fabImport = findViewById(R.id.fabImport)
        fabExport = findViewById(R.id.fabExport)
        fabHelp = findViewById(R.id.fabHelp)
        fabAbout = findViewById(R.id.fabAbout)
        fabMenuContainer = findViewById(R.id.fabMenuContainer)

        fabMenu.setOnClickListener {
            if (isMenuOpen) {
                closeMenu()
            } else {
                openMenu()
            }
        }

        fabAdd.setOnClickListener {
            closeMenu()
            showAddDialog()
        }
        fabImport.setOnClickListener {
            closeMenu()
            importPasswords()
        }
        fabExport.setOnClickListener {
            closeMenu()
            exportPasswords()
        }
        fabHelp.setOnClickListener {
            closeMenu()
            startActivity(Intent(this, HelpActivity::class.java))
        }
        fabAbout.setOnClickListener {
            closeMenu()
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun openMenu() {
        isMenuOpen = true
        fabMenuContainer.visibility = View.VISIBLE

        fabAdd.startAnimation(slideUpAnim)
        fabImport.startAnimation(slideUpAnim)
        fabExport.startAnimation(slideUpAnim)
        fabHelp.startAnimation(slideUpAnim)
        fabAbout.startAnimation(slideUpAnim)

        fabMenu.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
    }

    private fun closeMenu() {
        isMenuOpen = false

        fabAdd.startAnimation(slideDownAnim)
        fabImport.startAnimation(slideDownAnim)
        fabExport.startAnimation(slideDownAnim)
        fabHelp.startAnimation(slideDownAnim)
        fabAbout.startAnimation(slideDownAnim)

        fabAdd.postDelayed({
            fabMenuContainer.visibility = View.GONE
        }, 300)

        fabMenu.setImageResource(R.drawable.ic_more_vert_24)
    }

    private fun exportPasswords() {
        val exportData = storageManager.exportData()
        if (exportData.entries.isEmpty()) {
            Toast.makeText(this, "No passwords to export", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "passwords_export_${System.currentTimeMillis()}.json")
        }

        exportFileLauncher.launch(intent)
    }

    private fun exportToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val tempFile = File(cacheDir, "temp_export.json")
                if (storageManager.exportToJsonFile(tempFile)) {
                    outputStream.write(tempFile.readBytes())
                    tempFile.delete()
                    Toast.makeText(this, "✓ Exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importPasswords() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importFileLauncher.launch(intent)
    }

    private fun importFromUri(uri: Uri) {
        try {
            val tempFile = File(cacheDir, "import_temp_${System.currentTimeMillis()}.json")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (!tempFile.exists()) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
                return
            }

            val json = tempFile.readText()

            val entries = LinkedHashMap<String, ExportEntry>()
            var metadata: Metadata? = null

            com.google.gson.stream.JsonReader(json.reader()).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    when (key) {
                        "_metadata" -> {
                            reader.beginObject()
                            var exportedAt = ""
                            var appVersion = ""
                            var libVersion = ""
                            var count = 0
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "exported_at" -> exportedAt = reader.nextString()
                                    "app_version" -> appVersion = reader.nextString()
                                    "lib_version" -> libVersion = reader.nextString()
                                    "count" -> count = reader.nextInt()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            metadata = Metadata(exportedAt, appVersion, libVersion, count)
                        }
                        else -> {
                            reader.beginObject()
                            var publicKey = ""
                            var description = ""
                            var length = 12
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "public_key" -> publicKey = reader.nextString()
                                    "description" -> description = reader.nextString()
                                    "length" -> length = reader.nextInt()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            entries[key] = ExportEntry(publicKey, description, length)
                        }
                    }
                }
                reader.endObject()
            }

            val importData = ExportData(metadata ?: Metadata("", "", "", 0), entries)

            if (importData.entries.isEmpty()) {
                Toast.makeText(this, "No passwords found in file", Toast.LENGTH_SHORT).show()
                return
            }

            AlertDialog.Builder(this)
                .setTitle("Import Passwords")
                .setMessage("Import ${importData.entries.size} passwords?\n\nCreated: ${importData._metadata.exported_at}\nApp version: ${importData._metadata.app_version}")
                .setPositiveButton("Import") { _, _ ->
                    val success = storageManager.importFromEntries(importData.entries)
                    if (success) {
                        Toast.makeText(this, "✓ Imported ${importData.entries.size} passwords", Toast.LENGTH_SHORT).show()
                        loadEntries()
                    } else {
                        Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show()
                    }
                    tempFile.delete()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    tempFile.delete()
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadEntries() {
        val entries = storageManager.loadAllEntries()
        adapter.submitList(entries)
        findViewById<TextView>(R.id.emptyStateText).visibility =
            if (entries.isEmpty()) TextView.VISIBLE else TextView.GONE
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_entry, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.entryDescription)
        val lengthInput = dialogView.findViewById<TextInputEditText>(R.id.entryLength)
        val titleLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionLayout)
        val lengthLayout = dialogView.findViewById<TextInputLayout>(R.id.lengthLayout)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Password")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ -> }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val description = titleInput.text.toString().trim()
                val lengthStr = lengthInput.text.toString()
                val length = lengthStr.toIntOrNull()

                var isValid = true

                if (description.isBlank()) {
                    playErrorSound()
                    titleLayout.error = "Enter description"
                    isValid = false
                } else {
                    titleLayout.error = null
                }

                if (length == null || length < 12 || length > 100) {
                    playErrorSound()
                    lengthLayout.error = "Length must be 12-100"
                    isValid = false
                } else {
                    lengthLayout.error = null
                }

                if (isValid) {
                    dialog.dismiss()
                    showSecretPhraseDialog(description, length ?: 12, null)
                }
            }
        }

        dialog.show()
    }

    private fun showSecretPhraseDialog(description: String, length: Int, existingEntry: PasswordEntry?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_secret, null)
        val secretInput = dialogView.findViewById<TextInputEditText>(R.id.secretPhrase)
        val confirmInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPhrase)
        val secretLayout = dialogView.findViewById<TextInputLayout>(R.id.secretLayout)
        val confirmLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmLayout)
        val strengthText = dialogView.findViewById<TextView>(R.id.passwordStrengthText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Secret Phrase")
            .setMessage("For: $description")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ -> }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.isEnabled = false

            fun updateStrength(password: String) {
                val len = password.length
                when {
                    len < 6 -> {
                        strengthText.text = "⚠️ Too short - minimum 12 characters required"
                        strengthText.setTextColor(0xFFDC3545.toInt())
                        saveButton.isEnabled = false
                    }
                    len < 12 -> {
                        strengthText.text = "⚡ Getting better... ${12 - len} more characters to go"
                        strengthText.setTextColor(0xFFFFC107.toInt())
                        saveButton.isEnabled = false
                    }
                    else -> {
                        strengthText.text = "✅ Strong secret phrase! ✓"
                        strengthText.setTextColor(0xFF198754.toInt())
                        saveButton.isEnabled = true
                    }
                }
            }

            secretInput.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateStrength(s.toString())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            updateStrength(secretInput.text.toString())

            saveButton.setOnClickListener {
                val secret = secretInput.text.toString().trim()
                val confirm = confirmInput.text.toString().trim()

                var isValid = true

                if (secret.isEmpty()) {
                    secretLayout.error = "Enter secret phrase"
                    isValid = false
                } else if (secret.length < 12) {
                    playErrorSound()
                    secretLayout.error = "Secret phrase must be at least 12 characters"
                    isValid = false
                } else {
                    secretLayout.error = null
                }

                if (confirm != secret) {
                    playErrorSound()
                    confirmLayout.error = "Phrases don't match"
                    isValid = false
                } else {
                    confirmLayout.error = null
                }

                if (isValid) {
                    val publicKey = PasswordGenerator.generatePublicKey(secret)

                    if (existingEntry == null) {
                        val existingEntries = storageManager.loadAllEntries()
                        val duplicate = existingEntries.find { it.publicKey == publicKey }

                        if (duplicate != null) {
                            playErrorSound()
                            AlertDialog.Builder(this)
                                .setTitle("Duplicate Secret Phrase")
                                .setMessage("A password entry with the same secret phrase already exists for \"${duplicate.description}\".\n\nPlease use a different secret phrase.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            playSuccessSound()
                            val entry = PasswordEntry(
                                description = description,
                                publicKey = publicKey,
                                length = length
                            )
                            storageManager.saveEntry(entry)
                            Toast.makeText(this, "✓ Password saved", Toast.LENGTH_SHORT).show()
                            loadEntries()
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showGetPasswordDialog(entry: PasswordEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_get_password, null)
        val descriptionText = dialogView.findViewById<TextView>(R.id.passwordDescription)
        val secretInput = dialogView.findViewById<TextInputEditText>(R.id.secretInput)
        val secretLayout = dialogView.findViewById<TextInputLayout>(R.id.getSecretLayout)
        val passwordText = dialogView.findViewById<TextView>(R.id.passwordResult)
        val generateBtn = dialogView.findViewById<Button>(R.id.generateBtn)
        val copyBtn = dialogView.findViewById<Button>(R.id.copyBtn)

        descriptionText.text = entry.description

        val dialog = AlertDialog.Builder(this)
            .setTitle("Get Password")
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()

        generateBtn.setOnClickListener {
            val secret = secretInput.text.toString().trim()
            if (secret.length < 12) {
                playErrorSound()
                secretLayout.error = "Secret phrase must be at least 12 characters"
                passwordText.visibility = TextView.GONE
                copyBtn.visibility = Button.GONE
                return@setOnClickListener
            }

            if (secret.isEmpty()) {
                secretLayout.error = "Enter secret phrase"
                passwordText.visibility = TextView.GONE
                copyBtn.visibility = Button.GONE
            } else if (PasswordGenerator.verifySecret(secret, entry.publicKey)) {
                playSuccessSound()
                secretLayout.error = null
                passwordText.visibility = TextView.VISIBLE
                copyBtn.visibility = Button.VISIBLE
                val password = PasswordGenerator.generatePassword(secret, entry.length ?: 12)
                passwordText.text = password
                copyBtn.isEnabled = true
                Toast.makeText(this, "✓ Password generated", Toast.LENGTH_SHORT).show()
            } else {
                playErrorSound()
                secretLayout.error = "Wrong secret phrase!"
                passwordText.visibility = TextView.VISIBLE
                copyBtn.visibility = Button.VISIBLE
                passwordText.text = "Error: invalid phrase"
                copyBtn.isEnabled = false
            }
        }

        copyBtn.setOnClickListener {
            val password = passwordText.text.toString()
            if (password.isNotBlank() && !password.startsWith("Error")) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("password", password)
                clipboard.setPrimaryClip(clip)
                playSuccessSound()
                Toast.makeText(this, "✓ Copied", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditDialog(entry: PasswordEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_entry, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.entryDescription)
        val lengthInput = dialogView.findViewById<TextInputEditText>(R.id.entryLength)
        val titleLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionLayout)
        val lengthLayout = dialogView.findViewById<TextInputLayout>(R.id.lengthLayout)

        titleInput.setText(entry.description)
        lengthInput.setText(entry.length.toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Password Entry")
            .setMessage("Only description and length can be changed.\n\nSecret phrase remains the same.")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ -> }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val description = titleInput.text.toString().trim()
                val lengthStr = lengthInput.text.toString()
                val length = lengthStr.toIntOrNull()

                var isValid = true

                if (description.isBlank()) {
                    playErrorSound()
                    titleLayout.error = "Enter description"
                    isValid = false
                } else {
                    titleLayout.error = null
                }

                if (length == null || length < 12 || length > 100) {
                    playErrorSound()
                    lengthLayout.error = "Length must be 12-100"
                    isValid = false
                } else {
                    lengthLayout.error = null
                }

                if (isValid) {
                    playSuccessSound()
                    val updatedEntry = PasswordEntry(
                        description = description,
                        publicKey = entry.publicKey,
                        length = length
                    )
                    storageManager.updateEntry(updatedEntry)
                    Toast.makeText(this, "✓ Updated", Toast.LENGTH_SHORT).show()
                    loadEntries()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun deleteEntry(entry: PasswordEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete \"${entry.description}\"?")
            .setPositiveButton("Delete") { _, _ ->
                storageManager.deleteEntry(entry.publicKey)
                playErrorSound()
                Toast.makeText(this, "✓ Deleted", Toast.LENGTH_SHORT).show()
                loadEntries()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun stopSound() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
    }
}