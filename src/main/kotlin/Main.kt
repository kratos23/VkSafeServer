import db.data.createStubMarkets
import db.tables.MarketTable
import db.tables.OrderTable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import servlets.MarketsServlet
import servlets.NewOrderServlet
import vk.VK

fun main(args: Array<String>) {
    VK.BOT_API_KEY = args[0]
    VK.CLIENT_SECRET = args[1]
    Database.connect("jdbc:sqlite:./db", driver = "org.sqlite.JDBC", user = "root", password = "")
    transaction {
        SchemaUtils.create(MarketTable)
        SchemaUtils.create(OrderTable)
        createStubMarkets()
    }

    val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS or ServletContextHandler.GZIP)
    context.addServlet(ServletHolder(MarketsServlet()), "/markets")
    context.addServlet(ServletHolder(NewOrderServlet()), "/orders/new")
    val server = Server(8080)
    server.handler = context
    server.start()
}
