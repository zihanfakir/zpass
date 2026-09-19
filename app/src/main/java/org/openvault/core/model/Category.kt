package org.openvault.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val sortOrder: Int = 0
) {
    companion object {
        val ALL = Category("all", "All Items", "List", 0)
        val FAVORITES = Category("favorites", "Favorites", "Star", 1)
        val LOGINS = Category("logins", "Logins", "Key", 2)
        val SECURE_NOTES = Category("notes", "Secure Notes", "Description", 3)
        val CARDS = Category("cards", "Cards", "CreditCard", 4)
        val IDENTITIES = Category("identities", "Identities", "Badge", 5)

        val DEFAULT_CATEGORIES = listOf(LOGINS, SECURE_NOTES, CARDS, IDENTITIES)
        val FILTER_TABS = listOf(ALL, FAVORITES, LOGINS, SECURE_NOTES, CARDS, IDENTITIES)
    }
}
