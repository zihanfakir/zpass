package org.openvault.data.importexport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.openvault.core.crypto.AesGcmCipher
import org.openvault.core.crypto.KeyDerivation
import org.openvault.core.crypto.zeroize
import org.openvault.core.model.DecryptedBackupData
import org.openvault.core.model.ItemType
import org.openvault.core.model.OpenVaultExportContainer
import org.openvault.core.model.VaultItem
import java.util.Base64

object VaultImporter {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Imports an encrypted OpenVault container (.openvault JSON string).
     */
    fun importEncryptedBackup(
        jsonString: String,
        backupPassword: CharArray
    ): DecryptedBackupData {
        val container = json.decodeFromString<OpenVaultExportContainer>(jsonString)
        val salt = Base64.getDecoder().decode(container.saltBase64)
        val iv = Base64.getDecoder().decode(container.ivBase64)
        val ciphertext = Base64.getDecoder().decode(container.encryptedPayloadBase64)

        val backupKey = KeyDerivation.deriveMasterKey(
            backupPassword,
            salt,
            container.kdfIterations
        )

        try {
            val decryptedBytes = AesGcmCipher.decrypt(ciphertext, backupKey, iv)
            val decryptedJson = String(decryptedBytes, Charsets.UTF_8)
            return json.decodeFromString<DecryptedBackupData>(decryptedJson)
        } finally {
            backupKey.zeroize()
        }
    }

    /**
     * Imports standard Bitwarden JSON export.
     */
    fun importBitwardenJson(jsonString: String): List<VaultItem> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val itemsArray = root["items"]?.jsonArray ?: return emptyList()

        val parsedItems = mutableListOf<VaultItem>()
        for (el in itemsArray) {
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "Imported Item"
            val notes = obj["notes"]?.jsonPrimitive?.contentOrNull ?: ""
            val favorite = obj["favorite"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false

            val login = obj["login"]?.jsonObject
            val username = login?.get("username")?.jsonPrimitive?.contentOrNull ?: ""
            val password = login?.get("password")?.jsonPrimitive?.contentOrNull ?: ""
            val totp = login?.get("totp")?.jsonPrimitive?.contentOrNull ?: ""

            val uris = login?.get("uris")?.jsonArray
            val url = uris?.firstOrNull()?.jsonObject?.get("uri")?.jsonPrimitive?.contentOrNull ?: ""

            parsedItems.add(
                VaultItem(
                    title = name,
                    type = ItemType.LOGIN,
                    username = username,
                    password = password,
                    url = url,
                    notes = notes,
                    totpSecret = totp,
                    isFavorite = favorite
                )
            )
        }
        return parsedItems
    }

    /**
     * Imports credentials from a CSV formatted string.
     */
    fun importCsv(csvString: String): List<VaultItem> {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val header = parseCsvLine(lines[0]).map { it.trim().lowercase() }
        val titleIdx = header.indexOfFirst { it.contains("title") || it.contains("name") }
        val userIdx = header.indexOfFirst { it.contains("user") || it.contains("login") || it.contains("email") }
        val passIdx = header.indexOfFirst { it.contains("password") || it.contains("pass") }
        val urlIdx = header.indexOfFirst { it.contains("url") || it.contains("web") }
        val noteIdx = header.indexOfFirst { it.contains("note") || it.contains("comment") }
        val totpIdx = header.indexOfFirst { it.contains("totp") || it.contains("otp") || it.contains("2fa") }

        val items = mutableListOf<VaultItem>()
        for (i in 1 until lines.size) {
            val cols = parseCsvLine(lines[i])
            if (cols.isEmpty()) continue

            val title = if (titleIdx in cols.indices) cols[titleIdx] else "Item $i"
            val username = if (userIdx in cols.indices) cols[userIdx] else ""
            val password = if (passIdx in cols.indices) cols[passIdx] else ""
            val url = if (urlIdx in cols.indices) cols[urlIdx] else ""
            val notes = if (noteIdx in cols.indices) cols[noteIdx] else ""
            val totp = if (totpIdx in cols.indices) cols[totpIdx] else ""

            items.add(
                VaultItem(
                    title = title.ifBlank { "Untitled $i" },
                    type = ItemType.LOGIN,
                    username = username,
                    password = password,
                    url = url,
                    notes = notes,
                    totpSecret = totp
                )
            )
        }
        return items
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = java.lang.StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(cur.toString().trim())
                    cur.clear()
                }
                else -> cur.append(c)
            }
        }
        result.add(cur.toString().trim())
        return result
    }
}
