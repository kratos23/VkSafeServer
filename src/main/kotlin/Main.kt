import db.data.createStubMarkets
import db.tables.MarketTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    Database.connect("jdbc:sqlite:./db", driver = "org.sqlite.JDBC", user = "root", password = "")
    transaction {
        SchemaUtils.create(MarketTable)
        createStubMarkets()
    }
}
