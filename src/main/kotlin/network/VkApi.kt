package network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import vk.VK
import vk.VkServiceInterceptor


object VkApi {
    val client: OkHttpClient

    private fun getVkServiceToken(): String {
        val client = OkHttpClient()
                .newBuilder()
                .build()
        val req = Request.Builder()
                .url("https://oauth.vk.com/access_token?".toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("client_id", VK.CLIENT_ID)
                        .addQueryParameter("client_secret", VK.CLIENT_SECRET)
                        .addQueryParameter("grant_type", "client_credentials")
                        .addQueryParameter("v", VK.API_VERSION)
                        .build())
                .get()
                .build()
        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body!!.string())
        resp.close()
        return json.getString("access_token")
    }

    init {
        val vkToken = getVkServiceToken()
        client = OkHttpClient.Builder()
                .addNetworkInterceptor(VkServiceInterceptor(vkToken))
                .build()
    }
}