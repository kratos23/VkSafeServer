package db.entities

import db.tables.MarketTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder

class Market(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Market>(MarketTable)

    var name by MarketTable.name
    var adminId by MarketTable.adminId
}

fun getUserMarket(userId: Long): Market? {
    return Market.find {
        MarketTable.adminId eq userId
    }.orderBy(MarketTable.id to SortOrder.DESC).firstOrNull()
}