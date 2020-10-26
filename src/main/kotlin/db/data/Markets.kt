package db.data

import db.entities.Market
import vk.VK

fun createStubMarkets() {
    fun stubMarket(id: Long, marketName: String) {
        createIfNotExists(id = id) {
            name = marketName
            adminId = VK.MY_ID
        }
    }
    stubMarket(77521, "Noize MC")
    stubMarket(29060604, "HUAWEI Mobile")
    stubMarket(2736916, "Anacondaz")
    stubMarket(48067211, "Nike")
    stubMarket(84734062, "HyperX")
    stubMarket(7030876, "Порнофильмы")
    stubMarket(24204002, "Проф Зал | Мир Украшений. Косметика, гель-лаки")
    stubMarket(44074973, "Rexona")
    stubMarket(59840447, "Много Мебели")
    stubMarket(217557, "ПБК ЦСКА")
    stubMarket(23279823, "Cristiano Ronaldo")
    stubMarket(154169220, "Планетарий 1")
    stubMarket(12834456, "Nekoshop")
    stubMarket(64941297, "Подбелка - центральная пельменная №1")
    stubMarket(116130258, "Часы | Барахолка часов №1")
    stubMarket(15413538, "Dota 2")
    stubMarket(34517334, "Свадебный / семейный фотограф СПб | Афанасьев А.")
    stubMarket(2611, "Samsung")
}

private fun createIfNotExists(id: Long, init: Market.() -> Unit) {
    if (Market.findById(id) == null) {
        Market.new(id, init)
    }
}