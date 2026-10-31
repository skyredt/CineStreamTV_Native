package com.cinestream.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DesignCast(val name: String, val role: String, val image: String)
data class DesignEpisode(val id: String, val number: Int, val season: Int, val title: String, val overview: String, val image: String, val minutes: Int, val date: String, val pageUrl: String = "")
data class DesignSeason(val number: Int, val name: String, val episodes: List<DesignEpisode>)
data class DesignTitle(val id: String, val title: String, val tagline: String, val overview: String, val poster: String, val backdrop: String, val year: Int, val rating: String, val quality: String, val genres: List<String>, val isTv: Boolean, val featured: Boolean, val minutes: Int, val cast: List<DesignCast>, val seasons: List<DesignSeason>, val provider: String = "", val pageUrl: String = "", val playbackPage: String = "")
data class DesignRow(val id: String, val title: String, val tag: String, val titles: List<DesignTitle>)
data class DesignCatalog(val titles: List<DesignTitle>, val rows: List<DesignRow>) {
    companion object {
        fun load(context: Context): DesignCatalog {
            return parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
        }
        fun parse(json: String): DesignCatalog {
            val root = JSONObject(json)
            val titles = root.getJSONArray("titles").objects().map { t ->
                DesignTitle(t.getString("id"), t.getString("title"), t.optString("tagline"), t.getString("overview"), t.getString("posterUrl"), t.getString("backdropUrl"), t.getInt("releaseYear"), t.get("voteAverage").toString(), t.getString("qualityBadge"), t.getJSONArray("genres").strings(), t.getString("mediaType") == "tv", t.optBoolean("isFeatured"), t.optInt("durationMinutes"),
                    t.optJSONArray("cast").objects().map { DesignCast(it.getString("name"), it.getString("character"), it.getString("profileUrl")) },
                    t.optJSONArray("seasons").objects().map { s -> DesignSeason(s.getInt("seasonNumber"), s.getString("name"), s.getJSONArray("episodes").objects().map { e -> DesignEpisode("${t.getString("id")}-${s.getInt("seasonNumber")}-${e.getInt("episodeNumber")}", e.getInt("episodeNumber"), e.getInt("seasonNumber"), e.getString("title"), e.getString("overview"), e.getString("stillUrl"), e.getInt("durationMinutes"), e.getString("airDate")) }) })
            }
            val rows = root.getJSONArray("rows").objects().map { r -> DesignRow(r.getString("id"), r.getString("title"), r.optString("categoryTag"), r.getJSONArray("ids").strings().mapNotNull { id -> titles.find { it.id == id } }) }
            return DesignCatalog(titles, rows)
        }
    }
}
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
