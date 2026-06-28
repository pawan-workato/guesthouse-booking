package com.guesthouse.booking.data.remote
import com.guesthouse.booking.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
class ApiClient(tokenStorage: TokenStorage) {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenStorage.getToken()
            val req = if (token.isNullOrBlank()) chain.request() else chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE })
        .build()
    val api: GuesthouseApi = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build())).build().create(GuesthouseApi::class.java)
}
