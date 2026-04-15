// Copyright (c) 2026, Alexander Suvorov. All rights reserved.
package com.example.smart_password_manager_android

data class PasswordEntry(
    val description: String,
    val publicKey: String,
    val length: Int?
)