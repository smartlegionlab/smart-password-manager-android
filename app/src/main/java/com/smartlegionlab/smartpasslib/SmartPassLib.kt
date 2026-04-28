package com.smartlegionlab.smartpasslib

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * SmartPassLib v1.0.4 - Kotlin smart password generator
 * Cross-platform deterministic password generation
 * Same secret + same length = same password across all platforms
 * Decentralized by design — no central servers, no cloud dependency, no third-party trust required
 *
 * Compatible with smartpasslib Python/JS/Go/C# implementations
 *
 * Key derivation:
 * - Private key: 30 iterations of SHA-256 (used for password generation)
 * - Public key: 60 iterations of SHA-256 (used for verification, stored locally)
 *
 * Secret phrase:
 *   - is not transferred anywhere
 *   - is not stored anywhere
 *   - is required to generate the private key when creating a smart password
 *
 * Ecosystem:
 *   - Core library (Python): https://github.com/smartlegionlab/smartpasslib
 *   - Core library (JS): https://github.com/smartlegionlab/smartpasslib-js
 *   - Core library (Kotlin): https://github.com/smartlegionlab/smartpasslib-kotlin
 *   - Core library (Go): https://github.com/smartlegionlab/smartpasslib-go
 *   - Core library (C#): https://github.com/smartlegionlab/smartpasslib-csharp
 *   - Desktop Python: https://github.com/smartlegionlab/smart-password-manager-desktop
 *   - Desktop C#: https://github.com/smartlegionlab/SmartPasswordManagerCsharpDesktop
 *   - CLI Manager Python: https://github.com/smartlegionlab/clipassman
 *   - CLI Manager C#: https://github.com/smartlegionlab/SmartPasswordManagerCsharpCli
 *   - CLI Generator Python: https://github.com/smartlegionlab/clipassgen
 *   - CLI Generator C#: https://github.com/smartlegionlab/SmartPasswordGeneratorCsharpCli
 *   - Web: https://github.com/smartlegionlab/smart-password-manager-web
 *   - Android: https://github.com/smartlegionlab/smart-password-manager-android
 *
 * Author: Alexander Suvorov https://github.com/smartlegionlab
 * License: BSD 3-Clause https://github.com/smartlegionlab/smartpasslib-kotlin/blob/master/LICENSE
 * Copyright (c) 2026, Alexander Suvorov. All rights reserved.
 */
object SmartPassLib {

    const val VERSION = "1.0.4"

    // Character set for password generation (must match other implementations)
    const val CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$&*-_"

    // Iteration counts
    private const val PRIVATE_ITERATIONS = 30  // For private key (password generation)
    private const val PUBLIC_ITERATIONS = 60   // For public key (verification, stored on server)

    // Cryptographically secure random generator
    private val secureRandom = SecureRandom()

    /**
     * SHA-256 hash function
     * @param text Text to hash
     * @return Hex string of hash
     */
    private fun sha256(text: String): String {
        val bytes = text.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate a key from secret phrase with specified number of iterations
     * @param secret Secret phrase
     * @param iterations Number of hash iterations
     * @return Key hex string
     * @throws IllegalArgumentException if secret is less than 12 characters
     */
    @Throws(IllegalArgumentException::class)
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

    /**
     * Generate private key from secret phrase (30 iterations)
     * Used for password generation, never stored or transmitted
     * @param secret Secret phrase (minimum 12 characters)
     * @return Private key hex string (64 characters = 256 bits)
     */
    @Throws(IllegalArgumentException::class)
    fun generatePrivateKey(secret: String): String {
        return generateKey(secret, PRIVATE_ITERATIONS)
    }

    /**
     * Generate public key from secret phrase (60 iterations)
     * Used for verification, stored on server
     * @param secret Secret phrase (minimum 12 characters)
     * @return Public key hex string
     */
    @Throws(IllegalArgumentException::class)
    fun generatePublicKey(secret: String): String {
        return generateKey(secret, PUBLIC_ITERATIONS)
    }

    /**
     * Verify that a secret phrase matches a stored public key
     * @param secret Secret phrase to verify
     * @param publicKey Public key to check against
     * @return True if valid
     */
    @Throws(IllegalArgumentException::class)
    fun verifySecret(secret: String, publicKey: String): Boolean {
        val computedKey = generatePublicKey(secret)
        return computedKey == publicKey
    }

    /**
     * Convert hex string to byte array
     * @param hex Hex string
     * @return Byte array
     */
    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in hex.indices step 2) {
            val byte = hex.substring(i, i + 2).toInt(16)
            result[i / 2] = byte.toByte()
        }
        return result
    }

    /**
     * Generate deterministic smart password from private key
     * @param privateKey Private key hex string (from generatePrivateKey)
     * @param length Desired password length (min 12, max 1000)
     * @return Generated password
     */
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

    /**
     * Generate deterministic smart password directly from secret phrase
     * This is the main method for end users
     * @param secret Secret phrase (minimum 12 characters)
     * @param length Desired password length (min 12, max 1000)
     * @return Generated password
     */
    @Throws(IllegalArgumentException::class)
    fun generateSmartPassword(secret: String, length: Int): String {
        require(secret.length >= 12) {
            "Secret phrase must be at least 12 characters. Current: ${secret.length}"
        }
        require(length in 12..1000) {
            "Password length must be between 12 and 1000. Current: $length"
        }

        val privateKey = generatePrivateKey(secret)
        return generatePasswordFromPrivateKey(privateKey, length)
    }

    /**
     * Generate strong random password (cryptographically secure)
     * @param length Desired password length (min 12, max 1000)
     * @return Generated random password
     */
    @Throws(IllegalArgumentException::class)
    fun generateStrongPassword(length: Int): String {
        require(length in 12..1000) {
            "Password length must be between 12 and 1000. Current: $length"
        }

        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)

        return bytes.joinToString("") { byte ->
            val index = byte.toInt() and 0xFF
            CHARS[index % CHARS.length].toString()
        }
    }

    /**
     * Generate base random password (simpler random)
     * @param length Desired password length (min 12, max 1000)
     * @return Generated random password
     */
    @Throws(IllegalArgumentException::class)
    fun generateBasePassword(length: Int): String {
        return generateStrongPassword(length)
    }

    /**
     * Generate authentication code (shorter, for 2FA)
     * @param length Desired code length (min 4, max 20)
     * @return Generated code
     */
    @Throws(IllegalArgumentException::class)
    fun generateCode(length: Int): String {
        require(length in 4..20) {
            "Code length must be between 4 and 20. Current: $length"
        }

        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)

        return bytes.joinToString("") { byte ->
            val index = byte.toInt() and 0xFF
            CHARS[index % CHARS.length].toString()
        }
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