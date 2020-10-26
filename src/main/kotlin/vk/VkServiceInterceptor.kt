package vk

import okhttp3.Interceptor
import okhttp3.Response

class VkServiceInterceptor(private val serviceToken : String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldReq = chain.request()
        val newUrl = oldReq.url
                .newBuilder()
                .addQueryParameter("access_token", serviceToken)
                .addQueryParameter("v", VK.API_VERSION)
                .addQueryParameter("client_secret", VK.CLIENT_SECRET)
                .build()
        val newReq = oldReq
                .newBuilder()
                .url(newUrl)
                .build()
        return chain.proceed(newReq)
    }
}