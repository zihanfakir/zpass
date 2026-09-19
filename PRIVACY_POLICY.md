# OpenVault Privacy Policy & Transparency Charter

**Effective Date:** September 19, 2026  
**Project:** OpenVault Password Manager  
**Organization:** OpenVault Non-Profit Privacy Foundation  

---

## 1. Fundamental Charter

OpenVault was created with an uncompromising mission: **to provide individuals worldwide with a completely free, mathematically secure, zero-knowledge password manager and authenticator that respects human privacy as an inviolable right.**

* **No Advertisements:** OpenVault contains zero advertising frameworks, ad networks, or sponsored links.
* **No Tracking & Analytics:** We do not include Google Analytics, Firebase, Mixpanel, Segment, Crashlytics, or any third-party behavioral analytics SDKs.
* **No Accounts or Mandatory Registration:** You do not need to register with an email address, phone number, or payment card to use OpenVault.
* **No Telemetry:** OpenVault does not "phone home." It transmits zero diagnostic logs, zero app usage telemetry, and zero device identifiers.
* **No Data Monetization:** We do not collect, buy, aggregate, broker, or sell any personal data whatsoever. There are no paid tiers, subscriptions, or freemium upsells.

---

## 2. Zero-Knowledge Architecture

OpenVault operates on a strict **Zero-Knowledge Principle**:

1. **Client-Side Derivation:** Your Master Password never leaves your device and is never written to disk in unencrypted form. Cryptographic keys are derived entirely within local device memory using memory-hard Key Derivation Functions (Argon2id / PBKDF2-HMAC-SHA256 with 600,000 rounds).
2. **Authenticated Encryption at Rest:** Every credential, note, URL, and TOTP secret is individually encrypted on your device using AES-256-GCM (Galois/Counter Mode with 128-bit authentication tags).
3. **Hardware Keystore Protection:** Quick biometric (fingerprint/face) and PIN unlock keys are secured within the Android Keystore hardware security module (TEE/StrongBox) and require direct user biometric confirmation.
4. **Export Integrity:** When exporting your vault, OpenVault defaults to strongly encrypted backups (`.openvault`) using keys derived from a passphrase of your choice.

---

## 3. Permissions Requested on Android

OpenVault requests only the absolute bare minimum Android permissions necessary for local vault functionality:

| Permission | Purpose | Why It Cannot Compromise Privacy |
| :--- | :--- | :--- |
| `android.permission.USE_BIOMETRIC` | Enables biometric unlock via system BiometricPrompt | Managed entirely by the Android OS; biometric templates remain inside the secure hardware enclave. |
| `FLAG_SECURE` (Window Flag) | Prevents screenshots and hides app previews in Android's recent apps switcher | Security feature to protect sensitive credentials from screen recorders and snooping. |

**Notice on Internet Permission:**
OpenVault's core vault application does not require internet connectivity to operate. All database transactions occur strictly inside local SQLite storage on your device.

---

## 4. Clipboard Security

When you copy a password or TOTP code to your device clipboard:
* OpenVault marks the clipboard content with `ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+ to instruct compatible keyboards and system overlays not to save the value to clipboard history.
* An automatic background cleaner wipes the clipboard memory after your configured timeout (30 or 60 seconds).

---

## 5. Third-Party Code and Open Source Verification

OpenVault is 100% open source under the Apache 2.0 License. The complete source code, build scripts, dependency manifests, and cryptographic tests are publicly auditable. Anyone can inspect, build, and verify that the binary running on their device contains no hidden backdoors, trackers, or foreign telemetry.

---

## 6. Contact & Security

If you have questions regarding this privacy charter or wish to report a security vulnerability, please refer to [docs/SECURITY.md](docs/SECURITY.md) or contact the core security team at `security@openvault.org`.
