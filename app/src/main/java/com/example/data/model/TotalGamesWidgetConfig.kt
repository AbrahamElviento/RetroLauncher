package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class WidgetSubItem(
    val key: String,       // "games", "favorites", "completed"
    val label: String,     // display label
    val color: String,     // Hex color, e.g. "#3D5AFE"
    val icon: String,      // Icon name
    val enabled: Boolean,
    val order: Int
)

object TotalGamesWidgetConfig {
    fun parse(jsonStr: String, defaultColor: String = "#3D5AFE", defaultIcon: String = "games"): List<WidgetSubItem> {
        val defaultItems = listOf(
            WidgetSubItem("games", "Total Games", defaultColor, defaultIcon, true, 0),
            WidgetSubItem("favorites", "Favorites", "#FF4444", "heart", true, 1),
            WidgetSubItem("completed", "Completed", "#4CAF50", "star", true, 2)
        )
        if (jsonStr.isBlank() || !jsonStr.trim().startsWith("[")) {
            return defaultItems
        }
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<WidgetSubItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WidgetSubItem(
                        key = obj.getString("key"),
                        label = obj.getString("label"),
                        color = obj.getString("color"),
                        icon = obj.getString("icon"),
                        enabled = obj.getBoolean("enabled"),
                        order = obj.getInt("order")
                    )
                )
            }
            // Ensure if any subitems are missing from JSON, they get filled back in
            val existingKeys = list.map { it.key }.toSet()
            defaultItems.forEach { defaultItem ->
                if (defaultItem.key !in existingKeys) {
                    list.add(defaultItem.copy(order = list.size))
                }
            }
            list.sortedBy { it.order }
        } catch (e: Exception) {
            e.printStackTrace()
            defaultItems
        }
    }

    fun serialize(items: List<WidgetSubItem>): String {
        return try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("key", item.key)
                obj.put("label", item.label)
                obj.put("color", item.color)
                obj.put("icon", item.icon)
                obj.put("enabled", item.enabled)
                obj.put("order", item.order)
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
