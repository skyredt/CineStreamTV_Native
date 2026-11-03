package com.cinestream.tv

import java.net.URLEncoder
import org.jsoup.nodes.Document

/** Positions follow the provider's published /vodshow/ filter links. */
internal fun dbkuFilterUrl(type: Int, filters: SearchFilters): String {
    require(type in listOf(1, 2, 4, 21))
    val fields = MutableList(12) { "" }
    fields[0] = type.toString()
    fields[1] = filters.country.takeUnless { it == "All" }?.let { URLEncoder.encode(it, "UTF-8") }.orEmpty()
    fields[3] = filters.genre.takeUnless { it == "All" }?.let { URLEncoder.encode(it, "UTF-8") }.orEmpty()
    return "${ProvidersRepository.DBKU}/vodshow/${fields.joinToString("-")}.html"
}
internal fun parseDbkuBrowse(doc: Document, type: Int, filters: SearchFilters): List<DesignTitle> {
    require(doc.selectFirst(".myui-screen__list") != null) { "DBKU filtered catalog markup was not recognized." }
    val selected = doc.select(".myui-screen__list a.btn-warm").eachText()
    require(filters.genre == "All" || filters.genre in selected) { "DBKU did not confirm the selected genre filter." }
    require(filters.country == "All" || filters.country in selected) { "DBKU did not confirm the selected country filter." }
    return doc.select(".myui-panel a.myui-vodlist__thumb[href*=/voddetail/]").map { a ->
        val card = a.closest("li") ?: a
        liveTitle("dbku", a.absUrl("href"), a.attr("title"), a.absUrl("data-original"),
            score = Regex("\\d+(?:\\.\\d+)?").find(card.select(".pic-tag-top").text())?.value.orEmpty(), tv = type != 1,
            genres = listOfNotNull(filters.genre.takeUnless { it == "All" }))
            .copy(countries = listOfNotNull(filters.country.takeUnless { it == "All" }), isShort = type == 21)
    }.distinctBy { it.id }
}
