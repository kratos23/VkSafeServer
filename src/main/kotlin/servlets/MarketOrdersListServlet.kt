package servlets

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.users.Fields
import db.entities.Order
import db.entities.getUserMarket
import db.tables.OrderTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import vk.VkTokenChecker
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MarketOrdersListServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val token = req.getParameter("token")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)?.toLongOrNull()
        if (userId == null) {
            resp.sendError(401)
            return
        }
        val market = transaction { getUserMarket(userId)!! }
        val userActor = UserActor(userId.toInt(), token)
        val ordersJSON = JSONArray()
        val apiClient = VkApiClient(HttpTransportClient.getInstance())
        val clientIdSet = TreeSet<Long>()
        transaction {
            Order.find {
                (OrderTable.groupId eq market.id.value) and (OrderTable.status neq Order.Status.CREATED)
            }.orderBy(OrderTable.id to SortOrder.DESC).forEach {
                ordersJSON.put(it.toJSON())
                clientIdSet.add(it.clientId)
            }
        }
        val resultJSON = JSONObject()
        val groupsJSON = if (clientIdSet.isNotEmpty()) {
            JSONObject(apiClient.users().get(userActor)
                    .userIds(clientIdSet.map { it.toString() }.toList())
                    .fields(Fields.PHOTO_200)
                    .executeAsString())
        } else {
            JSONObject().apply {
                put("response", JSONArray())
            }
        }
        resultJSON.put("groups", groupsJSON)
        resultJSON.put("orders", ordersJSON)
        resp.status = 200
        resp.contentType = "application/json; charset=UTF-8"
        resp.writer.println(resultJSON.toString())
    }
}