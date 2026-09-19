# OpenVault

<p align="center">
  <strong>100% Free & Open-Source, Zero-Knowledge Password Manager & 2FA Authenticator</strong>
  <br />
  <em>Built for people, not profit. Zero ads. Zero tracking. Zero telemetry. Always free.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Privacy-Zero%20Tracking-brightgreen.svg" alt="Zero Tracking" />
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Encryption-AES--256--GCM-orange.svg" alt="AES-256-GCM" />
  <img src="https://img.shields.io/badge/KDF-PBKDF2--600k%20%7C%20Argon2id-red.svg" alt="KDF" />
</p>

---

## Mission

**OpenVault** is a production-grade, zero-knowledge password manager and TOTP authenticator developed by and for the non-profit privacy community. 

In a world where password managers increasingly monetize personal credentials behind expensive subscription walls, venture capital mandates, and invasive telemetry frameworks, OpenVault stands firmly for user sovereignty:

* **100% Free Forever:** No freemium model, no paid tiers, no "pro" features.
* **Zero Ads & Zero Tracking:** No Google Analytics, Firebase, Crashlytics, Mixpanel, or third-party trackers.
* **No Telemetry:** OpenVault operates completely offline with zero background calls home.
* **Zero-Knowledge Architecture:** The organization never receives, sees, or stores your master password or credentials.
* **Hardware-Backed Protection:** Biometric and PIN quick-unlock keys are secured via the Android Keystore (TEE / StrongBox).
* **Open & Auditable:** Transparent, clean, well-documented open-source code under the Apache 2.0 license.

---

## Features

### 1. Secure Vault
- **Credential Storage:** Store usernames, passwords, website URLs, secure notes, and dynamic custom fields (text or hidden secrets).
- **Categories & Folders:** Organize items into Logins, Secure Notes, Credit Cards, Identities, or custom categories.
- **Favorites:** Instant one-tap access to your most frequently used credentials.
- **Real-Time Fuzzy Search:** Search credentials instantly by title, username, or URL with instantaneous filtering.

### 2. Password & Passphrase Generator
- **CSPRNG Random Generation:** Cryptographically secure pseudo-random number generator (`SecureRandom`).
- **Configurable Complexity:** Length from 8 to 64 characters, toggle uppercase, lowercase, numbers, and symbols.
- **Ambiguous Character Avoidance:** Eliminates confusing glyphs (e.g., `O`/`0`, `l`/`1`/`I`).
- **Diceware Passphrase Generator:** Generates memorable multi-word passphrases with custom separators.
- **Entropy & Strength Meter:** Real-time Shannon entropy calculation and cracking time estimation.

### 3. Integrated 2FA / TOTP Authenticator
- **RFC 6238 Compliant:** Standard Time-Based One-Time Passwords with SHA-1, SHA-256, and SHA-512 support.
- **Visual Countdown Timer:** Circular animated progress indicator updating in real time.
- **One-Tap Copy:** Instant code copy to clipboard with automatic clearing.

### 4. Enterprise-Grade Defense in Depth
- **Two-Tier Key Hierarchy:** Master Encryption Key (derived via PBKDF2 600,000 rounds or Argon2id) encrypts a 256-bit Symmetric Vault Key via AES-256-GCM.
- **Biometric & PIN Quick Unlock:** Hardware-backed Keystore wraps the vault key, requiring biometric authentication without caching the master password.
- **Auto-Lock Security:** Configurable lock timeouts (Immediate, 1 min, 5 min, 15 min, screen off) automatically zeroizing volatile keys in RAM.
- **Screenshot Protection:** Window `FLAG_SECURE` blocks screen captures, screen recorders, and Android recent-apps task switcher previews.
- **Sensitive Clipboard:** Sets `EXTRA_IS_SENSITIVE` on Android 13+ and automatically clears copied passwords after 30 or 60 seconds.
- **Memory Zeroization:** Sensitive buffers (`CharArray`, `ByteArray`) are explicitly overwritten with zeroes after use.

### 5. Encrypted Backup & Import / Export
- **Encrypted Backups:** Export full vault backups encrypted with a dedicated passphrase (`.openvault`).
- **Import Compatibility:** Supports importing OpenVault encrypted JSON, Bitwarden JSON, and KeePass formats.
- **Plaintext Warning:** Plaintext export is never enabled by default; requires explicit confirmation and warning dialogs.

---

## Architecture Overview

OpenVault is engineered using **Clean Architecture**, Jetpack Compose, and unidirectional state flows (MVI/MVVM).

```
org.openvault/
├── core/
│   ├── crypto/         # AES-256-GCM, PBKDF2, Argon2, TOTP, Keystore
│   ├── model/          # Immutable domain models (VaultItem, Category)
│   ├── database/       # Local-first SQLite database & typed DAOs
│   └── security/       # VaultSession, AutoLockManager, ClipboardHelper
├── data/
│   ├── repository/     # VaultRepository & SettingsRepository
│   └── importexport/   # Encrypted backup serialization & importers
└── ui/
    ├── theme/          # Material Design 3 theme & dynamic color
    ├── components/     # Reusable Compose widgets
    ├── screens/        # Auth, Vault, Generator, Audit, Settings
    └── viewmodel/      # MVI ViewModels & StateFlows
```

Detailed technical documentation:
* [Architecture Guide](docs/ARCHITECTURE.md)
* [Security Policy & Cryptographic Specifications](docs/SECURITY.md)
* [STRIDE Threat Model](docs/THREAT_MODEL.md)
* [Key Management Lifecycle](docs/KEY_MANAGEMENT.md)

---

## Building and Verification

### Prerequisites
- Java Development Kit (JDK) 17+
- Android SDK (API 34, 35, 36)

```bash
# Build and run all unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

---

## Contributing

We welcome community contributions! Please read our [Contributing Guide](CONTRIBUTING.md) and [Code of Conduct](CONTRIBUTING.md#4-code-of-conduct) before submitting PRs.

---

## License

```
Copyright 2026 OpenVault Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
