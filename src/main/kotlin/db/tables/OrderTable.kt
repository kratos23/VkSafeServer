package db.tables

import db.entities.Order
import org.jetbrains.exposed.dao.id.LongIdTable

object OrderTable : LongIdTable() {
    val groupId = long("groupId")
    val status = enumeration("status", Order.Status::class)
    val payedAt = long("payedAt").default(-1L)
    val clientId = long("clientId")
    val cartJSON = text("cartJSON")
    val address = text("address").default("")
    val comment = text("comment").default("")
    val productsJSON = text("productsJSON")
    val paymentFormSent = bool("paymentFormSent").default(false)
    val paymentConfirmationSent = bool("paymentConfirmSent").default(false)
}