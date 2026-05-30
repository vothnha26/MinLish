package com.edu.minlish.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val AUTH_BASE_URL = "http://10.0.2.2:8081/"
    private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val authRetrofit: Retrofit = createRetrofit(AUTH_BASE_URL)
    val dictionaryRetrofit: Retrofit = createRetrofit(DICTIONARY_BASE_URL)

    inline fun <reified T> createAuthService(): T {
        return authRetrofit.create(T::class.java)
    }

    inline fun <reified T> createDictionaryService(): T {
        return dictionaryRetrofit.create(T::class.java)
    }
}
