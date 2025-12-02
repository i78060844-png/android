package com.android.swingmusic.network.data.cache

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor that adds cache control headers when network is available
 */
class NetworkCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        val cacheControl = CacheControl.Builder()
            .maxAge(30, TimeUnit.SECONDS) // Cache for 30 seconds when online
            .build()
        
        return response.newBuilder()
            .removeHeader("Pragma")
            .removeHeader("Cache-Control")
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}

/**
 * Interceptor that serves cached responses when offline
 */
class OfflineCacheInterceptor(
    private val context: Context
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        if (!isNetworkAvailable()) {
            val cacheControl = CacheControl.Builder()
                .maxStale(7, TimeUnit.DAYS) // Use stale cache up to 7 days when offline
                .onlyIfCached()
                .build()
            
            request = request.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .cacheControl(cacheControl)
                .build()
        }
        
        return chain.proceed(request)
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Helper to check network connectivity
 */
object NetworkHelper {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    fun isNetworkUnstable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return true
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return true
        
        // Consider network unstable if it's metered or has low bandwidth
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).not() ||
               !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
