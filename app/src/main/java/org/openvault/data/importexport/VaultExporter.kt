package org.openvault.data.importexport

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openvault.core.crypto.AesGcmCipher
import org.openvault.core.crypto.KeyDerivation
import org.openvault.core.crypto.zeroize
import org.openvault.core.model.Category
import org.openvault.core.model.DecryptedBackupData
import org.openvault.core.model.OpenVaultExportContainer
import org.openvault.core.model.VaultItem
import java.util.Base64

object VaultExporter {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Exports full vault encrypted with a user-supplied backup password using PBKDF2 + AES-256-GCM.
     */
    fun exportEncryptedBackup(
        items: List<VaultItem>,
        categories: List<Category>,
        backupPassword: CharArray,
        kdfIterations: Int = KeyDerivation.DEFAULT_ITERATIONS
    ): String {
        val backupData = DecryptedBackupData(
            exportedAt = System.currentTimeMillis(),
            items = items,
            categories = categories
        )

        val plaintextJson = json.encodeToString(backupData)
        val plaintextBytes = plaintextJson.toByteArray(Charsets.UTF_8)

        val salt = KeyDerivation.generateSalt()
        val backupKey = KeyDerivation.deriveMasterKey(backupPassword, salt, kdfIterations)

        try {
            val encOutput = AesGcmCipher.encrypt(plaintextBytes, backupKey)

            val container = OpenVaultExportContainer(
                format = "openvault-backup",
                version = 1,
                exportedAt = System.currentTimeMillis(),
                kdfAlgorithm = "PBKDF2-HMAC-SHA256",
                kdfIterations = kdfIterations,
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                ivBase64 = Base64.getEncoder().encodeToString(encOutput.iv),
                encryptedPayloadBase64 = Base64.getEncoder().encodeToString(encOutput.ciphertext)
            )

            return json.encodeToString(container)
        } finally {
            backupKey.zeroize()
        }
    }

    /**
     * Exports items in unencrypted CSV format.
     * Note: Users must be warned explicitly before this method is called.
     */
    fun exportPlaintextCsv(items: List<VaultItem>): String {
        val sb = StringBuilder()
        sb.appendLine("title,type,username,password,url,notes,totpSecret,favorite")

        for (item in items) {
            val row = listOf(
                escapeCsv(item.title),
                escapeCsv(item.type.name),
                escapeCsv(item.username),
                escapeCsv(item.password),
                escapeCsv(item.url),
                escapeCsv(item.notes.replace("\n", " ")),
                escapeCsv(item.totpSecret),
                if (item.isFavorite) "1" else "0"
            ).joinToString(",")
            sb.appendLine(row)
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
