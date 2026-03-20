package com.netlog.networklog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.netlog.networklog.R
import com.netlog.networklog.model.NetworkRecord
import com.netlog.networklog.repository.NetworkLogRepository
import com.netlog.networklog.util.JsonFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详情 Tab Fragment
 * position: 0=概览 1=请求头 2=请求体 3=响应头 4=响应体
 */
class NetworkLogDetailFragment : Fragment() {

    companion object {
        private const val ARG_RECORD_ID = "arg_record_id"
        private const val ARG_POSITION = "arg_position"

        fun newInstance(recordId: Long, position: Int): NetworkLogDetailFragment {
            return NetworkLogDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_RECORD_ID, recordId)
                    putInt(ARG_POSITION, position)
                }
            }
        }

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_network_log_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recordId = arguments?.getLong(ARG_RECORD_ID) ?: return
        val position = arguments?.getInt(ARG_POSITION) ?: return
        val record = NetworkLogRepository.getRecord(recordId) ?: return

        val tvContent = view.findViewById<TextView>(R.id.tv_content)
        tvContent.text = when (position) {
            0 -> buildOverview(record)
            1 -> buildHeaders(record.requestHeaders)
            2 -> buildBody(record.requestBody)
            3 -> buildHeaders(record.responseHeaders)
            4 -> buildBody(record.responseBody)
            else -> ""
        }
    }

    private fun buildOverview(record: NetworkRecord): String {
        return buildString {
            appendLine("URL")
            appendLine(record.url)
            appendLine()
            appendLine("方法        ${record.method}")
            appendLine("状态码      ${record.statusText}")
            appendLine("耗时        ${if (record.duration >= 0) "${record.duration}ms" else "处理中"}")
            appendLine("开始时间    ${DATE_FORMAT.format(Date(record.startTime))}")
            if (record.endTime > 0) {
                appendLine("结束时间    ${DATE_FORMAT.format(Date(record.endTime))}")
            }
            appendLine("请求体大小  ${formatSize(record.requestBodySize)}")
            appendLine("响应体大小  ${formatSize(record.responseBodySize)}")
            if (record.error != null) {
                appendLine()
                appendLine("错误信息")
                appendLine(record.error)
            }
        }
    }

    private fun buildHeaders(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "（无）"
        return headers.entries.joinToString("\n") { (k, v) -> "$k: $v" }
    }

    private fun buildBody(body: String?): String {
        if (body.isNullOrBlank()) return "（空）"
        return JsonFormatter.format(body)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0B"
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)}KB"
            else -> "${"%.1f".format(bytes / (1024f * 1024f))}MB"
        }
    }
}
