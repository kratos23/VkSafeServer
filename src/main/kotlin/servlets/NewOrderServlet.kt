package servlets

import db.entities.Order
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class NewOrderServlet : HttpServlet() {

    private fun getTotalPrice(productsJson: String, cartJSON: String): Long {
        val cart = JSONObject(cartJSON).getJSONObject("countMap")
        val products = JSONArray(productsJson)
        val totalPrice = products.filterIsInstance(JSONObject::class.java).map { product ->
            val price = product.getJSONObject("price")
            val cnt = cart.getLong(product.getLong("id").toString())
            price.getString("amount").toLong() * cnt
        }.sum()
        return totalPrice
    }

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

            // TODO SEND USER VK pay form in vk bot

            val orderId = transaction {
                Order.new {
                    status = Order.Status.CREATED
                    clientId = userId.toLong()
                    groupId = reqGroupId
                    cartJSON = reqCartJSON
                    address = reqAddress
                    comment = reqComment
                    productsJSON = reqProductsJSON
                }.id
            }
            val responseJSON = JSONObject()
            responseJSON.put("orderId", orderId)
            resp.status = 200
            resp.writer.println(responseJSON.toString())
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