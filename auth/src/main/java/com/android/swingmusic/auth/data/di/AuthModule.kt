package com.android.swingmusic.auth.data.di


import android.content.Context
import com.android.swingmusic.auth.data.api.service.AuthApiService
import com.android.swingmusic.auth.data.datastore.AuthTokensDataStore
import com.android.swingmusic.database.data.dao.BaseUrlDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun providesAuthTokenDataStore(
        @ApplicationContext context: Context
    ): AuthTokensDataStore {
        return AuthTokensDataStore(context = context.applicationContext)
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        
        // 10MB cache
        val cacheSize = 10L * 1024 * 1024
        val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)
        
        // Network interceptor - adds cache headers when online
        val networkCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder()
                .maxAge(30, TimeUnit.SECONDS)
                .build()
            response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
        
        // Offline interceptor - serves stale cache when offline
        val offlineCacheInterceptor = Interceptor { chain ->
            var request = chain.request()
            if (!isNetworkAvailable(context)) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .onlyIfCached()
                    .build()
                request = request.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .cacheControl(cacheControl)
                    .build()
            }
            chain.proceed(request)
        }
        
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(interceptor)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(networkCacheInterceptor)
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Provides
    @Singleton
    fun providesAuthApiService(okHttpClient: OkHttpClient, baseUrlDao: BaseUrlDao): AuthApiService {
        val baseUrl = runBlocking(Dispatchers.IO) {
            baseUrlDao.getBaseUrl()?.url ?: "http://default"
        }
        Timber.e(":auth -> BASE URL: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(AuthApiService::class.java)
    }
}
