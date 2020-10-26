import db.data.createStubMarkets
import db.tables.MarketTable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import servlets.MarketsServlet

fun main() {
    Database.connect("jdbc:sqlite:./db", driver = "org.sqlite.JDBC", user = "root", password = "")
    transaction {
        SchemaUtils.create(MarketTable)
        createStubMarkets()
    }

    val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS or ServletContextHandler.GZIP)
    context.addServlet(ServletHolder(MarketsServlet()), "/markets")
    val server = Server(8080)
    server.handler = context
    server.start()
}
