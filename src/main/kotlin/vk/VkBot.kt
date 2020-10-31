package vk

import com.google.gson.JsonObject
import com.vk.api.sdk.callback.longpoll.CallbackApiLongPoll
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.GroupActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.messages.Message
import db.entities.Market
import db.entities.Order
import db.entities.VkMessages
import db.tables.OrderTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class VkBot : CallbackApiLongPoll(VkApiClient(HttpTransportClient.getInstance()),
        GroupActor(VK.BOT_GROUP_ID, VK.BOT_API_KEY)) {

    private val apiClient = VkApiClient(HttpTransportClient.getInstance())
    private val groupActor = GroupActor(VK.BOT_GROUP_ID, VK.BOT_API_KEY)

    private fun getNextRandomId(msgUserId: Int): Int {
        return transaction {
            val user = VkMessages.findById(msgUserId.toLong())
            if (user == null) {
                VkMessages.new(msgUserId.toLong()) {}
            }

            val user2 = VkMessages.findById(msgUserId.toLong())!!
            ++user2.nextId
        }
    }

    private fun sendMessageToUser(msg: String, userId: Int) {
        kotlin.runCatching {
            apiClient.messages()
                    .send(groupActor)
                    .message(msg)
                    .randomId(getNextRandomId(userId))
                    .userId(userId)
                    .execute()
        }
    }

    fun sendMessageToAdmin(msg: String) = sendMessageToUser(msg, VK.MY_ID.toInt())

    private fun sendOrderPaymentForm(orderId: Long) {
        try {
            val order = transaction {
                Order.findById(orderId)
            } ?: return
            val group = transaction {
                Market.findById(order.groupId)
            }

            val totalPrice = order.getTotalPrice()
            val priceS = String.format(Locale.getDefault(), "%.2f", totalPrice / 100.0)

            val keyboard = JSONObject()
            keyboard.put("inline", true)
            val paymentDescription = java.net.URLEncoder.encode("вСейфе - оплата заказа#$orderId", "utf-8")
            val vkPayButton = JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "vkpay")
                    put("payload", "[]")
                    put("hash", """action=pay-to-group&group_id=${VK.BOT_GROUP_ID}&amount=$priceS&aid=${VK.CLIENT_ID}&description=$paymentDescription""")
                })
            }
            val row1 = JSONArray().apply {
                put(vkPayButton)
            }
            val orderConfirmationPayload = JSONObject().apply {
                put("orderId", order.id)
                put("type", PAYMENT_CONFIRMATION_PAYLOAD_TYPE)
            }
            val payConfirmation = JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "text")
                    put("label", "Я оплатил")
                    put("payload", orderConfirmationPayload.toString())
                })
                put("color", "positive")
            }
            val row2 = JSONArray().apply {
                put(payConfirmation)
            }
            keyboard.put("buttons", JSONArray().apply {
                put(row1)
                put(row2)
            })

            val msg = """Оплата заказа#${order.id} в магазине ${group?.name} на сумму $priceS"""

            apiClient.messages()
                    .send(groupActor)
                    .message(msg)
                    .unsafeParam("keyboard", keyboard.toString())
                    .randomId(getNextRandomId(order.clientId.toInt()))
                    .userId(order.clientId.toInt())
                    .execute()
            transaction {
                order.paymentFormSent = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkForPendingOrders(userId: Long) {
        val newOrderIds = transaction {
            OrderTable.select {
                OrderTable.clientId eq userId and (OrderTable.paymentFormSent eq false) and (OrderTable.status eq Order.Status.CREATED)
            }.map { order ->
                order[OrderTable.id].value
            }
        }
        newOrderIds.forEach { orderId ->
            sendOrderPaymentForm(orderId)
        }
    }

    fun newOrderReceived(orderId: Long) {
        sendOrderPaymentForm(orderId)
    }

    private fun onPaymentConfirm(orderId: Long) {
        val order = transaction {
            Order.findById(orderId)
        } ?: return
        if (order.paymentConfirmSent) {
            return
        }
        val totalPrice = order.getTotalPrice()
        val priceS = String.format(Locale.getDefault(), "%.2f", totalPrice / 100.0)
        sendMessageToAdmin("Проверьте оплату заказа#$orderId на сумму $priceS от пользователя ${order.clientId}\n" +
                "/confirm $orderId для подтверждения\n"
                + "/reject $orderId для отклонения\n")
        sendMessageToUser("Сейчас мы проверим получение оплаты заказа#$orderId.\n" +
                "Пожалуйста подождите.", order.clientId.toInt())

        transaction {
            order.paymentConfirmSent = true
        }
    }

    private fun confirmOrderPayment(orderId: Long) {
        val order = transaction {
            Order.findById(orderId)
        } ?: return
        transaction {
            order.status = Order.Status.PAID
            order.payedAt = System.currentTimeMillis()
        }
        sendMessageToUser("Заказ#$orderId успешно оплачен." +
                " Следить за статусом заказа вы можете в нашем мобильном приложении.", order.clientId.toInt())
        sendMessageToAdmin("Оплата заказа#$orderId подтверждена")
    }

    private fun rejectOrderPayment(orderId: Long) {
        val order = transaction {
            Order.findById(orderId)
        } ?: return
        transaction {
            order.paymentConfirmSent = false
        }
        sendMessageToUser("Мы не смогли найти оплату заказа#${order.id}. Пожалуйста, оплатите заказ.", order.clientId.toInt())
        sendOrderPaymentForm(orderId)
        sendMessageToAdmin("Оплата заказа#$orderId отклонена")
    }

    private fun onMessageFromAdmin(message: Message) {
        val text = message.text
        val id = text.split(" ").getOrNull(1)?.toLongOrNull()
        if (text.startsWith("/confirm")) {
            id?.let { confirmOrderPayment(it) }
        } else if (text.startsWith("/reject")) {
            id?.let { rejectOrderPayment(it) }
        }
    }

    override fun messageNew(groupId: Int, message: Message) {
        val userId = message.fromId
        checkForPendingOrders(userId.toLong())
        if (message.fromId.toLong() == VK.MY_ID) {
            onMessageFromAdmin(message)
        }
        message.payload?.let {
            val payloadJSON = JSONObject(it)
            when (payloadJSON.getString("type")) {
                PAYMENT_CONFIRMATION_PAYLOAD_TYPE -> {
                    val orderId = payloadJSON.getLong("orderId")
                    onPaymentConfirm(orderId)
                }
            }
        }
    }

    override fun parse(json: JsonObject?): Boolean {
        println(json)
        return kotlin.runCatching { super.parse(json) }.getOrDefault(false)
    }

    companion object {
        const val PAYMENT_CONFIRMATION_PAYLOAD_TYPE = "payment_confirm"
    }
}