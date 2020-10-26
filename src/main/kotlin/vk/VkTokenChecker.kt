package vk

import network.VkApi
import okhttp3.Request
import org.json.JSONObject

class VkTokenChecker {
    // if valid returns userId
    fun check(token:String) : String? {
        val client = VkApi.client
        val req = Request.Builder()
                .url("https://api.vk.com/method/secure.checkToken?token=$token")
                .get()
                .build()
        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body)
        resp.close()
        return json.optString("user_id", null)
    }
}