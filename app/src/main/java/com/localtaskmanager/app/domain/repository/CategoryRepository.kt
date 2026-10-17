package com.localtaskmanager.app.domain.repository

import com.localtaskmanager.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategoriesFlow(): Flow<List<Category>>
    suspend fun getAllCategories(): List<Category>
    suspend fun getCategoryById(id: String): Category?
    suspend fun getCategoryByName(name: String): Category?
    suspend fun getDefaultCategory(): Category?
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String)
}
