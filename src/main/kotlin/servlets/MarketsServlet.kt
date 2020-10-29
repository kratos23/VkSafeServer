package servlets

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.UserActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.queries.groups.GroupField
import db.tables.MarketTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upperCase
import vk.VkTokenChecker
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class MarketsServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val token = req.getParameter("token")
        val tokenChecker = VkTokenChecker()
        val userId = tokenChecker.check(token)
        if (userId == null) {
            resp.sendError(401)
        } else {
            resp.status = 200
            val q = req.getParameter("q") ?: ""
            val page = req.parameterMap.getOrDefault("page", arrayOf("0"))[0].toIntOrNull() ?: 0
            val offset = page.toLong() * PAGE_SIZE.toLong()

            val marketsIds = transaction {
                MarketTable
                        .select {
                            MarketTable.name
                                    .upperCase()
                                    .like("%${q.toUpperCase()}%")
                        }
                        .orderBy(MarketTable.id)
                        .limit(PAGE_SIZE, offset)
                        .map { it[MarketTable.id].value.toString() }
            }
            if (marketsIds.isEmpty()) {
                resp.writer.println("""{"response":[]} """)
            } else {
                val userActor = UserActor(userId.toInt(), token)
                val transportClient = HttpTransportClient.getInstance()
                val vk = VkApiClient(transportClient)
                val response = vk.groups().getById(userActor)
                        .groupIds(marketsIds)
                        .unsafeParam("language", 0)
                        .fields(listOf(GroupField.ACTIVITY, GroupField.MARKET))
                        .executeAsString()
                resp.contentType = "application/json; charset=UTF-8"
                resp.writer.println(response)
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 25
    }
}