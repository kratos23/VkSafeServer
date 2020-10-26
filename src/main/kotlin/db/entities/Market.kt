package db.entities

import db.tables.MarketTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Market(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Market>(MarketTable)

    var name by MarketTable.name
    var adminId by MarketTable.adminId
}