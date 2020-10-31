package servlets

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import db.entities.Order
import db.tables.OrderTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import vk.VkTokenChecker
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CustomerOrderListServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val token = req.getParameter("token")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)?.toLongOrNull()
        if (userId == null) {
            resp.sendError(401)
            return
        }
        val userActor = UserActor(userId.toInt(), token)
        val ordersJSON = JSONArray()
        val apiClient = VkApiClient(HttpTransportClient.getInstance())
        val groupIdSet = TreeSet<Long>()
        transaction {
            Order.find {
                OrderTable.clientId eq userId
            }.orderBy(OrderTable.id to SortOrder.DESC).forEach {
                ordersJSON.put(it.toJSON())
                groupIdSet.add(it.groupId)
            }
        }
        val resultJSON = JSONObject()
        val groupsJSON = if (groupIdSet.isNotEmpty()) {
            JSONObject(apiClient.groups().getById(userActor)
                    .groupIds(groupIdSet.map { it.toString() }.toList())
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