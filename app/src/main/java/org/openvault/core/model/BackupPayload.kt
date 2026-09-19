package org.openvault.core.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenVaultExportContainer(
    val format: String = "openvault-backup",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val kdfAlgorithm: String = "PBKDF2-HMAC-SHA256",
    val kdfIterations: Int = 600_000,
    val saltBase64: String,
    val ivBase64: String,
    val encryptedPayloadBase64: String
)

@Serializable
data class DecryptedBackupData(
    val exportedAt: Long = System.currentTimeMillis(),
    val items: List<VaultItem> = emptyList(),
    val categories: List<Category> = emptyList()
)
