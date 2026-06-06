package com.edu.minlish.features.library.domain.usecase

import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.repository.CategoryRepository

class ManageCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend fun getCategories(userId: String) = categoryRepository.getCategories(userId)
    suspend fun addCategory(name: String, userId: String) = categoryRepository.addCategory(Category(name = name, creatorId = userId))
    suspend fun updateCategory(category: Category) = categoryRepository.updateCategory(category)
    suspend fun deleteCategory(categoryId: String) = categoryRepository.deleteCategory(categoryId)
}
