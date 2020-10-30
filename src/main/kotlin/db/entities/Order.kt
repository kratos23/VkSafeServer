package db.entities

import db.tables.OrderTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONArray
import org.json.JSONObject

class Order(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Order>(OrderTable)

    var groupId by OrderTable.groupId
    var status by OrderTable.status
    var payedAt by OrderTable.payedAt
    var clientId by OrderTable.clientId
    var cartJSON by OrderTable.cartJSON
    var address by OrderTable.address
    var comment by OrderTable.comment
    var productsJSON by OrderTable.productsJSON
    var paymentFormSent by OrderTable.paymentFormSent
    var paymentConfirmSent by OrderTable.paymentConfirmationSent

    enum class Status {
        CREATED,
        PAID,
        CONFIRMED,
        DISPUTE,
        CLOSED,
        CANCELED
    }

    fun getTotalPrice(): Long {
        val cart = JSONObject(cartJSON).getJSONObject("countMap")
        val products = JSONArray(productsJSON)
        return products.filterIsInstance(JSONObject::class.java).map { product ->
            val price = product.getJSONObject("price")
            val cnt = cart.getLong(product.getLong("id").toString())
            price.getString("amount").toLong() * cnt
        }.sum()
    }

    fun toJSON(): JSONObject {
        return transaction {
            JSONObject().apply {
                put("groupId", groupId)
                put("status", status)
                put("payedAt", payedAt)
                put("clientId", clientId)
                put("cartJSON", cartJSON)
                put("address", address)
                put("comment", comment)
                put("price", getTotalPrice())
            }
        }
    }
}