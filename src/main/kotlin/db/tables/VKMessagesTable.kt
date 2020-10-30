package db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object VKMessagesTable : LongIdTable() {
    val nextInd = integer("nextInd").default(1)
}