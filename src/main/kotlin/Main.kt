import db.data.createStubMarkets
import db.tables.MarketTable
import db.tables.OrderTable
import db.tables.VKMessagesTable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import servlets.*
import vk.VK
import vk.VkBot
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    VK.BOT_API_KEY = args[0]
    VK.CLIENT_SECRET = args[1]
    Database.connect("jdbc:sqlite:./db", driver = "org.sqlite.JDBC", user = "root", password = "")
    transaction {
        SchemaUtils.create(MarketTable)
        SchemaUtils.create(OrderTable)
        SchemaUtils.create(VKMessagesTable)
        createStubMarkets()
    }

    val vkBot = VkBot()
    thread {
        vkBot.run()
    }

    val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS or ServletContextHandler.GZIP)
    context.addServlet(ServletHolder(MarketsServlet()), "/markets")
    context.addServlet(ServletHolder(NewOrderServlet(vkBot)), "/orders/new")
    context.addServlet(ServletHolder(CustomerOrderListServlet()), "/customer/orders")
    context.addServlet(ServletHolder(OrderInfoServlet()), "/order")
    context.addServlet(ServletHolder(OrderButtonsServlet(vkBot)), "/order/status")
    context.addServlet(ServletHolder(UserMarketServlet()), "/user/stores")
    context.addServlet(ServletHolder(MarketOrdersListServlet()), "/store/orders")
    val server = Server(args.getOrElse(2) { "80" }.toIntOrNull() ?: 80)
    server.handler = context
    server.start()
}
