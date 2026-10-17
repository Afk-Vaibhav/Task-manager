package com.localtaskmanager.app.data.repository

import com.localtaskmanager.app.data.local.dao.CategoryDao
import com.localtaskmanager.app.data.local.dao.TaskDao
import com.localtaskmanager.app.data.local.database.toDomain
import com.localtaskmanager.app.data.local.database.toEntity
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao
) : CategoryRepository {

    override fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategories().map { it.toDomain() }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name.trim())?.toDomain()
    }

    override suspend fun getDefaultCategory(): Category? {
        return categoryDao.getDefaultCategory()?.toDomain()
    }

    override suspend fun insertCategory(category: Category) {
        val trimmedName = category.name.trim()
        require(trimmedName.isNotEmpty()) { "Category name cannot be empty" }
        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null || existing.id == category.id) {
            "A category named '$trimmedName' already exists"
        }
        categoryDao.insertCategory(category.copy(name = trimmedName).toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        val trimmedName = category.name.trim()
        require(trimmedName.isNotEmpty()) { "Category name cannot be empty" }
        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null || existing.id == category.id) {
            "A category named '$trimmedName' already exists"
        }
        categoryDao.updateCategory(category.copy(name = trimmedName).toEntity())
    }

    override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String) {
        val category = categoryDao.getCategoryById(categoryId) ?: return
        require(!category.isDefault) { "Cannot delete default category" }
        // Reassign all tasks from this category to the target category
        taskDao.reassignCategory(fromCategoryId = categoryId, toCategoryId = reassignToCategoryId)
        categoryDao.deleteCategory(category)
    }
}
