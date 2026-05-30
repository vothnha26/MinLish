package com.edu.minlish.features.profilesetup.domain.repository

import com.edu.minlish.features.profilesetup.domain.model.UserLearningStats
import com.edu.minlish.features.profilesetup.domain.model.UserProfile

interface ProfileRepository {
    suspend fun completeProfileSetup(
        profile: UserProfile,
        stats: UserLearningStats
    ): Result<Unit>
    
    suspend fun getProfile(userId: String): Result<UserProfile?>
}
