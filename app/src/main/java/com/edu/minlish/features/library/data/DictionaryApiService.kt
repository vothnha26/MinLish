package com.edu.minlish.features.library.data

import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApiService {
    @GET("{word}")
    suspend fun getWordDefinition(@Path("word") word: String): List<DictionaryEntry>
}
