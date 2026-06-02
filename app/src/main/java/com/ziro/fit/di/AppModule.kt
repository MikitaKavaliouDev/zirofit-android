package com.ziro.fit.di

import android.util.Log
import com.ziro.fit.data.local.TokenManager
import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    //    private const val BASE_URL = "http://10.0.2.2:3321/"
       private const val BASE_URL = "http://127.0.0.1:3321/"
    //    http://127.0.0.1:3321/
//  private const val BASE_URL = "https://ziro.fit/"
    private const val TAG = "ZiroAPI"

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // 1. Attach current token
            tokenManager.getToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            var response = chain.proceed(requestBuilder.build())

            // 2. Handle 401 Unauthorized globally
            if (response.code == 401) {
                synchronized(this) {
                    val currentToken = tokenManager.getToken()
                    val requestToken = originalRequest.header("Authorization")?.removePrefix("Bearer ")

                    // 3. Check if another thread already refreshed the token
                    val refreshedToken = if (requestToken != currentToken) {
                        currentToken
                    } else {
                        // 4. Initiate refresh procedure
                        val refreshSuccess = runBlocking {
                            tokenManager.refreshToken()
                        }
                        if (refreshSuccess) tokenManager.getToken() else null
                    }

                    if (refreshedToken != null) {
                        // 5. Revalidate: Retry the request with the new token
                        response.close() // Close the failed response
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $refreshedToken")
                            .build()
                        response = chain.proceed(newRequest)
                    } else {
                        // 6. Refresh failed or no refresh token: Force Logout
                        runBlocking {
                            tokenManager.triggerLogout()
                        }
                    }
                }
            }
            
            response
        }
    }
    

private val rawLoggerInterceptor = Interceptor { chain ->
    val request = chain.request()
    
    // Log request
    Logger.d("ZiroAPI", "┌── ${request.method} ${request.url}")
    request.headers.forEach { (name, value) ->
        Logger.d("ZiroAPI", "│ $name: $value")
    }
    
    val response = chain.proceed(request)
    
    // Log response body (peek without consuming)
    val body = response.peekBody(1024 * 1024) // 1MB max
    Logger.d("ZiroAPI", "└── ${response.code} ${response.message}")
    Logger.d("ZiroAPI", "│ Body: ${body.string()}")
    
    response
}


    @Provides
    @Singleton
    fun provideZiroApi(authInterceptor: Interceptor): ZiroApi {
        // Custom logger for better formatting
        val logger = HttpLoggingInterceptor.Logger { message ->
            Log.d(TAG, message)
        }
        
        val loggingInterceptor = HttpLoggingInterceptor(logger).apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(rawLoggerInterceptor) 
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZiroApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): io.github.jan.supabase.SupabaseClient {
        // TODO: Move these to BuildConfig/Secrets
        return io.github.jan.supabase.createSupabaseClient(
                    //    supabaseUrl = "http://10.0.2.2:54321",
                        supabaseUrl = "http://127.0.0.1:54321",
           supabaseKey = "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
            // supabaseUrl = "https://pcrbmawiyzmqjvccozff.supabase.co",
            // supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBjcmJtYXdpeXptcWp2Y2NvemZmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDk2NDEwNzgsImV4cCI6MjA2NTIxNzA3OH0.etL_dgCTbZI765Gr9M3wGZDJjlCuhE2-CvPYZXKVU08"
        ) {

            install(io.github.jan.supabase.realtime.Realtime)
        }
    }
}
