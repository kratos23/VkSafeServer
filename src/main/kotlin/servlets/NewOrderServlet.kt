package servlets

import db.entities.Order
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import vk.VkBot
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class NewOrderServlet(val vkBot: VkBot) : HttpServlet() {

    private fun processNewOrder(req: HttpServletRequest, resp: HttpServletResponse) {
        val token = req.getParameter("token")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)
        if (userId == null) {
            resp.sendError(401)
        } else {

            val bodyJSON = JSONObject(req.reader.readText())
            val reqCartJSON = bodyJSON.getString("cart")
            val reqGroupId = bodyJSON.getLong("groupId")
            val reqComment = bodyJSON.getString("comment")
            val reqAddress = bodyJSON.getString("address")
            val reqProductsJSON = bodyJSON.getString("products")

            val orderId = transaction {
                Order.new {
                    status = Order.Status.CREATED
                    clientId = userId.toLong()
                    groupId = reqGroupId
                    cartJSON = reqCartJSON
                    address = reqAddress
                    comment = reqComment
                    productsJSON = reqProductsJSON
                }.id.value
            }
            val responseJSON = JSONObject()
            responseJSON.put("orderId", orderId)
            resp.status = 200
            resp.writer.println(responseJSON.toString())
            vkBot.newOrderReceived(orderId)
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val url = req.requestURL
        if (url.endsWith("/new")) {
            processNewOrder(req, resp)
        } else {
            resp.sendError(404, "NotFound")
        }
    }
}