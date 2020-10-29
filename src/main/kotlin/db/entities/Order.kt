package db.entities

import db.tables.OrderTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

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

    enum class Status {
        CREATED,
        PAID,
        CONFIRMED,
        DISPUTE,
        CLOSED,
        CANCELED
    }
}