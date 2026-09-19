# OpenVault Security Policy & Cryptographic Specifications

**Version:** 1.0  
**Status:** Active  
**Maintained by:** OpenVault Non-Profit Privacy Foundation  

OpenVault is dedicated to the highest standards of cryptographic rigor, zero-knowledge architecture, and defensive engineering. This document outlines our security policies, vulnerability reporting procedure, and detailed cryptographic specifications.

---

## 1. Vulnerability Disclosure Policy

We take reports of security vulnerabilities seriously. We encourage responsible disclosure by security researchers, privacy advocates, and users.

### 1.1 Reporting a Vulnerability
If you identify a security flaw, cryptographic weakness, or memory leak in OpenVault:
1. **Do NOT open a public GitHub issue.**
2. Send an encrypted email to `security@openvault.org`.
3. Include:
   - A detailed description of the vulnerability.
   - Steps or proof-of-concept code to reproduce the issue.
   - The version of OpenVault, Android OS version, and device tested.
   - Any suggested mitigations.

### 1.2 Our Response Timeline
- **Acknowledgment:** Within 24 hours of receipt.
- **Triage & Validation:** Within 72 hours.
- **Remediation & Patch Release:** Critical vulnerabilities are patched and released within 7 days.
- **Public Disclosure:** Coordinated after a fix has been verified and deployed to users.

---

## 2. Cryptographic Specifications

OpenVault adheres strictly to standard, peer-reviewed cryptographic primitives. **We do not invent custom cryptography.**

### 2.1 Key Derivation Functions (KDF)
* **Master Encryption Key (MEK) Derivation:**
  * **Algorithm:** PBKDF2 with HMAC-SHA256 (RFC 2898 / RFC 8018) or Argon2id (RFC 9106).
  * **Default Iterations:** `600,000` iterations (conforming to OWASP Password Storage Guidelines).
  * **Salt:** 32 bytes (256 bits) generated via `java.security.SecureRandom`.
  * **Output Length:** 256 bits (32 bytes).
* **PIN Key Derivation:**
  * **Algorithm:** PBKDF2 with HMAC-SHA256.
  * **Iterations:** `100,000` iterations + 32-byte unique device salt.

### 2.2 Symmetric Encryption (AEAD)
* **Algorithm:** AES-256-GCM (NIST SP 800-38D).
* **Key Length:** 256 bits.
* **Initialization Vector (IV):** 12 bytes (96 bits), freshly generated for every encryption operation using CSPRNG.
* **Authentication Tag:** 16 bytes (128 bits).
* **Associated Data (AAD):** Item ID and modification timestamp are bound to the encryption context to prevent ciphertext substitution and replay attacks.
* **Tamper Resistance:** Any bit modification in ciphertext, IV, or tag triggers an immediate `AEADBadTagException`, aborting decryption.

### 2.3 Hardware Keystore Integration
* **Provider:** `AndroidKeyStore`.
* **Key Spec:** AES-256 in GCM mode with `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`.
* **Biometric Authentication:** Keys are configured with `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`.
* **Protection:** Keys are stored within the device's hardware-backed Trusted Execution Environment (TEE) or StrongBox Keymaster.

### 2.4 Time-Based One-Time Password (TOTP)
* **Standard:** RFC 6238 & RFC 4226.
* **Supported Algorithms:** HMAC-SHA1, HMAC-SHA256, HMAC-SHA512.
* **Digit Length:** 6-digit (default) and 8-digit.
* **Time Step:** 30 seconds (default) or 60 seconds.
* **Secret Storage:** Stored strictly inside the encrypted JSON payload of the vault item.

### 2.5 Random Number Generation
* **Generator:** `java.security.SecureRandom`.
* Seeded automatically by the Android Linux kernel entropy pool (`/dev/urandom`).

---

## 3. Secure Memory & Data Sanitization

1. **Zeroization:** Ephemeral byte arrays holding the Symmetric Vault Key (SVK), derived keys, or decrypted byte buffers are immediately overwritten using `java.util.Arrays.fill(bytes, 0.toByte())` upon completion of cryptographic operations or upon vault locking.
2. **String Avoidance for Sensitive Buffers:** Internal password generation and key handling utilize `CharArray` and `ByteArray` rather than immutable `java.lang.String` where feasible to prevent lingering secrets in the JVM memory heap.
3. **No Sensitive Logging:** OpenVault strictly forbids logging sensitive data (`Log.d`, `Log.e`, `System.out`). Log statements in release builds are stripped via ProGuard/R8.

---

## 4. Platform Security Hardening

* **Android Auto-Backup Disabled:** `android:allowBackup="false"` in `AndroidManifest.xml` prevents unencrypted ADB backups and Google Drive cloud backups.
* **Window Security:** `FLAG_SECURE` prevents OS screen capture, third-party screen scraping, and prevents Android's system task switcher from caching plaintext snapshots.
* **Sensitive Clipboard:** Clipboard content is flagged with `ClipDescription.EXTRA_IS_SENSITIVE` (API 33+) and automatically cleared after 30 or 60 seconds.
* **Component Isolation:** No activities, services, or content providers are exported, preventing intent injection or unauthorized IPC from malicious third-party apps.
