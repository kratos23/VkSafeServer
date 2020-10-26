package db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object MarketTable : LongIdTable() {
    val name = varchar("name", 255)
    val adminId = long("userId")
}