# Contributing to OpenVault

Thank you for your interest in contributing to OpenVault! OpenVault is an open-source, non-profit privacy initiative. We welcome contributions from developers, designers, translators, and security researchers.

---

## 1. Core Principles

Any contribution to OpenVault must align with our founding principles:
* **100% Free & Open Source:** No proprietary blobs, no paid add-ons, no freemium model.
* **Privacy First:** Zero advertising, zero telemetry, zero analytics SDKs, zero data collection.
* **Standard Cryptography:** Never implement custom or non-standard cryptographic primitives. Always use NIST/RFC standard algorithms.
* **Security & Memory Hygiene:** Sensitive memory must be zeroized; no sensitive data in logs.

---

## 2. Development Setup

### Prerequisites
- **Android SDK:** Platforms 34, 35, 36
- **JDK:** Java 17 or higher
- **Android Studio:** Ladybug / Meerkat or later (or command-line Gradle)

### Building the Project
```bash
# Clone the repository
git clone https://github.com/openvault/openvault-android.git
cd openvault-android

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 3. Pull Request Guidelines

Before submitting a Pull Request:
1. **Security Audit:** Ensure no new dependencies introduce network telemetry, analytics, or tracking.
2. **Automated Tests:** Add unit tests for any new business, cryptographic, or parsing logic.
3. **No Sensitive Logging:** Ensure no passwords, keys, tokens, or plaintext are output to `Logcat` or `System.out`.
4. **Code Style:** Follow the official Kotlin coding conventions. Format your code before opening a PR.

---

## 4. Code of Conduct

OpenVault is dedicated to providing a welcoming, inclusive, and harassment-free experience for everyone. Respectful collaboration is required in all project spaces, issues, and discussions.
