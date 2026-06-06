package com.edu.minlish.core.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"

    val dictionaryRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(DICTIONARY_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    inline fun <reified T> createDictionaryService(): T {
        return dictionaryRetrofit.create(T::class.java)
    }
}
