// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PasswordAdapter(
    private val onGetPassword: (PasswordEntry) -> Unit,
    private val onEdit: (PasswordEntry) -> Unit,
    private val onDelete: (PasswordEntry) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var entries: List<PasswordEntry> = emptyList()

    fun submitList(newEntries: List<PasswordEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionText: TextView = itemView.findViewById(R.id.itemDescription)
        private val lengthText: TextView = itemView.findViewById(R.id.itemLength)
        private val publicKeyPreview: TextView = itemView.findViewById(R.id.itemPublicKey)
        private val getBtn: ImageButton = itemView.findViewById(R.id.btnGet)
        private val editBtn: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(entry: PasswordEntry) {
            descriptionText.text = entry.description
            lengthText.text = "🔢 ${entry.length} symbols"
            publicKeyPreview.text = "🔑 ${entry.publicKey.take(12)}..."

            getBtn.setOnClickListener { onGetPassword(entry) }
            editBtn.setOnClickListener { onEdit(entry) }
            deleteBtn.setOnClickListener { onDelete(entry) }
        }
    }
}