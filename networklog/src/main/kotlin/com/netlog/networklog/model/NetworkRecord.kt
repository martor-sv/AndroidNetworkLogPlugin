package com.netlog.networklog.model

/**
 * 记录一次网络请求的完整信息
 */
data class NetworkRecord(
    val id: Long = System.nanoTime(),
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val requestBodySize: Long = 0L,
    val startTime: Long = System.currentTimeMillis(),
    // 以下字段在响应到达后填充
    var statusCode: Int = 0,
    var responseHeaders: Map<String, String> = emptyMap(),
    var responseBody: String? = null,
    var responseBodySize: Long = 0L,
    var endTime: Long = 0L,
    var error: String? = null
) {
    val duration: Long get() = if (endTime > 0) endTime - startTime else -1

    val statusText: String
        get() = when (statusCode) {
            0 -> if (error != null) "ERR" else "Pending"
            else -> statusCode.toString()
        }

    val isSuccess: Boolean get() = statusCode in 200..299
    val isError: Boolean get() = error != null || statusCode >= 400
}
