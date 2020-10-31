package servlets

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.groups.Fields
import com.vk.api.sdk.objects.groups.Filter
import db.entities.getUserMarket
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class UserMarketServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val token = req.getParameter("token")
        val forceList: String? = req.getParameter("forceList")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)?.toLongOrNull()
        if (userId == null) {
            resp.sendError(401)
            return
        }
        val market = transaction { getUserMarket(userId) }
        resp.status = 200
        val resultJSON = JSONObject()
        if (market != null && forceList == null) {
            resultJSON.put("marketId", market.id.value)
        } else {
            val userActor = UserActor(userId.toInt(), token)
            val transportClient = HttpTransportClient.getInstance()
            val vk = VkApiClient(transportClient)
            resultJSON.put("groups", vk.groups()
                    .get(userActor)
                    .filter(Filter.ADMIN)
                    .fields(Fields.MARKET, Fields.ACTIVITY)
                    .count(1000)
                    .extended(true)
                    .executeAsString())
        }
        resp.contentType = "application/json; charset=UTF-8"
        resp.writer.println(resultJSON.toString())
    }
}