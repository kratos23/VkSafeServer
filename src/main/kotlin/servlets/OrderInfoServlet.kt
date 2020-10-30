package servlets

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import db.entities.Order
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class OrderInfoServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

        val token = req.getParameter("token")
        val orderId = req.getParameter("orderId").toLong()
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)?.toLongOrNull()
        if (userId == null) {
            resp.sendError(401)
            return
        }
        val userActor = UserActor(userId.toInt(), token)
        val apiClient = VkApiClient(HttpTransportClient.getInstance())


        val resultJSON = JSONObject()
        val order = transaction {
            Order.findById(orderId)
        }!!
        val orderJSON = order.toJSON()
        orderJSON.put("productsJSON", order.productsJSON)
        resultJSON.put("order", orderJSON)
        resultJSON.put("user", apiClient.users().get(userActor)
                .userIds(order.clientId.toString())
                .executeAsString()
        )
        resultJSON.put("group", apiClient.groups().getById(userActor)
                .groupId(order.groupId.toString()).executeAsString())
        resp.status = 200
        resp.contentType = "application/json; charset=UTF-8"
        resp.writer.println(resultJSON.toString())
    }
}