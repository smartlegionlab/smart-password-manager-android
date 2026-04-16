# Smart Password Manager Android <sup>v1.0.1</sup>

---

**A secure, offline smart password manager for Android with deterministic password generation. Store only metadata — passwords never exist until you generate them.**

---

[![GitHub top language](https://img.shields.io/github/languages/top/smartlegionlab/smart-password-manager-android)](https://github.com/smartlegionlab/smart-password-manager-android)
[![GitHub license](https://img.shields.io/github/license/smartlegionlab/smart-password-manager-android)](https://github.com/smartlegionlab/smart-password-manager-android/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/smartlegionlab/smart-password-manager-android)](https://github.com/smartlegionlab/smart-password-manager-android/)
[![GitHub stars](https://img.shields.io/github/stars/smartlegionlab/smart-password-manager-android?style=social)](https://github.com/smartlegionlab/smart-password-manager-android/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/smartlegionlab/smart-password-manager-android?style=social)](https://github.com/smartlegionlab/smart-password-manager-android/network/members)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)

---

## ⚠️ Disclaimer

**By using this software, you agree to the full disclaimer terms.**

**Summary:** Software provided "AS IS" without warranty. You assume all risks.

**Full legal disclaimer:** See [DISCLAIMER.md](https://github.com/smartlegionlab/smart-password-manager-android/blob/master/DISCLAIMER.md)

---

## Core Principles

- **Zero-Password Storage**: No passwords are ever stored on your device
- **Deterministic Regeneration**: Passwords are recreated identically from your secret phrase
- **Metadata Management**: Store only descriptions and verification keys (public keys)
- **On-Device Generation**: All cryptographic operations happen locally on your Android device
- **Cross-Platform**: Same passwords as Web, Desktop, CLI, Python, Go, Kotlin implementations
- **On-Demand Discovery**: Passwords exist only when you generate them

## Key Features

- **Smart Password Generation**: Deterministic from secret phrase + length
- **Offline-First**: Works completely without internet connection
- **Cross-Platform Compatible**: Same passwords as all other SmartPassLib implementations
- **Public Key Verification**: Verify secret knowledge without exposing the secret
- **Material Design UI**: Clean, intuitive interface with dark theme support
- **Secure Secret Entry**: Hidden input with show/hide toggle
- **One-Tap Copy**: Copy generated passwords to clipboard instantly
- **Export/Import**: Backup and restore your password metadata
- **Onboarding Guide**: Step-by-step introduction for new users
- **Expandable Project Cards**: Detailed info about ecosystem components

---

## Screenshots

| Main Screen                                                                                                             | Add Password                                                                                                              | Onboarding Guide                                                                                                      |
|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| ![Main Screen](https://github.com/smartlegionlab/smart-password-manager-android/raw/master/data/images/main_screen.png) | ![Add Password](https://github.com/smartlegionlab/smart-password-manager-android/raw/master/data/images/add_password.png) | ![Onboarding](https://github.com/smartlegionlab/smart-password-manager-android/raw/master/data/images/onboarding.png) |

---

## Quick Start

### Prerequisites
- Android 8.0 (API level 26) or higher
- Storage permission (for backup/export functionality)

### Installation

1. **Download the APK** from the [Releases](https://github.com/smartlegionlab/smart-password-manager-android/releases) page
2. **Enable "Unknown Sources"** in your Android settings (if needed)
3. **Install the APK** and open the app

### First Launch

1. **Read the Legal Disclaimer** — you must accept to continue
2. **Grant Storage Permission** — required for saving backups in Documents folder
3. **Complete the Onboarding Guide** — 7 steps explaining everything you need to know
4. **Start adding your smart passwords!**

### Adding a Smart Password

1. Tap the **menu button** (three dots ⋮) in the toolbar
2. Tap the **green (+) button** to add a new entry
3. Enter a **description** (e.g., "Gmail Account")
4. Set **password length** (12-1000 characters, default 16)
5. Enter your **secret phrase** (minimum 12 characters)
6. Tap **Save** — only the public key is stored!

### Getting Your Smart Password

1. Tap the **👁️ eye icon** on any entry
2. Enter your **secret phrase** (minimum 12 characters)
3. Your smart password is **generated instantly**
4. Tap **Copy** to copy to clipboard

### Editing & Deleting

- **Edit** — Tap the ✏️ pencil icon to change description or length only
- **Delete** — Tap the 🗑️ trash icon to remove an entry
- **⚠️ Note**: Secret phrase cannot be changed — you must delete and recreate

### Export/Import

- **Export** — Use the ⋮ menu to backup your metadata to Documents folder
- **Import** — Restore from previously exported backup
- **Format**: JSON file containing descriptions, public keys, and lengths

---

## Security Model

### How It Works

| Step | Operation                          | Location     | Stored? |
|------|------------------------------------|--------------|---------|
| 1    | Enter secret phrase                | Your mind    | ❌ Never |
| 2    | Generate private key (30 iter)     | RAM only     | ❌ Never |
| 3    | Generate public key (60 iter)      | Device       | ✅ Stored |
| 4    | Generate password from private key | RAM only     | ❌ Never |

### Key Derivation (Same as all SmartPassLib implementations)

| Key Type    | Iterations | Purpose                                    | Stored? |
|-------------|------------|--------------------------------------------|---------|
| Private Key | 30         | Password generation                        | ❌ Never |
| Public Key  | 60         | Secret verification (proof of knowledge)   | ✅ Yes   |

### Character Set
```
abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$&*-_
```

### Security Requirements

| Field           | Minimum  | Default  | Maximum   |
|-----------------|----------|----------|-----------|
| Secret phrase   | 12 chars | -        | unlimited |
| Password length | 12 chars | 12 chars | 100 chars |
| Description     | 1 char   | -        | unlimited |

---

## Important Security Notes

### Secret Phrase Rules
- **Minimum 12 characters** (strictly enforced)
- Case-sensitive
- Use a mix of: uppercase, lowercase, numbers, symbols, emoji, or Cyrillic
- **Never store digitally**
- **NEVER use your password description as your secret phrase**

### Strong Secret Examples
```
✅ "MyCatHippo2026"              — mixed case + numbers
✅ "P@ssw0rd!LongSecret"         — special chars + numbers + length
✅ "КотБегемот2026НаДиете"       — Cyrillic + numbers
✅ "GitHubPersonal2026!"         — descriptive + extra chars
```

### Weak Secret Examples (AVOID)
```
❌ "Gmail Account"               — using description as secret
❌ "password"                    — dictionary word, too short
❌ "1234567890"                  — only digits, too short
❌ "qwerty123"                   — keyboard pattern
❌ Same as description           — never use the same as description
```

### Critical Warnings
- **Secret phrases are NEVER stored** — if forgotten, passwords cannot be recovered
- **Smart passwords are NEVER stored** — they only exist when generated
- **Same secret + same length = same password everywhere** (cross-platform!)
- **Export your metadata regularly** as backup

---

## Research Paradigms & Publications

- **[Pointer-Based Security Paradigm](https://github.com/smartlegionlab/pointer-based-security-paradigm)** — Architectural Shift from Data Protection to Data Non-Existence
- **[Local Data Regeneration Paradigm](https://github.com/smartlegionlab/local-data-regeneration-paradigm)** — Ontological Shift from Data Transmission to Synchronous State Discovery

---

## Technical Foundation

Powered by **[smartpasslib-kotlin](https://github.com/smartlegionlab/smartpasslib-kotlin)** — Native Kotlin implementation of deterministic password generation.

**Cryptographic Specifications:**

| Component              | Specification                      |
|------------------------|------------------------------------|
| Hash Algorithm         | SHA-256                            |
| Private Key Iterations | 30                                 |
| Public Key Iterations  | 60                                 |
| Password Generation    | Deterministic from private key     |
| Min Secret Length      | 12 characters                      |
| Password Length Range  | 12-1000 characters                 |

---

## Permissions

| Permission                    | Required For                                    |
|-------------------------------|-------------------------------------------------|
| MANAGE_EXTERNAL_STORAGE (Android 11+) | Saving backups to Documents folder      |
| WRITE_EXTERNAL_STORAGE (Android 10-)  | Export/Import functionality            |

**Note:** Storage permission is required for backup/export only. The app works completely offline and never transmits data.

---

## Cross-Platform Compatibility

Smart Password Manager Android produces **identical passwords** to:

| Platform   | Application                                                                                                               |
|------------|---------------------------------------------------------------------------------------------------------------------------|
| Web        | [Web Manager](https://github.com/smartlegionlab/smart-password-manager-web)                                               |
| Desktop    | [Desktop Manager](https://github.com/smartlegionlab/smart-password-manager-desktop)                                       |
| CLI        | [CLI PassMan](https://github.com/smartlegionlab/clipassman) / [CLI PassGen](https://github.com/smartlegionlab/clipassgen) |
| Python     | [smartpasslib](https://github.com/smartlegionlab/smartpasslib)                                                            |
| Go         | [smartpasslib-go](https://github.com/smartlegionlab/smartpasslib-go)                                                      |
| Kotlin     | [smartpasslib-kotlin](https://github.com/smartlegionlab/smartpasslib-kotlin)                                              |
| JavaScript | [smartpasslib-js](https://github.com/smartlegionlab/smartpasslib-js)                                                      |

---

## Ecosystem

This Android application is part of a comprehensive suite:

### Core Libraries
- **[smartpasslib](https://github.com/smartlegionlab/smartpasslib)** — Python implementation
- **[smartpasslib-js](https://github.com/smartlegionlab/smartpasslib-js)** — JavaScript/TypeScript implementation
- **[smartpasslib-kotlin](https://github.com/smartlegionlab/smartpasslib-kotlin)** — Kotlin implementation (used in this app)
- **[smartpasslib-go](https://github.com/smartlegionlab/smartpasslib-go)** — Go implementation

### Applications
- **[Web Manager](https://github.com/smartlegionlab/smart-password-manager-web)** — Web-based interface
- **[Desktop Manager](https://github.com/smartlegionlab/smart-password-manager-desktop)** — Cross-platform desktop app (PyQt5)
- **[Android Manager](https://github.com/smartlegionlab/smart-password-manager-android)** — Mobile Android app (this project)
- **[CLI PassMan](https://github.com/smartlegionlab/clipassman)** — Console-based password manager
- **[CLI PassGen](https://github.com/smartlegionlab/clipassgen)** — Standalone command-line generator

---

## User Interface Guide

### Main Screen
- **Password Cards** — List of all your smart password entries
- **Description** — Your custom label for each entry
- **Length** — Shows password length in symbols
- **Public Key Preview** — First 12 chars of verification key

### Toolbar Buttons
| Button        | Action                                      |
|---------------|---------------------------------------------|
| ⋮ (Menu)      | Open menu with Add, Export, Import, Help, About |
| ← (Back)      | Navigate back (when applicable)             |

### Menu Options
| Option        | Action                                      |
|---------------|---------------------------------------------|
| + Add         | Create new smart password entry             |
| Export        | Backup all metadata to JSON file            |
| Import        | Restore metadata from backup                |
| Help          | Open help documentation                     |
| About         | Show app info and ecosystem links           |

### Entry Card Buttons
| Button | Action                                                 |
|--------|--------------------------------------------------------|
| 👁️ Eye  | Generate and display smart password (requires secret)  |
| ✏️ Edit | Change description or length (secret cannot change)    |
| 🗑️ Delete| Remove entry permanently                              |

---

## Development

### Build Requirements
- Android Studio Hedgehog | 2023.1.1 or higher
- JDK 11 or higher
- Android SDK with API 34

### Build from Source

```bash
# Clone the repository
git clone https://github.com/smartlegionlab/smart-password-manager-android.git

# Open in Android Studio
# Wait for Gradle sync to complete

# Build the APK
./gradlew assembleDebug
```

---

## [License](LICENSE)

**BSD 3-Clause License**

Copyright (c) 2026, Alexander Suvorov. All rights reserved.

---

## Author

**Alexander Suvorov** — [GitHub](https://github.com/smartlegionlab)

---

## Support

- **Issues**: [GitHub Issues](https://github.com/smartlegionlab/smart-password-manager-android/issues)
- **Ecosystem**: See links in About screen

---

**Made with ❤️ for privacy and security**
