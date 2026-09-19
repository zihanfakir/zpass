# OpenVault Threat Model

This document presents a structured security analysis of the OpenVault Android application based on the **STRIDE** methodology (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, and Elevation of Privilege).

---

## 1. System Assumptions and Trust Boundaries

### 1.1 In-Scope Assets
1. **Master Password & Derived Keys (MEK, PIN Key, Keystore Key)**
2. **Symmetric Vault Key (SVK)**
3. **Stored Credentials (Usernames, Passwords, URLs, Secure Notes, Custom Fields)**
4. **TOTP Seeds & Generated Tokens**
5. **Encrypted Backup Files (`.openvault`)**

### 1.2 Trust Boundaries
* **Boundary 1 (User / App Interface):** Visual display and touch inputs on the Android device.
* **Boundary 2 (In-Memory Realm):** JVM Heap, volatile RAM holding the active `VaultSession`.
* **Boundary 3 (Persistent Storage Realm):** Local SQLite storage, SharedPreferences, File system.
* **Boundary 4 (External / Android OS):** Other third-party apps, Android framework services (Clipboard, NotificationManager, WindowManager).

### 1.3 Out-of-Scope Threats
* Fully compromised Linux kernel / hostile rootkit / kernel-level memory scraping on a compromised OS.
* Physical hardware tampering / electron microscope extraction of TEE silicon.
* User entering master password while being physically observed (shoulder surfing).

---

## 2. STRIDE Analysis Matrix

| Threat Category | Specific Attack Vector | Risk Level | Architectural Mitigation | Residual Risk |
| :--- | :--- | :--- | :--- | :--- |
| **Spoofing** | Malicious app attempts to invoke OpenVault activities to extract data | High | All app components are non-exported (`exported="false"`), except the single launcher activity with zero input parameters. | Negligible |
| **Spoofing** | Impersonation via forged biometric authentication | Medium | `BiometricPrompt` uses `CryptoObject` bound directly to an authenticated Keystore AES key; biometric template remains inside secure hardware enclave. | Bound to device biometric sensor security |
| **Tampering** | Malicious app or rooted user alters encrypted SQLite database records | High | AES-256-GCM AEAD enforces 128-bit MAC tag verification. Tampering causes `AEADBadTagException`, safely aborting decryption. | Tampered item cannot be recovered (DoS), but cannot be injected |
| **Tampering** | Modification of encrypted backup files during transit/import | High | Backups include HMAC / GCM authentication tag over the entire encrypted payload. | Any modified bit causes import rejection |
| **Repudiation** | Desynchronization or conflicting offline edits | Low | Deterministic revision timestamps (epoch ms) and UUID tracking ensure conflict resolution order. | None |
| **Information Disclosure** | Memory dump of heap when app is backgrounded | High | • Active `VaultSession` key is explicitly wiped via `zeroize()`.<br>• `AutoLockManager` terminates session when app is sent to background. | Ephemeral RAM access during active unlock window |
| **Information Disclosure** | Snooping via screen recordings or Android recent apps switcher | Medium | Window `FLAG_SECURE` is active on all vault screens, rendering screenshots and task switcher previews blank. | None on non-rooted Android OS |
| **Information Disclosure** | Clipboard snooping by background clipboard listener apps | High | • Marked with `EXTRA_IS_SENSITIVE` on Android 13+.<br>• Background coroutine purges clipboard after 30s/60s. | Third-party app reading clipboard within the 30s window |
| **Information Disclosure** | Unencrypted cloud / ADB extraction | High | `android:allowBackup="false"` in manifest blocks ADB and Google Drive automatic backups. | User manual export (warned if plaintext) |
| **Denial of Service** | Offline brute-force dictionary attacks against database or backup | High | 600,000 rounds of PBKDF2-HMAC-SHA256 (or Argon2id) with 32-byte salt ensures each password attempt takes ~500ms on CPU, making bulk cracking impractical. | Weak user master passwords |
| **Elevation of Privilege** | Exploit vulnerable third-party analytics/ad libraries | Critical | **Zero third-party analytics or advertising SDKs exist in OpenVault.** Clean, auditable, minimal dependency tree. | Eliminated |

---

## 3. Cryptographic Assumptions

1. **AES-256-GCM Security:** AES is cryptographically secure with no practical attacks against 256-bit keys; GCM provides 128-bit integrity authentication.
2. **IV Uniqueness:** Every encryption operation utilizes a freshly generated 12-byte CSPRNG IV. No IV is ever reused under the same key.
3. **Entropy Source:** Android's `SecureRandom` uses the Linux kernel's cryptographically secure PRNG (`/dev/urandom`).
