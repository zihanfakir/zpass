# OpenVault Key Management & Cryptographic Lifecycle

This document describes the two-tier zero-knowledge cryptographic key hierarchy, key derivation parameters, hardware Keystore binding, and in-memory key lifecycle used by OpenVault.

---

## 1. Key Hierarchy

```
┌──────────────────────────────────────────────────────────────┐
│                    Master Password (User)                    │
└──────────────────────────────┬───────────────────────────────┘
                               │ + 32-byte CSPRNG Salt
                               ▼
        PBKDF2-HMAC-SHA256 (600,000 rounds) / Argon2id
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             Master Encryption Key (MEK) [256-bit]            │
└──────────────────────────────┬───────────────────────────────┘
                               │ AES-256-GCM Encrypts
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             Symmetric Vault Key (SVK) [256-bit]              │
│               (Generated once via CSPRNG)                    │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
               ▼ AES-256-GCM                   ▼ AES-256-GCM
┌──────────────────────────────┐ ┌─────────────────────────────┐
│    Encrypted Vault Items     │ │   Biometric/PIN Wrapped Key │
│  (Ciphertext + 12-byte IV +  │ │   (Android Keystore TEE or  │
│      16-byte Auth Tag)       │ │     PIN-derived KDF key)    │
└──────────────────────────────┘ └─────────────────────────────┘
```

---

## 2. Key Derivation Specifications

### 2.1 Master Encryption Key (MEK)
- **Input:** User-provided Master Password (char array).
- **Salt:** 32 bytes (256 bits), generated via `SecureRandom` on initial vault creation and stored in `vault_metadata`.
- **Algorithm:** PBKDF2 with HMAC-SHA256 (RFC 8018) or Argon2id (RFC 9106).
- **Work Factor:** `600,000` iterations (OWASP recommendation).
- **Derived Output:** 256 bits (32 bytes).
- **Lifecycle:** Generated ephemerally during unlock; immediately wiped from memory after decrypting the SVK.

### 2.2 Symmetric Vault Key (SVK)
- **Generation:** 256 bits of cryptographic entropy generated once via `SecureRandom.nextBytes(32)` during vault initialization.
- **Role:** Encrypts and decrypts all vault items, categories, and custom fields.
- **Storage:** Stored in `vault_metadata` table as ciphertext encrypted by the MEK (`encrypted_vault_key`).
- **Memory Lifecycle:** Kept in volatile memory inside `VaultSession` while the vault is in the `Unlocked` state. Wiped upon timeout, app backgrounding, or manual lock.

### 2.3 Biometric Wrapped Key
- **Hardware Enclave:** An AES-256 key is generated inside the Android Keystore (`AndroidKeyStore`) with alias `openvault_biometric_key`.
- **User Authentication:** Configured with `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`.
- **Role:** Encrypts the SVK into `biometric_wrapped_key`.
- **Unlock Sequence:** Successful `BiometricPrompt` authentication authorizes the Keystore to decrypt `biometric_wrapped_key`, reconstructing the SVK in RAM without prompting for the Master Password.

### 2.4 Quick PIN Wrapped Key
- **Input:** 4 to 8 digit numeric PIN.
- **Salt:** 32-byte independent device salt.
- **Algorithm:** PBKDF2-HMAC-SHA256 (100,000 iterations).
- **Role:** Derives a 256-bit PIN key that encrypts the SVK into `pin_wrapped_key`.

---

## 3. In-Memory Key Hygiene & Zeroization

To defend against heap-inspection attacks, memory dumps, and lingering garbage collection buffers:

1. **Zeroization:** Sensitive byte and char buffers implement the `zeroize()` extension:
   ```kotlin
   fun ByteArray.zeroize() {
       Arrays.fill(this, 0.toByte())
   }
   
   fun CharArray.zeroize() {
       Arrays.fill(this, '\u0000')
   }
   ```
2. **Session Termination:** When the vault transitions from `Unlocked` to `Locked`, `VaultSession.destroy()` is invoked immediately:
   ```kotlin
   fun destroy() {
       vaultKey?.zeroize()
       vaultKey = null
   }
   ```
3. **No Garbage Collection Reliance:** OpenVault does not wait for JVM garbage collection to purge cryptographic keys. Keys are explicitly overwritten in place.
