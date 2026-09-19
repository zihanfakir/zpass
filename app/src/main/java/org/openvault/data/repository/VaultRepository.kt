package org.openvault.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openvault.core.crypto.AesGcmCipher
import org.openvault.core.crypto.KeyDerivation
import org.openvault.core.crypto.KeystoreManager
import org.openvault.core.crypto.PasswordGenerator
import org.openvault.core.crypto.zeroize
import org.openvault.core.database.OpenVaultDbHelper
import org.openvault.core.model.Category
import org.openvault.core.model.CustomField
import org.openvault.core.model.ItemType
import org.openvault.core.model.StrengthRating
import org.openvault.core.model.VaultItem
import org.openvault.core.security.VaultSession
import javax.crypto.SecretKey

@Serializable
private data class EncryptedItemPayload(
    val type: ItemType = ItemType.LOGIN,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val customFields: List<CustomField> = emptyList()
)

data class VaultAuditReport(
    val totalItems: Int,
    val weakItems: List<VaultItem>,
    val reusedPasswordsCount: Map<String, Int>,
    val reusedItems: List<VaultItem>,
    val missing2faLogins: List<VaultItem>
)

class VaultRepository(context: Context) {

    private val dbHelper = OpenVaultDbHelper(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _itemsFlow = MutableStateFlow<List<VaultItem>>(emptyList())
    val itemsFlow: StateFlow<List<VaultItem>> = _itemsFlow.asStateFlow()

    private val _categoriesFlow = MutableStateFlow<List<Category>>(Category.DEFAULT_CATEGORIES)
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    init {
        // Initialize lock state based on whether vault has been created
        VaultSession.setInitialState(hasVault())
        if (hasVault()) {
            scope.launch {
                loadCategories()
            }
        }
    }

    fun hasVault(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${OpenVaultDbHelper.COL_META_VALUE} FROM ${OpenVaultDbHelper.TABLE_METADATA} WHERE ${OpenVaultDbHelper.COL_META_KEY} = ?",
            arrayOf(OpenVaultDbHelper.KEY_ENCRYPTED_VAULT_KEY)
        )
        val exists = cursor.use { it.moveToFirst() }
        return exists
    }

    fun isBiometricEnabled(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${OpenVaultDbHelper.COL_META_VALUE} FROM ${OpenVaultDbHelper.TABLE_METADATA} WHERE ${OpenVaultDbHelper.COL_META_KEY} = ?",
            arrayOf(OpenVaultDbHelper.KEY_BIOMETRIC_WRAPPED_KEY)
        )
        return cursor.use { it.moveToFirst() }
    }

    fun isPinEnabled(): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${OpenVaultDbHelper.COL_META_VALUE} FROM ${OpenVaultDbHelper.TABLE_METADATA} WHERE ${OpenVaultDbHelper.COL_META_KEY} = ?",
            arrayOf(OpenVaultDbHelper.KEY_PIN_WRAPPED_KEY)
        )
        return cursor.use { it.moveToFirst() }
    }

    suspend fun createVault(masterPassword: CharArray) = withContext(Dispatchers.IO) {
        val salt = KeyDerivation.generateSalt()
        val vaultKey = KeyDerivation.generateVaultKey()
        val mek = KeyDerivation.deriveMasterKey(masterPassword, salt)

        try {
            val encVaultKey = AesGcmCipher.encrypt(vaultKey, mek)

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                setMetadata(db, OpenVaultDbHelper.KEY_SALT, salt.toHexString())
                setMetadata(db, OpenVaultDbHelper.KEY_ENCRYPTED_VAULT_KEY, encVaultKey.ciphertext.toHexString())
                setMetadata(db, OpenVaultDbHelper.KEY_VAULT_KEY_IV, encVaultKey.iv.toHexString())
                setMetadata(db, OpenVaultDbHelper.KEY_KDF_ITERATIONS, KeyDerivation.DEFAULT_ITERATIONS.toString())
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            VaultSession.unlock(vaultKey)
            loadCategories()
            refreshItems()
        } finally {
            mek.zeroize()
            vaultKey.zeroize()
        }
    }

    suspend fun unlockWithMasterPassword(masterPassword: CharArray): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val saltHex = getMetadata(db, OpenVaultDbHelper.KEY_SALT) ?: return@withContext false
        val encVaultKeyHex = getMetadata(db, OpenVaultDbHelper.KEY_ENCRYPTED_VAULT_KEY) ?: return@withContext false
        val ivHex = getMetadata(db, OpenVaultDbHelper.KEY_VAULT_KEY_IV) ?: return@withContext false
        val iterations = getMetadata(db, OpenVaultDbHelper.KEY_KDF_ITERATIONS)?.toIntOrNull()
            ?: KeyDerivation.DEFAULT_ITERATIONS

        val salt = hexToByteArray(saltHex)
        val encVaultKey = hexToByteArray(encVaultKeyHex)
        val iv = hexToByteArray(ivHex)

        val mek = KeyDerivation.deriveMasterKey(masterPassword, salt, iterations)
        return@withContext try {
            val vaultKey = AesGcmCipher.decrypt(encVaultKey, mek, iv)
            VaultSession.unlock(vaultKey)
            vaultKey.zeroize()
            loadCategories()
            refreshItems()
            true
        } catch (e: Exception) {
            false
        } finally {
            mek.zeroize()
        }
    }

    suspend fun unlockWithPin(pin: CharArray): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val pinSaltHex = getMetadata(db, OpenVaultDbHelper.KEY_PIN_SALT) ?: return@withContext false
        val pinWrappedHex = getMetadata(db, OpenVaultDbHelper.KEY_PIN_WRAPPED_KEY) ?: return@withContext false
        val pinIvHex = getMetadata(db, OpenVaultDbHelper.KEY_PIN_IV) ?: return@withContext false

        val salt = hexToByteArray(pinSaltHex)
        val pinWrappedKey = hexToByteArray(pinWrappedHex)
        val iv = hexToByteArray(pinIvHex)

        val pinKey = KeyDerivation.derivePinKey(pin, salt)
        return@withContext try {
            val vaultKey = AesGcmCipher.decrypt(pinWrappedKey, pinKey, iv)
            VaultSession.unlock(vaultKey)
            vaultKey.zeroize()
            loadCategories()
            refreshItems()
            true
        } catch (e: Exception) {
            false
        } finally {
            pinKey.zeroize()
        }
    }

    suspend fun enablePin(pin: CharArray) = withContext(Dispatchers.IO) {
        val vaultKey = VaultSession.requireVaultKey()
        val salt = KeyDerivation.generateSalt()
        val pinKey = KeyDerivation.derivePinKey(pin, salt)
        try {
            val encPinKey = AesGcmCipher.encrypt(vaultKey, pinKey)
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                setMetadata(db, OpenVaultDbHelper.KEY_PIN_SALT, salt.toHexString())
                setMetadata(db, OpenVaultDbHelper.KEY_PIN_WRAPPED_KEY, encPinKey.ciphertext.toHexString())
                setMetadata(db, OpenVaultDbHelper.KEY_PIN_IV, encPinKey.iv.toHexString())
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            pinKey.zeroize()
        }
    }

    suspend fun disablePin() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(OpenVaultDbHelper.TABLE_METADATA, "${OpenVaultDbHelper.COL_META_KEY} LIKE 'pin_%'", null)
    }

    suspend fun enableBiometric(secretKey: SecretKey) = withContext(Dispatchers.IO) {
        val vaultKey = VaultSession.requireVaultKey()
        val output = KeystoreManager.wrapVaultKey(vaultKey, secretKey)
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            setMetadata(db, OpenVaultDbHelper.KEY_BIOMETRIC_WRAPPED_KEY, output.ciphertext.toHexString())
            setMetadata(db, OpenVaultDbHelper.KEY_BIOMETRIC_IV, output.iv.toHexString())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun unlockWithBiometric(secretKey: SecretKey): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val wrappedHex = getMetadata(db, OpenVaultDbHelper.KEY_BIOMETRIC_WRAPPED_KEY) ?: return@withContext false
        val ivHex = getMetadata(db, OpenVaultDbHelper.KEY_BIOMETRIC_IV) ?: return@withContext false

        val wrappedKey = hexToByteArray(wrappedHex)
        val iv = hexToByteArray(ivHex)

        return@withContext try {
            val vaultKey = KeystoreManager.unwrapVaultKey(wrappedKey, iv, secretKey)
            VaultSession.unlock(vaultKey)
            vaultKey.zeroize()
            loadCategories()
            refreshItems()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun disableBiometric() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(OpenVaultDbHelper.TABLE_METADATA, "${OpenVaultDbHelper.COL_META_KEY} LIKE 'biometric_%'", null)
        KeystoreManager.deleteBiometricKey()
    }

    suspend fun refreshItems() = withContext(Dispatchers.IO) {
        if (!VaultSession.isUnlocked) {
            _itemsFlow.value = emptyList()
            return@withContext
        }

        val vaultKey = VaultSession.requireVaultKey()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            OpenVaultDbHelper.TABLE_ITEMS,
            null,
            "${OpenVaultDbHelper.COL_ITEM_IS_DELETED} = 0",
            null,
            null,
            null,
            "${OpenVaultDbHelper.COL_ITEM_TITLE} COLLATE NOCASE ASC"
        )

        val items = mutableListOf<VaultItem>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_ID)
            val titleIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_TITLE)
            val catIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_CATEGORY_ID)
            val payloadIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_PAYLOAD)
            val ivIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_IV)
            val favIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_IS_FAVORITE)
            val createdIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_CREATED_AT)
            val updatedIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_ITEM_UPDATED_AT)

            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val title = it.getString(titleIdx)
                val categoryId = it.getString(catIdx)
                val encryptedPayload = it.getBlob(payloadIdx)
                val iv = it.getBlob(ivIdx)
                val isFavorite = it.getInt(favIdx) == 1
                val createdAt = it.getLong(createdIdx)
                val updatedAt = it.getLong(updatedIdx)

                try {
                    val decryptedBytes = AesGcmCipher.decrypt(encryptedPayload, vaultKey, iv)
                    val payloadJson = String(decryptedBytes, Charsets.UTF_8)
                    val payload = json.decodeFromString<EncryptedItemPayload>(payloadJson)

                    items.add(
                        VaultItem(
                            id = id,
                            title = title,
                            type = payload.type,
                            categoryId = categoryId,
                            username = payload.username,
                            password = payload.password,
                            url = payload.url,
                            notes = payload.notes,
                            totpSecret = payload.totpSecret,
                            customFields = payload.customFields,
                            isFavorite = isFavorite,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                } catch (e: Exception) {
                    // Item failed decryption (corrupted or tampered)
                }
            }
        }
        _itemsFlow.value = items
    }

    suspend fun saveItem(item: VaultItem) = withContext(Dispatchers.IO) {
        val vaultKey = VaultSession.requireVaultKey()
        val payload = EncryptedItemPayload(
            type = item.type,
            username = item.username,
            password = item.password,
            url = item.url,
            notes = item.notes,
            totpSecret = item.totpSecret,
            customFields = item.customFields
        )

        val payloadJson = json.encodeToString(payload)
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val encrypted = AesGcmCipher.encrypt(payloadBytes, vaultKey)

        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(OpenVaultDbHelper.COL_ITEM_ID, item.id)
            put(OpenVaultDbHelper.COL_ITEM_TITLE, item.title)
            put(OpenVaultDbHelper.COL_ITEM_CATEGORY_ID, item.categoryId)
            put(OpenVaultDbHelper.COL_ITEM_PAYLOAD, encrypted.ciphertext)
            put(OpenVaultDbHelper.COL_ITEM_IV, encrypted.iv)
            put(OpenVaultDbHelper.COL_ITEM_IS_FAVORITE, if (item.isFavorite) 1 else 0)
            put(OpenVaultDbHelper.COL_ITEM_CREATED_AT, item.createdAt)
            put(OpenVaultDbHelper.COL_ITEM_UPDATED_AT, System.currentTimeMillis())
            put(OpenVaultDbHelper.COL_ITEM_IS_DELETED, 0)
        }

        db.insertWithOnConflict(
            OpenVaultDbHelper.TABLE_ITEMS,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        refreshItems()
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(OpenVaultDbHelper.TABLE_ITEMS, "${OpenVaultDbHelper.COL_ITEM_ID} = ?", arrayOf(id))
        refreshItems()
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        val current = _itemsFlow.value.find { it.id == id } ?: return@withContext
        val updated = current.copy(isFavorite = !current.isFavorite)
        saveItem(updated)
    }

    suspend fun loadCategories() = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            OpenVaultDbHelper.TABLE_CATEGORIES,
            null,
            null,
            null,
            null,
            null,
            "${OpenVaultDbHelper.COL_CAT_SORT_ORDER} ASC"
        )
        val list = mutableListOf<Category>()
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_CAT_ID)
            val nameIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_CAT_NAME)
            val iconIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_CAT_ICON)
            val orderIdx = it.getColumnIndexOrThrow(OpenVaultDbHelper.COL_CAT_SORT_ORDER)

            while (it.moveToNext()) {
                list.add(
                    Category(
                        id = it.getString(idIdx),
                        name = it.getString(nameIdx),
                        iconName = it.getString(iconIdx),
                        sortOrder = it.getInt(orderIdx)
                    )
                )
            }
        }
        if (list.isNotEmpty()) {
            _categoriesFlow.value = list
        }
    }

    fun auditVault(): VaultAuditReport {
        val currentItems = _itemsFlow.value
        val weakItems = mutableListOf<VaultItem>()
        val passwordCounts = mutableMapOf<String, Int>()
        val missing2fa = mutableListOf<VaultItem>()

        for (item in currentItems) {
            if (item.type == ItemType.LOGIN && item.password.isNotBlank()) {
                val strength = PasswordGenerator.calculateStrength(item.password)
                if (strength.rating == StrengthRating.VERY_WEAK || strength.rating == StrengthRating.WEAK) {
                    weakItems.add(item)
                }

                passwordCounts[item.password] = (passwordCounts[item.password] ?: 0) + 1

                if (item.totpSecret.isBlank()) {
                    missing2fa.add(item)
                }
            }
        }

        val reusedPasswords = passwordCounts.filter { it.value > 1 }
        val reusedItems = currentItems.filter {
            it.type == ItemType.LOGIN && reusedPasswords.containsKey(it.password)
        }

        return VaultAuditReport(
            totalItems = currentItems.size,
            weakItems = weakItems,
            reusedPasswordsCount = reusedPasswords,
            reusedItems = reusedItems,
            missing2faLogins = missing2fa
        )
    }

    private fun setMetadata(db: android.database.sqlite.SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues().apply {
            put(OpenVaultDbHelper.COL_META_KEY, key)
            put(OpenVaultDbHelper.COL_META_VALUE, value)
        }
        db.insertWithOnConflict(
            OpenVaultDbHelper.TABLE_METADATA,
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun getMetadata(db: android.database.sqlite.SQLiteDatabase, key: String): String? {
        val cursor = db.rawQuery(
            "SELECT ${OpenVaultDbHelper.COL_META_VALUE} FROM ${OpenVaultDbHelper.TABLE_METADATA} WHERE ${OpenVaultDbHelper.COL_META_KEY} = ?",
            arrayOf(key)
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun hexToByteArray(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }
}
