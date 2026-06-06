package com.edu.minlish.features.library.domain.repository

import com.edu.minlish.features.library.domain.model.Category

interface CategoryRepository {
    suspend fun getCategories(userId: String): Result<List<Category>>
    suspend fun addCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
}
