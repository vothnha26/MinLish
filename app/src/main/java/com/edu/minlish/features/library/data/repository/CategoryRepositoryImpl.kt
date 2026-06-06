package com.edu.minlish.features.library.data.repository

import com.edu.minlish.features.library.domain.model.Category
import com.edu.minlish.features.library.domain.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class CategoryRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CategoryRepository {

    override suspend fun getCategories(userId: String): Result<List<Category>> {
        return try {
            val snapshot = withTimeout(10000) {
                firestore.collection("categories")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()
            }
            val categories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCategory(category: Category): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .add(category)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .document(category.id)
                    .set(category)
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            withTimeout(10000) {
                firestore.collection("categories")
                    .document(categoryId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
