package org.openvault.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.openvault.core.model.Category

class OpenVaultDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "openvault.db"
        const val DATABASE_VERSION = 1

        // Table vault_items
        const val TABLE_ITEMS = "vault_items"
        const val COL_ITEM_ID = "id"
        const val COL_ITEM_TITLE = "title"
        const val COL_ITEM_CATEGORY_ID = "category_id"
        const val COL_ITEM_PAYLOAD = "encrypted_payload"
        const val COL_ITEM_IV = "iv"
        const val COL_ITEM_IS_FAVORITE = "is_favorite"
        const val COL_ITEM_CREATED_AT = "created_at"
        const val COL_ITEM_UPDATED_AT = "updated_at"
        const val COL_ITEM_IS_DELETED = "is_deleted"

        // Table categories
        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_ICON = "icon"
        const val COL_CAT_SORT_ORDER = "sort_order"

        // Table vault_metadata
        const val TABLE_METADATA = "vault_metadata"
        const val COL_META_KEY = "key"
        const val COL_META_VALUE = "value"

        // Metadata keys
        const val KEY_SALT = "salt_hex"
        const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key_hex"
        const val KEY_VAULT_KEY_IV = "vault_key_iv_hex"
        const val KEY_BIOMETRIC_WRAPPED_KEY = "biometric_wrapped_key_hex"
        const val KEY_BIOMETRIC_IV = "biometric_iv_hex"
        const val KEY_PIN_SALT = "pin_salt_hex"
        const val KEY_PIN_WRAPPED_KEY = "pin_wrapped_key_hex"
        const val KEY_PIN_IV = "pin_iv_hex"
        const val KEY_KDF_ITERATIONS = "kdf_iterations"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ITEM_ID TEXT PRIMARY KEY,
                $COL_ITEM_TITLE TEXT NOT NULL,
                $COL_ITEM_CATEGORY_ID TEXT NOT NULL,
                $COL_ITEM_PAYLOAD BLOB NOT NULL,
                $COL_ITEM_IV BLOB NOT NULL,
                $COL_ITEM_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COL_ITEM_CREATED_AT INTEGER NOT NULL,
                $COL_ITEM_UPDATED_AT INTEGER NOT NULL,
                $COL_ITEM_IS_DELETED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID TEXT PRIMARY KEY,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_ICON TEXT NOT NULL,
                $COL_CAT_SORT_ORDER INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_METADATA (
                $COL_META_KEY TEXT PRIMARY KEY,
                $COL_META_VALUE TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Seed default categories
        Category.DEFAULT_CATEGORIES.forEach { cat ->
            val cv = ContentValues().apply {
                put(COL_CAT_ID, cat.id)
                put(COL_CAT_NAME, cat.name)
                put(COL_CAT_ICON, cat.iconName)
                put(COL_CAT_SORT_ORDER, cat.sortOrder)
            }
            db.insert(TABLE_CATEGORIES, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handled through versioned migrations in future updates
    }
}
