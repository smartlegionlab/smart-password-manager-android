/**
 * SmartPassLib v4.0.0 - Kotlin smart password generator
 * Cross-platform deterministic password generation
 * Same secret + same length = same password across all platforms
 * Decentralized by design — no central servers, no cloud dependency, no third-party trust required
 *
 * Compatible with smartpasslib Python/JS/Go/C# implementations
 *
 * Key derivation:
 * - Private key: 15-30 iterations (dynamic, deterministic per secret)
 * - Public key: 45-60 iterations (dynamic, deterministic per secret)
 *
 * Secret phrase:
 *   - is not transferred anywhere
 *   - is not stored anywhere
 *   - is required to generate the private key when creating a smart password
 *   - minimum 12 characters (enforced)
 *
 * Password length:
 *   - minimum 12 characters (enforced)
 *   - maximum 100 characters (enforced)
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

package com.smartlegionlab.smartpasslib

import java.security.MessageDigest
import java.security.SecureRandom

object SmartPassLib {

    const val VERSION = "4.0.0"

    // Character set for password generation (Google-compatible)
    // Must match exactly with Python version: symbols + uppercase + digits + lowercase
    const val CHARS = "!@#$%^&*()_+-=[]{};:,.<>?/ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz"

    // Cryptographically secure random generator
    private val secureRandom = SecureRandom()

    /**
     * SHA-256 hash function
     * @param text Text to hash
     * @return Hex string of hash (lowercase)
     */
    private fun sha256(text: String): String {
        val bytes = text.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun validateSecret(secret: String) {
        require(secret.length >= 12) {
            "Secret phrase must be at least 12 characters. Current: ${secret.length}"
        }
    }

    private fun validatePasswordLength(length: Int) {
        require(length in 12..100) {
            "Password length must be between 12 and 100. Current: $length"
        }
    }

    private fun validateCodeLength(length: Int) {
        require(length in 4..100) {
            "Code length must be between 4 and 100. Current: $length"
        }
    }

    /**
     * Get deterministic steps count from secret hash
     * @param secret Secret phrase
     * @param minSteps Minimum steps
     * @param maxSteps Maximum steps
     * @param salt Salt for different key types
     * @return Steps count
     */
    private fun getStepsFromSecret(secret: String, minSteps: Int, maxSteps: Int, salt: String): Int {
        val hashValue = sha256("$secret:$salt")
        val hashInt = hashValue.substring(0, 8).toLong(16)
        val steps = minSteps + (hashInt % (maxSteps - minSteps + 1)).toInt()
        return steps
    }

    /**
     * Generate a key from secret phrase with specified number of iterations
     * @param secret Secret phrase
     * @param steps Number of hash iterations
     * @param salt Salt for key derivation
     * @return Key hex string
     */
    private fun generateKey(secret: String, steps: Int, salt: String): String {
        validateSecret(secret)

        var allHash = sha256("$secret:$salt")

        for (i in 0 until steps) {
            val tempString = "$allHash:$i"
            allHash = sha256(tempString)
        }

        return allHash
    }

    /**
     * Generate private key from secret phrase (15-30 deterministic iterations)
     * Used for password generation, never stored or transmitted
     * @param secret Secret phrase (minimum 12 characters)
     * @return Private key hex string (64 characters = 256 bits)
     */
    fun generatePrivateKey(secret: String): String {
        validateSecret(secret)
        val steps = getStepsFromSecret(secret, 15, 30, "private")
        return generateKey(secret, steps, "private")
    }

    /**
     * Generate public key from secret phrase (45-60 deterministic iterations)
     * Used for verification, stored locally
     * @param secret Secret phrase (minimum 12 characters)
     * @return Public key hex string
     */
    fun generatePublicKey(secret: String): String {
        validateSecret(secret)
        val steps = getStepsFromSecret(secret, 45, 60, "public")
        return generateKey(secret, steps, "public")
    }

    /**
     * Verify that a secret phrase matches a stored public key
     * @param secret Secret phrase to verify
     * @param publicKey Public key to check against
     * @return True if valid
     */
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
     * @param length Desired password length (12-100)
     * @return Generated password
     */
    private fun generatePasswordFromPrivateKey(privateKey: String, length: Int): String {
        validatePasswordLength(length)

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
     * @param length Desired password length (12-100)
     * @return Generated password
     */
    fun generateSmartPassword(secret: String, length: Int): String {
        validateSecret(secret)
        validatePasswordLength(length)
        val privateKey = generatePrivateKey(secret)
        return generatePasswordFromPrivateKey(privateKey, length)
    }

    /**
     * Generate strong random password (cryptographically secure)
     * @param length Desired password length (12-100)
     * @return Generated random password
     */
    fun generateStrongPassword(length: Int): String {
        validatePasswordLength(length)

        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)

        return bytes.joinToString("") { byte ->
            val index = byte.toInt() and 0xFF
            CHARS[index % CHARS.length].toString()
        }
    }

    /**
     * Generate base random password (simpler random)
     * @param length Desired password length (12-100)
     * @return Generated random password
     */
    fun generateBasePassword(length: Int): String {
        return generateStrongPassword(length)
    }

    /**
     * Generate authentication code (shorter, for 2FA)
     * @param length Desired code length (4-100)
     * @return Generated code
     */
    fun generateCode(length: Int): String {
        validateCodeLength(length)

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