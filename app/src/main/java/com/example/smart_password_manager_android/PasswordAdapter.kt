// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class PasswordAdapter(
    private val context: Context,
    private val onGetPassword: (SmartPassword) -> Unit,
    private val onEdit: (SmartPassword) -> Unit,
    private val onDelete: (SmartPassword) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var entries: MutableList<SmartPassword> = mutableListOf()
    private val expandedStates = mutableMapOf<Int, Boolean>()

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newEntries: List<SmartPassword>) {
        entries = newEntries.toMutableList()
        expandedStates.clear()
        notifyDataSetChanged()
    }

    fun getEntries(): List<SmartPassword> = entries

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return

        val item = entries.removeAt(fromPosition)
        entries.add(toPosition, item)

        val oldExpanded = expandedStates[fromPosition] ?: false
        expandedStates[toPosition] = oldExpanded
        expandedStates.remove(fromPosition)

        notifyItemMoved(fromPosition, toPosition)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrateLong() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (e: Exception) { }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position], position)
    }

    override fun getItemCount(): Int = entries.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView as MaterialCardView
        private val descriptionText: TextView = itemView.findViewById(R.id.itemDescription)
        private val lengthText: TextView = itemView.findViewById(R.id.itemLength)
        private val publicKeyPreview: TextView = itemView.findViewById(R.id.itemPublicKey)
        private val getBtn: ImageButton = itemView.findViewById(R.id.btnGet)
        private val editBtn: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val toggleBtn: ImageButton = itemView.findViewById(R.id.btnToggleDescription)

        @SuppressLint("SetTextI18n")
        fun bind(entry: SmartPassword, position: Int) {
            val isExpanded = expandedStates[position] ?: false

            descriptionText.text = entry.description
            if (isExpanded) {
                descriptionText.maxLines = Int.MAX_VALUE
                descriptionText.ellipsize = null
                toggleBtn.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                descriptionText.maxLines = 1
                descriptionText.ellipsize = android.text.TextUtils.TruncateAt.END
                toggleBtn.setImageResource(android.R.drawable.arrow_down_float)
            }

            lengthText.text = "🔢 ${entry.length ?: 12} symbols"

            val shortKey = if (entry.publicKey.length > 16) {
                "🔑 ${entry.publicKey.take(12)}..."
            } else {
                "🔑 ${entry.publicKey}"
            }
            publicKeyPreview.text = shortKey

            if (entry.description.length > 20) {
                toggleBtn.visibility = View.VISIBLE

                toggleBtn.setOnClickListener {
                    val newExpandedState = !(expandedStates[position] ?: false)
                    expandedStates[position] = newExpandedState
                    notifyItemChanged(position)
                }
            } else {
                toggleBtn.visibility = View.GONE
            }

            getBtn.setOnClickListener { onGetPassword(entry) }
            editBtn.setOnClickListener { onEdit(entry) }
            deleteBtn.setOnClickListener { onDelete(entry) }
        }

        @RequiresPermission(Manifest.permission.VIBRATE)
        fun highlightForDrag() {
            cardView.strokeWidth = 8
            cardView.strokeColor = ContextCompat.getColor(context, R.color.primary_blue)
            cardView.cardElevation = 24f
            vibrateLong()
        }

        fun clearHighlight() {
            cardView.strokeWidth = 0
            cardView.cardElevation = 4f
        }
    }
}