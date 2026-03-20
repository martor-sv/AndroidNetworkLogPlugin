package com.netlog.networklog.interceptor

import com.netlog.networklog.model.NetworkRecord
import com.netlog.networklog.repository.NetworkLogRepository
import okhttp3.*
import com.netlog.networklog.util.NetworkLogCompat
import okio.Buffer
import okio.GzipSource
import okio.buffer
import java.io.EOFException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@Suppress("DEPRECATION_ERROR", "DEPRECATION")
class NetworkLogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 防御性读取请求信息
        var recordId: Long? = null
        try {
            val requestBodyString = (NetworkLogCompat.requestBody(request) as? RequestBody)?.let { body ->
                val buffer = Buffer()
                try {
                    body.writeTo(buffer)
                    buffer.readString(charset(request) ?: StandardCharsets.UTF_8)
                } catch (t: Throwable) {
                    "[读取请求体失败: ${t.message}]"
                }
            }

            val requestHeaders = mutableMapOf<String, String>()
            val requestHeadersObj = NetworkLogCompat.requestHeaders(request)
            if (requestHeadersObj != null) {
                for (i in 0 until NetworkLogCompat.headersSize(requestHeadersObj)) {
                    requestHeaders[requestHeadersObj.name(i)] = requestHeadersObj.value(i)
                }
            }

            val record = NetworkRecord(
                method = NetworkLogCompat.requestMethod(request) ?: "UNKNOWN",
                url = NetworkLogCompat.requestUrl(request)?.toString() ?: "UNKNOWN",
                requestHeaders = requestHeaders,
                requestBody = requestBodyString,
                requestBodySize = (NetworkLogCompat.requestBody(request) as? RequestBody)?.contentLength() ?: 0L
            )
            recordId = record.id
            NetworkLogRepository.addRecord(record)
        } catch (t: Throwable) {
            // 日志记录失败，不应影响正常请求
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Throwable) {
            // 网络层异常，记录并抛出
            recordId?.let { id ->
                NetworkLogRepository.updateRecord(id) { rec ->
                    rec.error = e.message ?: e.javaClass.simpleName
                    rec.endTime = System.currentTimeMillis()
                }
            }
            throw e
        }

        try {
            val endTime = System.currentTimeMillis()
            val responseBodyString = if (shouldHasBody(response)) {
                readResponseBody(response)
            } else null

            val responseHeaders = mutableMapOf<String, String>()
            val respHeadersObj = NetworkLogCompat.responseHeaders(response)
            if (respHeadersObj != null) {
                for (i in 0 until NetworkLogCompat.headersSize(respHeadersObj)) {
                    responseHeaders[respHeadersObj.name(i)] = respHeadersObj.value(i)
                }
            }

            recordId?.let { id ->
                NetworkLogRepository.updateRecord(id) { rec ->
                    rec.statusCode = NetworkLogCompat.responseCode(response)
                    rec.responseHeaders = responseHeaders
                    rec.responseBody = responseBodyString
                    rec.responseBodySize = NetworkLogCompat.responseBody(response)?.contentLength() ?: 0L
                    rec.endTime = endTime
                }
            }

            val newBody = NetworkLogCompat.createResponseBody(
                NetworkLogCompat.responseBody(response)?.contentType(),
                responseBodyString ?: ""
            )
            return if (newBody != null) {
                response.newBuilder().body(newBody).build()
            } else {
                response
            }
        } catch (t: Throwable) {
            // 响应解析失败，直接返回原始 response
            return response
        }
    }

    private fun charset(request: okhttp3.Request): Charset? {
        return (NetworkLogCompat.requestBody(request) as? RequestBody)?.contentType()?.charset(StandardCharsets.UTF_8)
    }

    private fun readResponseBody(response: Response): String? {
        return try {
            val responseBody = NetworkLogCompat.responseBody(response) ?: return null
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            var buffer = NetworkLogCompat.getBuffer(source)

            // 处理 gzip
            val contentEncoding = NetworkLogCompat.header(response, "Content-Encoding")
            if ("gzip".equals(contentEncoding, ignoreCase = true)) {
                val decompressedBuffer = Buffer()
                try {
                    GzipSource(buffer.clone()).use { gzippedSource ->
                        gzippedSource.buffer().use { bufferedGzip ->
                            bufferedGzip.request(Long.MAX_VALUE)
                            decompressedBuffer.writeAll(bufferedGzip)
                        }
                    }
                    buffer = decompressedBuffer
                } catch (t: Throwable) {
                    // gzip 解压失败
                }
            }

            if (NetworkLogCompat.bufferSize(NetworkLogCompat.getBuffer(source)) == 0L) return ""

            val contentType: MediaType? = responseBody.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            if (!isPlaintext(buffer)) return "[二进制数据]"

            try {
                buffer.clone().readString(charset)
            } catch (t: Throwable) {
                "[读取失败: ${t.message}]"
            }
        } catch (t: Throwable) {
            "[响应解析崩溃: ${t.javaClass.simpleName}]"
        }
    }

    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = minOf(NetworkLogCompat.bufferSize(buffer), 64)
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (t: Throwable) {
            false
        }
    }

    private fun shouldHasBody(response: Response): Boolean {
        return try {
            val request = NetworkLogCompat.request(response)
            if (NetworkLogCompat.requestMethod(request) == "HEAD") return false
            val code = NetworkLogCompat.responseCode(response)
            !((code < 200 || code == 204 || code == 304) && NetworkLogCompat.header(response, "Content-Length") == null &&
                !"chunked".equals(NetworkLogCompat.header(response, "Transfer-Encoding"), ignoreCase = true))
        } catch (t: Throwable) {
            false
        }
    }
}
