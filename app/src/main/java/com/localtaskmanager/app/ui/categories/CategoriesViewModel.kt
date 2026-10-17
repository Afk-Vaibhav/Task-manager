package com.localtaskmanager.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = categoryRepository.getAllCategoriesFlow()
        .let { flow ->
            MutableStateFlow(CategoriesUiState()).apply {
                viewModelScope.launch {
                    flow.collect { list ->
                        value = CategoriesUiState(categories = list, isLoading = false)
                    }
                }
            }
        }.asStateFlow()

    fun createCategory(name: String, colorArgb: Int, iconKey: String = "other") {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch
            try {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = trimmed,
                    iconKey = iconKey,
                    colorArgb = colorArgb,
                    isDefault = false
                )
                categoryRepository.insertCategory(newCategory)
            } catch (e: Exception) {
                // Category name duplicate or validation error
            }
        }
    }

    fun updateCategory(category: Category, newName: String, newColorArgb: Int) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return@launch
            try {
                categoryRepository.updateCategory(
                    category.copy(name = trimmed, colorArgb = newColorArgb)
                )
            } catch (e: Exception) {
                // Handle duplicate name
            }
        }
    }

    fun deleteCategory(categoryId: String, reassignToCategoryId: String) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(
                    categoryId = categoryId,
                    reassignToCategoryId = reassignToCategoryId
                )
            } catch (e: Exception) {
                // Cannot delete default category
            }
        }
    }
}
