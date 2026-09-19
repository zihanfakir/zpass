package org.openvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openvault.core.model.Category
import org.openvault.core.model.VaultItem
import org.openvault.core.security.VaultSession
import org.openvault.data.repository.VaultAuditReport
import org.openvault.data.repository.VaultRepository

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    val allItems: StateFlow<List<VaultItem>> = repository.itemsFlow
    val categories: StateFlow<List<Category>> = repository.categoriesFlow

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        allItems,
        _searchQuery,
        _selectedCategoryId
    ) { items, query, categoryId ->
        items.filter { item ->
            val matchesCategory = when (categoryId) {
                "all" -> true
                "favorites" -> item.isFavorite
                else -> item.categoryId == categoryId
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        item.username.contains(query, ignoreCase = true) ||
                        item.url.contains(query, ignoreCase = true) ||
                        item.notes.contains(query, ignoreCase = true)
            }

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(itemId)
        }
    }

    fun saveItem(item: VaultItem) {
        viewModelScope.launch {
            repository.saveItem(item)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }

    fun lockVault() {
        VaultSession.lock()
    }

    fun getAuditReport(): VaultAuditReport {
        return repository.auditVault()
    }
}
