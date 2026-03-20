package com.netlog.networklog.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON 格式化工具，将压缩的 JSON 字符串格式化为可读的缩进格式
 */
object JsonFormatter {

    fun format(text: String): String {
        val trimmed = text.trim()
        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> text
            }
        } catch (e: JSONException) {
            text // 非 JSON，原样返回
        }
    }
}
