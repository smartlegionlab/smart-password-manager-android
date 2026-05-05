// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smartlegionlab.smartpasslib.SmartPassLib
import java.io.File
import androidx.core.view.isVisible
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup

class SmartPasswordManager : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 201
    }

    private lateinit var storageManager: StorageManager
    private lateinit var adapter: PasswordAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabScanQr: FloatingActionButton
    private lateinit var fabImport: FloatingActionButton
    private lateinit var fabExport: FloatingActionButton
    private lateinit var fabHelp: FloatingActionButton
    private lateinit var fabAbout: FloatingActionButton
    private lateinit var fabMenuContainer: LinearLayout
    private lateinit var serviceCountText: TextView
    private lateinit var searchButton: ImageView
    private lateinit var searchBar: View
    private lateinit var searchInput: EditText
    private lateinit var closeSearchButton: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var isMenuOpen = false
    private var allEntries: List<SmartPassword> = emptyList()
    private var filteredEntries: List<SmartPassword> = emptyList()
    private var currentQuery = ""

    private lateinit var slideUpAnim: android.view.animation.Animation
    private lateinit var slideDownAnim: android.view.animation.Animation
    private lateinit var slideInDownAnim: android.view.animation.Animation
    private lateinit var slideOutUpAnim: android.view.animation.Animation

    private val tooltipHandler = Handler(Looper.getMainLooper())
    private var currentTooltip: PopupWindow? = null

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

    private val qrScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadEntries()
            Toast.makeText(this, "✓ Password list updated", Toast.LENGTH_SHORT).show()
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
        slideInDownAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_down)
        slideOutUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_up)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        storageManager = StorageManager(this)

        setupSearch()
        setupRecyclerView()
        setupDragAndDrop()
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

    private fun setupSearch() {
        searchButton = findViewById(R.id.searchButton)
        searchBar = findViewById(R.id.searchBar)
        searchInput = findViewById(R.id.searchInput)
        closeSearchButton = findViewById(R.id.closeSearchButton)
        serviceCountText = findViewById(R.id.serviceCountText)

        searchButton.setOnClickListener {
            showSearchBar()
        }

        closeSearchButton.setOnClickListener {
            hideSearchBar()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                filterEntries(currentQuery)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }
    }

    private fun showSearchBar() {
        searchBar.visibility = View.VISIBLE
        searchBar.startAnimation(slideInDownAnim)
        searchButton.visibility = View.GONE
        searchInput.requestFocus()
        showKeyboard()
    }

    private fun hideSearchBar() {
        searchBar.startAnimation(slideOutUpAnim)
        searchBar.visibility = View.GONE
        searchButton.visibility = View.VISIBLE

        searchInput.text.clear()
        currentQuery = ""
        filterEntries("")
        hideKeyboard()
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    private fun filterEntries(query: String) {
        filteredEntries = if (query.isEmpty()) {
            allEntries
        } else {
            allEntries.filter { it.description.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filteredEntries)
        updateServiceCount()
    }

    @SuppressLint("SetTextI18n")
    private fun updateServiceCount() {
        val count = filteredEntries.size
        serviceCountText.text = "$count passwords"

        val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
        if (filteredEntries.isEmpty()) {
            emptyStateText.text = if (searchInput.text.isNullOrEmpty()) {
                "No passwords yet.\nTap + to add your first password"
            } else {
                "No passwords match \"${searchInput.text}\""
            }
            emptyStateText.visibility = TextView.VISIBLE
            findViewById<RecyclerView>(R.id.recyclerView).visibility = View.GONE
        } else {
            emptyStateText.visibility = TextView.GONE
            findViewById<RecyclerView>(R.id.recyclerView).visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(
            context = this,
            onGetPassword = { entry -> showGetPasswordDialog(entry) },
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry -> deleteEntry(entry) }
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@SmartPasswordManager)
            adapter = this@SmartPasswordManager.adapter
        }
    }

    private fun checkCameraPermissionAndLaunchScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                    startQrScanner()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    showCameraPermissionExplanationDialog()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST
                    )
                }
            }
        } else {
            startQrScanner()
        }
    }

    private fun showCameraPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("Smart Password Manager needs camera permission to scan QR codes for importing passwords.")
            .setPositiveButton("Grant Permission") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startQrScanner() {
        val intent = Intent(this, QrCodeScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startQrScanner()
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
                }
            }
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

    private fun setupDragAndDrop() {
        val simpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition

                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                    return false
                }

                adapter.moveItem(fromPos, toPos)

                val newOrder = adapter.getEntries().map { it.publicKey }

                storageManager.updateOrder(newOrder)

                updateAllEntriesOrder(newOrder)

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                val position = viewHolder?.bindingAdapterPosition ?: return
                if (position == RecyclerView.NO_POSITION) return

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    (viewHolder as? PasswordAdapter.ViewHolder)?.highlightForDrag()
                    vibrateLong()
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    (viewHolder as? PasswordAdapter.ViewHolder)?.clearHighlight()
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                (viewHolder as? PasswordAdapter.ViewHolder)?.clearHighlight()
            }
        }

        ItemTouchHelper(simpleCallback).attachToRecyclerView(findViewById(R.id.recyclerView))
    }

    private fun updateAllEntriesOrder(newOrder: List<String>) {
        val orderMap = newOrder.mapIndexed { index, key -> key to index }.toMap()
        allEntries = allEntries.sortedBy { orderMap[it.publicKey] ?: Int.MAX_VALUE }

        if (currentQuery.isNotEmpty()) {
            filterEntries(currentQuery)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrateLong() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFabs() {
        fabMenu = findViewById(R.id.fabMenu)
        fabAdd = findViewById(R.id.fabAdd)
        fabScanQr = findViewById(R.id.fabScanQr)
        fabImport = findViewById(R.id.fabImport)
        fabExport = findViewById(R.id.fabExport)
        fabHelp = findViewById(R.id.fabHelp)
        fabAbout = findViewById(R.id.fabAbout)
        fabMenuContainer = findViewById(R.id.fabMenuContainer)

        setupLongPressTooltips()

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

        fabScanQr.setOnClickListener {
            closeMenu()
            checkCameraPermissionAndLaunchScanner()
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

    private fun showTooltipLeft(anchorView: View, text: String) {
        val tooltipView = LayoutInflater.from(this).inflate(R.layout.tooltip_left_layout, null)
        val tooltipText = tooltipView.findViewById<TextView>(R.id.tooltipText)
        tooltipText.text = text

        val popupWindow = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        tooltipView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val tooltipWidth = tooltipView.measuredWidth
        val tooltipHeight = tooltipView.measuredHeight

        val x = location[0] - tooltipWidth - 16
        val y = location[1] + (anchorView.height / 2) - (tooltipHeight / 2)

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)

        Handler(Looper.getMainLooper()).postDelayed({
            popupWindow.dismiss()
        }, 4000)
    }

    private fun hideTooltip() {
        currentTooltip?.dismiss()
        currentTooltip = null
        tooltipHandler.removeCallbacksAndMessages(null)
    }

    private fun openMenu() {
        isMenuOpen = true
        fabMenuContainer.visibility = View.VISIBLE

        fabAdd.visibility = View.VISIBLE
        fabScanQr.visibility = View.VISIBLE
        fabImport.visibility = View.VISIBLE
        fabExport.visibility = View.VISIBLE
        fabHelp.visibility = View.VISIBLE
        fabAbout.visibility = View.VISIBLE

        fabAdd.startAnimation(slideUpAnim)
        fabScanQr.startAnimation(slideUpAnim)
        fabImport.startAnimation(slideUpAnim)
        fabExport.startAnimation(slideUpAnim)
        fabHelp.startAnimation(slideUpAnim)
        fabAbout.startAnimation(slideUpAnim)

        fabMenu.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isMenuOpen) {
                showAllTooltips()
            }
        }, 350)
    }

    private fun showAllTooltips() {
        showTooltipLeft(fabAdd, "➕ Add new password")

        showTooltipLeft(fabScanQr, "📷 Scan QR code")

        showTooltipLeft(fabImport, "📥 Import from JSON")

        showTooltipLeft(fabExport, "📤 Export to JSON")

        showTooltipLeft(fabHelp, "❓ Help & guide")

        showTooltipLeft(fabAbout, "ℹ️ About app")
    }

    private fun setupLongPressTooltips() {
        fabAdd.setOnLongClickListener {
            showTooltipLeft(fabAdd, "➕ Add new password")
            true
        }
        fabScanQr.setOnLongClickListener {
            showTooltipLeft(fabScanQr, "📷 Scan QR code")
            true
        }
        fabImport.setOnLongClickListener {
            showTooltipLeft(fabImport, "📥 Import from JSON")
            true
        }
        fabExport.setOnLongClickListener {
            showTooltipLeft(fabExport, "📤 Export to JSON")
            true
        }
        fabHelp.setOnLongClickListener {
            showTooltipLeft(fabHelp, "❓ Help & guide")
            true
        }
        fabAbout.setOnLongClickListener {
            showTooltipLeft(fabAbout, "ℹ️ About app")
            true
        }
    }

    private fun closeMenu() {
        isMenuOpen = false

        hideTooltip()

        fabAdd.startAnimation(slideDownAnim)
        fabScanQr.startAnimation(slideDownAnim)
        fabImport.startAnimation(slideDownAnim)
        fabExport.startAnimation(slideDownAnim)
        fabHelp.startAnimation(slideDownAnim)
        fabAbout.startAnimation(slideDownAnim)

        fabAdd.postDelayed({
            fabAdd.visibility = View.GONE
            fabScanQr.visibility = View.GONE
            fabImport.visibility = View.GONE
            fabExport.visibility = View.GONE
            fabHelp.visibility = View.GONE
            fabAbout.visibility = View.GONE
            fabMenuContainer.visibility = View.GONE
        }, 300)

        fabMenu.setImageResource(R.drawable.ic_more_vert_24)

        tooltipHandler.removeCallbacksAndMessages(null)
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
                            var appName = ""
                            var appVersion = ""
                            var appType = ""
                            var libName = ""
                            var libVersion = ""
                            var libLang = ""
                            var count = 0
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "exported_at" -> exportedAt = reader.nextString()
                                    "app_name" -> appName = reader.nextString()
                                    "app_version" -> appVersion = reader.nextString()
                                    "app_type" -> appType = reader.nextString()
                                    "lib_name" -> libName = reader.nextString()
                                    "lib_version" -> libVersion = reader.nextString()
                                    "lib_lang" -> libLang = reader.nextString()
                                    "count" -> count = reader.nextInt()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            metadata = Metadata(exportedAt, appName, appVersion, appType, libName, libVersion, libLang, count)
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

            val importData = ExportData(metadata ?: Metadata("", "", "", "", "", "", "", 0), entries)

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

    @SuppressLint("UseKtx")
    private fun loadEntries() {
        allEntries = storageManager.loadAllEntries()
        filteredEntries = allEntries
        adapter.submitList(filteredEntries)
        updateServiceCount()

        if (searchBar.isVisible) {
            hideSearchBar()
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

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_custom, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.entryDescription)
        val lengthInput = dialogView.findViewById<TextInputEditText>(R.id.entryLength)
        val titleLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionLayout)
        val lengthLayout = dialogView.findViewById<TextInputLayout>(R.id.lengthLayout)
        val charCounter = dialogView.findViewById<TextView>(R.id.charCounter)
        val btnCreate = dialogView.findViewById<MaterialButton>(R.id.btnCreate)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        titleInput.filters = arrayOf(android.text.InputFilter.LengthFilter(255))
        setupCharCounter(titleInput, charCounter, 255)

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnCreate.setOnClickListener {
            var description = titleInput.text.toString().trim()
            val lengthStr = lengthInput.text.toString()
            val length = lengthStr.toIntOrNull()

            var isValid = true

            if (description.isBlank()) {
                playErrorSound()
                titleLayout.error = "Enter service name"
                isValid = false
            } else if (description.length > 255) {
                playErrorSound()
                titleLayout.error = "Description must be less than 255 characters"
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
                description = sanitizeString(description)
                dialog.dismiss()
                showSecretPhraseDialog(description, length ?: 12, null)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSecretPhraseDialog(description: String, length: Int, existingEntry: SmartPassword?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_secret_custom, null)
        val secretInput = dialogView.findViewById<TextInputEditText>(R.id.secretPhrase)
        val confirmInput = dialogView.findViewById<TextInputEditText>(R.id.confirmPhrase)
        val secretLayout = dialogView.findViewById<TextInputLayout>(R.id.secretLayout)
        val confirmLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmLayout)
        val strengthText = dialogView.findViewById<TextView>(R.id.passwordStrengthText)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val subtitleText = dialogView.findViewById<TextView>(R.id.dialogSubtitle)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        if (existingEntry != null) {
            titleText.text = "Verify Secret Phrase"
            subtitleText.text = "Enter your secret phrase to confirm"
        } else {
            titleText.text = "Secret Phrase"
            subtitleText.text = "This phrase is NEVER stored"
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnSave.isEnabled = false

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        fun updateStrength(password: String) {
            val len = password.length
            when {
                len < 6 -> {
                    strengthText.text = "⚠️ Too short - minimum 12 characters required"
                    strengthText.setTextColor(0xFFDC3545.toInt())
                    btnSave.isEnabled = false
                }
                len < 12 -> {
                    strengthText.text = "⚡ ${12 - len} more characters to go"
                    strengthText.setTextColor(0xFFFFC107.toInt())
                    btnSave.isEnabled = false
                }
                else -> {
                    strengthText.text = "✅ Strong secret phrase!"
                    strengthText.setTextColor(0xFF198754.toInt())
                    btnSave.isEnabled = true
                }
            }
        }

        secretInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateStrength(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        updateStrength(secretInput.text.toString())

        btnSave.setOnClickListener {
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
                val publicKey = SmartPassLib.generatePublicKey(secret)

                if (existingEntry == null) {
                    val existingEntries = storageManager.loadAllEntries()
                    val duplicate = existingEntries.find { it.publicKey == publicKey }

                    if (duplicate != null) {
                        playErrorSound()
                        showErrorDialog("Duplicate Secret Phrase", "A password entry with the same secret phrase already exists for \"${duplicate.description}\".\n\nPlease use a different secret phrase.")
                    } else {
                        playSuccessSound()
                        val entry = SmartPassword(
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

    @SuppressLint("SetTextI18n")
    private fun showGetPasswordDialog(entry: SmartPassword) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_get_password_custom, null)
        val descriptionText = dialogView.findViewById<TextView>(R.id.passwordDescription)
        val secretInput = dialogView.findViewById<TextInputEditText>(R.id.secretInput)
        val secretLayout = dialogView.findViewById<TextInputLayout>(R.id.getSecretLayout)
        val passwordText = dialogView.findViewById<TextView>(R.id.passwordResult)
        val generateBtn = dialogView.findViewById<MaterialButton>(R.id.generateBtn)
        val copyBtn = dialogView.findViewById<MaterialButton>(R.id.copyBtn)
        val closeBtn = dialogView.findViewById<ImageView>(R.id.btnClose)

        descriptionText.text = entry.description

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

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
            } else if (SmartPassLib.verifySecret(secret, entry.publicKey)) {
                playSuccessSound()
                secretLayout.error = null
                passwordText.visibility = TextView.VISIBLE
                copyBtn.visibility = Button.VISIBLE
                val password = SmartPassLib.generatePassword(secret, entry.length ?: 12)
                passwordText.text = password
                copyBtn.isEnabled = true
                Toast.makeText(this, "✓ Password generated", Toast.LENGTH_SHORT).show()
            } else {
                playErrorSound()
                secretLayout.error = "Wrong secret phrase!"
                passwordText.visibility = TextView.VISIBLE
                copyBtn.visibility = Button.VISIBLE
                passwordText.text = "❌ Invalid secret phrase"
                copyBtn.isEnabled = false
            }
        }

        copyBtn.setOnClickListener {
            val password = passwordText.text.toString()
            if (password.isNotBlank() && !password.startsWith("❌")) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("password", password)
                clipboard.setPrimaryClip(clip)
                playSuccessSound()
                Toast.makeText(this, "✓ Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(entry: SmartPassword) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_custom, null)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.entryDescription)
        val lengthInput = dialogView.findViewById<TextInputEditText>(R.id.entryLength)
        val titleLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionLayout)
        val lengthLayout = dialogView.findViewById<TextInputLayout>(R.id.lengthLayout)
        val charCounter = dialogView.findViewById<TextView>(R.id.charCounter)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        titleInput.filters = arrayOf(android.text.InputFilter.LengthFilter(255))
        setupCharCounter(titleInput, charCounter, 255)

        titleInput.setText(entry.description)
        lengthInput.setText(entry.length.toString())

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            var description = titleInput.text.toString().trim()
            val lengthStr = lengthInput.text.toString()
            val length = lengthStr.toIntOrNull()

            var isValid = true

            if (description.isBlank()) {
                playErrorSound()
                titleLayout.error = "Enter service name"
                isValid = false
            } else if (description.length > 255) {
                playErrorSound()
                titleLayout.error = "Description must be less than 255 characters"
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
                description = sanitizeString(description)
                playSuccessSound()
                val updatedEntry = SmartPassword(
                    description = description,
                    publicKey = entry.publicKey,
                    length = length
                )
                storageManager.updateEntry(updatedEntry)
                Toast.makeText(this, "✓ Password updated", Toast.LENGTH_SHORT).show()
                loadEntries()
                dialog.dismiss()
            }
        }
    }

    private fun deleteEntry(entry: SmartPassword) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_custom, null)
        val deleteDescription = dialogView.findViewById<TextView>(R.id.deleteDescription)
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        deleteDescription.text = entry.description

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            storageManager.deleteEntry(entry.publicKey)
            playErrorSound()
            Toast.makeText(this, "✓ Password deleted", Toast.LENGTH_SHORT).show()
            loadEntries()
            dialog.dismiss()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_error_custom, null)
        val errorTitle = dialogView.findViewById<TextView>(R.id.errorTitle)
        val errorMessage = dialogView.findViewById<TextView>(R.id.errorMessage)
        val closeBtn = dialogView.findViewById<ImageView>(R.id.btnCloseError)
        val okBtn = dialogView.findViewById<MaterialButton>(R.id.btnOk)

        errorTitle.text = title
        errorMessage.text = message

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        okBtn.setOnClickListener {
            dialog.dismiss()
        }
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
        hideTooltip()
        tooltipHandler.removeCallbacksAndMessages(null)
    }
}