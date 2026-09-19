package org.openvault.importexport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openvault.core.model.Category
import org.openvault.core.model.CustomField
import org.openvault.core.model.FieldType
import org.openvault.core.model.ItemType
import org.openvault.core.model.VaultItem
import org.openvault.data.importexport.VaultExporter
import org.openvault.data.importexport.VaultImporter

class ImportExportTest {

    private val sampleItems = listOf(
        VaultItem(
            id = "item-1",
            title = "Proton Mail",
            type = ItemType.LOGIN,
            username = "alice@proton.me",
            password = "SecretPassword123!",
            url = "https://mail.proton.me",
            notes = "Recovery phrase stored in safe",
            totpSecret = "JBSWY3DPEHPK3PXP",
            isFavorite = true,
            customFields = listOf(
                CustomField(name = "Security Pin", value = "9876", type = FieldType.CONCEALED)
            )
        ),
        VaultItem(
            id = "item-2",
            title = "Server SSH Key",
            type = ItemType.SECURE_NOTE,
            notes = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI..."
        )
    )

    private val sampleCategories = Category.DEFAULT_CATEGORIES

    @Test
    fun testEncryptedBackupRoundTrip() {
        val backupPassword = "StrongBackupPassword999!".toCharArray()

        // Fast iterations for unit test
        val exportedJson = VaultExporter.exportEncryptedBackup(
            items = sampleItems,
            categories = sampleCategories,
            backupPassword = backupPassword,
            kdfIterations = 1000
        )

        assertNotNull(exportedJson)
        assertTrue(exportedJson.contains("openvault-backup"))

        // Decrypt with correct password
        val importedData = VaultImporter.importEncryptedBackup(exportedJson, backupPassword)
        assertEquals(2, importedData.items.size)

        val protonItem = importedData.items.find { it.title == "Proton Mail" }
        assertNotNull(protonItem)
        assertEquals("alice@proton.me", protonItem?.username)
        assertEquals("SecretPassword123!", protonItem?.password)
        assertEquals("JBSWY3DPEHPK3PXP", protonItem?.totpSecret)
        assertEquals(1, protonItem?.customFields?.size)
        assertEquals("Security Pin", protonItem?.customFields?.first()?.name)
    }

    @Test
    fun testEncryptedBackupWithIncorrectPasswordFails() {
        val backupPassword = "CorrectPassword123!".toCharArray()
        val wrongPassword = "WrongPassword456!".toCharArray()

        val exportedJson = VaultExporter.exportEncryptedBackup(
            items = sampleItems,
            categories = sampleCategories,
            backupPassword = backupPassword,
            kdfIterations = 1000
        )

        assertThrows(Exception::class.java) {
            VaultImporter.importEncryptedBackup(exportedJson, wrongPassword)
        }
    }

    @Test
    fun testPlaintextCsvExportAndImportRoundTrip() {
        val csv = VaultExporter.exportPlaintextCsv(sampleItems)
        assertTrue(csv.contains("title,type,username,password"))
        assertTrue(csv.contains("Proton Mail"))

        val importedItems = VaultImporter.importCsv(csv)
        assertEquals(2, importedItems.size)
        assertEquals("Proton Mail", importedItems[0].title)
        assertEquals("alice@proton.me", importedItems[0].username)
        assertEquals("SecretPassword123!", importedItems[0].password)
    }

    @Test
    fun testBitwardenJsonImport() {
        val bitwardenJson = """
            {
              "items": [
                {
                  "name": "GitHub",
                  "notes": "Work account",
                  "favorite": true,
                  "login": {
                    "username": "octocat",
                    "password": "CorrectHorseBatteryStaple",
                    "totp": "HXDMVJZTOSWXYZ3Z",
                    "uris": [
                      { "uri": "https://github.com" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val imported = VaultImporter.importBitwardenJson(bitwardenJson)
        assertEquals(1, imported.size)
        val item = imported[0]
        assertEquals("GitHub", item.title)
        assertEquals("octocat", item.username)
        assertEquals("CorrectHorseBatteryStaple", item.password)
        assertEquals("https://github.com", item.url)
        assertEquals("HXDMVJZTOSWXYZ3Z", item.totpSecret)
        assertTrue(item.isFavorite)
    }
}
