package servlets

import db.entities.Market
import db.entities.Order
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import vk.VkBot
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class OrderButtonsServlet(private val bot: VkBot) : HttpServlet() {
    private fun cancelOrder(order: Order) {
        transaction {
            order.status = Order.Status.CANCELED
        }
        bot.sendMessageToAdmin("""Отмена заказа#${order.id} 
            |Вернуть ${order.getTotalPrice()} пользователю id${order.clientId}
        """.trimMargin())
    }

    private fun openDispute(order: Order) {
        transaction {
            order.status = Order.Status.DISPUTE
        }
        bot.sendMessageToAdmin("""Открыт спор по заказу#{$order.id}  между пользователем
| id${order.clientId} и магазином ${order.groupId}""".trimMargin())
    }

    private fun confirmOrder(order: Order) {
        transaction {
            order.status = Order.Status.CLOSED
        }
        val market = transaction {
            Market.findById(order.groupId)
        }

        bot.sendMessageToAdmin("""Подтверждение заказа#${order.id}  покупатемм
            |Перевести ${order.getTotalPrice()} пользователю id${market?.adminId}
        """.trimMargin())
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        super.doPost(req, resp)
        val token = req.getParameter("token")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)?.toLongOrNull()
        if (userId == null) {
            resp.sendError(401)
            return
        }
        val body = JSONObject(req.reader.readText())
        val fromState = body.getString("from")
        val orderId = body.getLong("orderId")
        val btn = body.getString("btn")
        val order = transaction {
            Order.findById(orderId)!!
        }
        if (order.status.toString() != fromState) {
            resp.status = 200
        } else {
            when (btn) {
                CUSTOMER_CANCEL_ORDER_BTN_ID -> {
                    if (userId == order.clientId && order.status == Order.Status.PAID) {
                        cancelOrder(order)
                        resp.status = 200
                    } else {
                        resp.sendError(403)
                    }
                }
                OPEN_DISPUTE_CUSTOMER_BTN_ID -> {
                    if (userId == order.clientId && order.status == Order.Status.CONFIRMED) {
                        openDispute(order)
                        resp.status = 200
                    } else {
                        resp.sendError(403)
                    }
                }
                CONFIRM_ORDER_CUSTOMER_BTN_ID -> {
                    if (userId == order.clientId && order.status == Order.Status.CONFIRMED) {
                        confirmOrder(order)
                        resp.status = 200
                    } else {
                        resp.sendError(403)
                    }
                }
            }
        }
    }

    companion object {
        const val CUSTOMER_CANCEL_ORDER_BTN_ID = "cancel_order_btn_customer"
        const val OPEN_DISPUTE_CUSTOMER_BTN_ID = "open_dispute_customer"
        const val CONFIRM_ORDER_CUSTOMER_BTN_ID = "confirm_order_customer"
    }
}