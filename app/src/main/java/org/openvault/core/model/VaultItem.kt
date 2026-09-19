package org.openvault.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ItemType {
    LOGIN,
    SECURE_NOTE,
    CARD,
    IDENTITY
}

@Serializable
enum class FieldType {
    TEXT,
    CONCEALED
}

@Serializable
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String,
    val type: FieldType = FieldType.TEXT
)

@Serializable
data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ItemType = ItemType.LOGIN,
    val categoryId: String = "logins",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val totpSecret: String = "",
    val customFields: List<CustomField> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val hasTotp: Boolean
        get() = totpSecret.isNotBlank()

    val displaySubtitle: String
        get() = when {
            username.isNotBlank() -> username
            url.isNotBlank() -> url
            notes.isNotBlank() -> notes.take(40).replace("\n", " ")
            else -> type.name.lowercase().replaceFirstChar { it.uppercase() }
        }
}
