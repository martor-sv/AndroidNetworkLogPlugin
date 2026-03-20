package com.netlog.networklog.interceptor

import com.netlog.networklog.model.NetworkRecord
import com.netlog.networklog.repository.NetworkLogRepository
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import okio.buffer
import java.io.EOFException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * OkHttp 网络日志拦截器
 * 添加到 OkHttpClient 后，自动记录所有请求和响应
 */
class NetworkLogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 读取请求体
        val requestBodyString = request.body?.let { body ->
            val buffer = Buffer()
            try {
                body.writeTo(buffer)
                buffer.readString(charset(request) ?: StandardCharsets.UTF_8)
            } catch (e: Exception) {
                "[无法读取请求体: ${e.message}]"
            }
        }

        // 构建请求头 Map
        val requestHeaders = mutableMapOf<String, String>()
        request.headers.forEach { (name, value) -> requestHeaders[name] = value }

        val record = NetworkRecord(
            method = request.method,
            url = request.url.toString(),
            requestHeaders = requestHeaders,
            requestBody = requestBodyString,
            requestBodySize = request.body?.contentLength() ?: 0L
        )
        NetworkLogRepository.addRecord(record)

        return try {
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()

            // 读取响应体（不消耗原始流）
            val responseBodyString = if (response.promisesBody()) {
                readResponseBody(response)
            } else null

            // 构建响应头 Map
            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) -> responseHeaders[name] = value }

            NetworkLogRepository.updateRecord(record.id) { rec ->
                rec.statusCode = response.code
                rec.responseHeaders = responseHeaders
                rec.responseBody = responseBodyString
                rec.responseBodySize = response.body?.contentLength() ?: 0L
                rec.endTime = endTime
            }

            // 重建响应体，避免流被消耗
            val newBody = ResponseBody.create(
                response.body?.contentType(),
                responseBodyString ?: ""
            )
            response.newBuilder().body(newBody).build()
        } catch (e: Exception) {
            NetworkLogRepository.updateRecord(record.id) { rec ->
                rec.error = e.message ?: e.javaClass.simpleName
                rec.endTime = System.currentTimeMillis()
            }
            throw e
        }
    }

    private fun charset(request: okhttp3.Request): Charset? {
        return request.body?.contentType()?.charset(StandardCharsets.UTF_8)
    }

    private fun readResponseBody(response: Response): String? {
        val responseBody = response.body ?: return null
        val source = responseBody.source()
        source.request(Long.MAX_VALUE)
        var buffer = source.buffer

        // 处理 gzip
        val contentEncoding = response.header("Content-Encoding")
        if ("gzip".equals(contentEncoding, ignoreCase = true)) {
            val decompressedBuffer = Buffer()
            GzipSource(buffer.clone()).use { gzippedSource ->
                gzippedSource.buffer().use { bufferedGzip ->
                    bufferedGzip.request(Long.MAX_VALUE)
                    decompressedBuffer.writeAll(bufferedGzip)
                }
            }
            buffer = decompressedBuffer
        }

        if (buffer.size == 0L) return ""

        val contentType: MediaType? = responseBody.contentType()
        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

        if (!isPlaintext(buffer)) return "[二进制数据]"

        return try {
            buffer.clone().readString(charset)
        } catch (e: Exception) {
            "[读取失败: ${e.message}]"
        }
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = minOf(buffer.size, 64)
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (e: EOFException) {
            false
        }
    }
}
