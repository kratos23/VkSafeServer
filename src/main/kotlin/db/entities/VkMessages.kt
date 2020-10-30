package db.entities

import db.tables.VKMessagesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class VkMessages(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<VkMessages>(VKMessagesTable)

    var userId by VKMessagesTable.id
    var nextId by VKMessagesTable.nextInd
}