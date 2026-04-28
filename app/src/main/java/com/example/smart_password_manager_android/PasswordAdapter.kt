// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

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

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun submitList(newEntries: List<SmartPassword>) {
        entries = newEntries.toMutableList()
        notifyDataSetChanged()
    }

    fun getEntries(): List<SmartPassword> = entries

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return

        val item = entries.removeAt(fromPosition)
        entries.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

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

        fun bind(entry: SmartPassword, position: Int) {
            descriptionText.text = entry.description
            lengthText.text = "🔢 ${entry.length ?: 12} symbols"
            publicKeyPreview.text = "🔑 ${entry.publicKey.take(12)}..."

            getBtn.setOnClickListener { onGetPassword(entry) }
            editBtn.setOnClickListener { onEdit(entry) }
            deleteBtn.setOnClickListener { onDelete(entry) }
        }

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