# Migration Guide: v1.x.x to v4.0.0

> **📌 Version Note:** Smart Password Manager Android jumps from v1.x.x directly to v4.0.0 to align with smartpasslib-kotlin v4.0.0. All smartpasslib implementations (Python, C#, JS, Go, Kotlin) now share the same version number and algorithm.

## ⚠️ Breaking Change Notice

**Smart Password Manager Android v4.0.0 is NOT backward compatible with v1.x.x**

| Version    | Status      | Why                                                              |
|------------|-------------|------------------------------------------------------------------|
| v1.x.x     | Deprecated  | Fixed iterations (30/60), limited character set                  |
| **v4.0.0** | **Current** | Dynamic iterations (15-30/45-60), expanded charset, max security |

Smart passwords generated with v1.x.x cannot be regenerated using v4.0.0 due to fundamental changes in the deterministic generation algorithm.

---

## Why the change?

**Smart Password Manager Android v4.0.0 introduces fundamental improvements:**

- **Dynamic iteration counts** — deterministic steps vary per secret (15-30 for private, 45-60 for public)
- **Expanded character set** — Google-compatible symbols (26 special chars + A-Z + a-z + 0-9)
- **Enhanced key derivation** — salt separation for public/private keys ("private"/"public")
- **Unified length validation** — password length must be 12-100 characters (was 12-1000)
- **Input validation** — secret phrases must be at least 12 characters (enforced)
- **Maximum security** — no secret exposure in iterations
- **Cross-platform consistency** — identical algorithm with all smartpasslib implementations

---

## What changed in the Android app:

| Aspect                 | v1.x.x           | v4.0.0                                |
|------------------------|------------------|---------------------------------------|
| Private key iterations | Fixed 30         | Dynamic 15-30                         |
| Public key iterations  | Fixed 60         | Dynamic 45-60                         |
| Character set          | `abc...!@#$&*-_` | `!@#$%^&*()_+-=[]{};:,.<>?/A-Za-z0-9` |
| Password max length    | 1000             | 100                                   |
| Secret validation      | Min 4 chars      | Min 12 characters (enforced)          |
| Code max length        | 20               | 100                                   |
| Key derivation salt    | None             | "private"/"public"                    |
| Secret in iterations   | Yes (exposed)    | No (secure)                           |

---

## Data File Compatibility

**The old `passwords.json` file is NOT compatible with v4.0.0**

Public keys stored in v1.x.x files cannot be used with v4.0.0 because:
- Iteration counts changed from fixed 60 to dynamic 45-60
- Salt "public" was added to key derivation

**Result:** Old entries cannot be verified. Passwords cannot be regenerated.

**No automatic migration is provided** — you need to recreate entries manually.

---

## Migration Steps

### Step 1: Retrieve existing passwords using old version

**BEFORE UPGRADING**, retrieve all actual passwords from your current v1.x.x app:

1. Open the old version of Smart Password Manager Android
2. For each entry, tap the **👁️** (eye) icon
3. Enter your secret phrase
4. **Copy the generated password** and save it somewhere safe (e.g., a text file, another password manager)
5. Repeat for all entries

> ⚠️ **CRITICAL:** Do this BEFORE updating! After v4.0.0, you will NOT be able to regenerate old passwords.

### Step 2: Backup your old data file

The old `passwords.json` file is located at:
```
Documents/smart_password_manager_android/passwords.json
```

Back it up for reference (though it won't work with v4.0.0):

### Step 3: Upgrade to v4.0.0

Install the new APK via releases.

### Step 4: Clear old data (optional but recommended)

Since the old `passwords.json` is incompatible, you have two options:

**Option A: Start fresh (recommended)**
- Uninstall the old app completely before installing v4.0.0
- Or manually delete: `Documents/smart_password_manager_android/` folder

**Option B: Keep old data (will cause errors)**
- Old entries will appear but verification will FAIL
- You will need to delete each old entry manually

### Step 5: Recreate your entries

Using the **SAME secret phrases and lengths**, recreate all password entries:

1. Tap the **green [+]** button (FAB menu)
2. Enter description (e.g., "Gmail")
3. Set the **same length** as before (12-100 only — you must choose a new length)
4. Enter your secret phrase (minimum 12 characters)
5. Confirm secret phrase
6. Save

### Step 6: Generate new passwords

For each recreated entry:
1. Tap the **👁️** eye icon
2. Enter the same secret phrase
3. Copy the **new** generated password

### Step 7: Update your services

Replace old passwords (from Step 1) with newly generated ones (from Step 6) on each website/service.

### Step 8: Verify

- Log in to each service using the new password
- Confirm the app generates the same password every time (same secret → same password)

---

## What About QR Code Import?

**QR codes generated by v1.x.x are NOT compatible with v4.0.0**

- Old QR codes contain public keys from v1.x.x algorithm
- v4.0.0 will reject them because verification will fail
- You must generate new QR codes from v4.0.0 if you need to share entries

---

## What About Exported JSON Files?

**Exported JSON files from v1.x.x contain incompatible public keys**

- The export format is identical, but the public keys inside are NOT compatible
- Importing an old JSON file into v4.0.0 will fail verification
- All entries must be recreated manually

---

## Important Notes

- **No automatic migration** — manual regeneration required for each password
- **No data migration** — old `passwords.json` is incompatible
- **Your secret phrases remain the same** — only generated passwords change
- **Secret phrases shorter than 12 characters will now be rejected**
- **Old passwords still work** on services until you change them
- **Test with non-essential accounts first**
- **Keep your old APK backup** in case you need to retrieve more passwords

---

## Rollback

If you need to rollback to v1.x.x:

1. Uninstall v4.0.0
2. Reinstall the old APK
3. Restore your old `passwords.json` backup to `Documents/smart_password_manager_android/`
4. Your old passwords will work again (but you'll still need to migrate eventually)

---

## Need Help?

- **Issues**: [GitHub Issues](https://github.com/smartlegionlab/smart-password-manager-android/issues)
- **Core Library Issues**: [smartpasslib Issues](https://github.com/smartlegionlab/smartpasslib/issues)
- **Migration Questions**: Open an issue with tag `migration`

---
