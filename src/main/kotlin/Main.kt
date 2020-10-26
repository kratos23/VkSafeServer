import network.VkApi
import org.eclipse.jetty.servlet.ServletContextHandler
import vk.VK
import vk.VkTokenChecker

fun main() {
    val vkTokenChecker = VkTokenChecker()
    vkTokenChecker.check("12")
    /*val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS or ServletContextHandler.GZIP
            or ServletContextHandler.SECURITY)*/
}