// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

import com.google.gson.Gson

data class QrCodeData(
    val l: Int,
    val k: String
) {
    companion object {
        fun fromJson(json: String): QrCodeData? {
            return try {
                Gson().fromJson(json, QrCodeData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}