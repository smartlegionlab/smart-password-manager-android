    // Copyright (c) 2026, Alexander Suvorov. All rights reserved.
    package com.example.smart_password_manager_android
    
    import java.security.MessageDigest
    
    object PasswordGenerator {
    
        private const val CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$&*-_"
        private const val PRIVATE_ITERATIONS = 30
        private const val PUBLIC_ITERATIONS = 60
    
        private fun sha256(text: String): String {
            val bytes = text.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    
        private fun generateKey(secret: String, iterations: Int): String {
            require(secret.length >= 12) {
                "Secret phrase must be at least 12 characters. Current: ${secret.length}"
            }
    
            var allHash = sha256(secret)
    
            for (i in 0 until iterations) {
                val tempString = "$allHash:$secret:$i"
                allHash = sha256(tempString)
            }
    
            return allHash
        }
    
        fun generatePrivateKey(secret: String): String {
            return generateKey(secret, PRIVATE_ITERATIONS)
        }
    
        fun generatePublicKey(secret: String): String {
            return generateKey(secret, PUBLIC_ITERATIONS)
        }
    
        fun verifySecret(secret: String, publicKey: String): Boolean {
            val computedKey = generatePublicKey(secret)
            return computedKey == publicKey
        }
    
        private fun hexToBytes(hex: String): ByteArray {
            val result = ByteArray(hex.length / 2)
            for (i in hex.indices step 2) {
                val byte = hex.substring(i, i + 2).toInt(16)
                result[i / 2] = byte.toByte()
            }
            return result
        }
    
        private fun generatePasswordFromPrivateKey(privateKey: String, length: Int): String {
            require(length in 12..1000) {
                "Password length must be between 12 and 1000. Current: $length"
            }
    
            val result = StringBuilder()
            var counter = 0
    
            while (result.length < length) {
                val data = "$privateKey:$counter"
                val hashHex = sha256(data)
                val hashBytes = hexToBytes(hashHex)
    
                for (byte in hashBytes) {
                    if (result.length < length) {
                        val index = byte.toInt() and 0xFF
                        result.append(CHARS[index % CHARS.length])
                    } else {
                        break
                    }
                }
                counter++
            }
    
            return result.toString()
        }
    
        fun generatePassword(secret: String, length: Int): String {
            require(secret.length >= 12) {
                "Secret phrase must be at least 12 characters. Current: ${secret.length}"
            }
            require(length in 12..1000) {
                "Password length must be between 12 and 1000. Current: $length"
            }
    
            val privateKey = generatePrivateKey(secret)
            return generatePasswordFromPrivateKey(privateKey, length)
        }
    }