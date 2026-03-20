package com.netlog.networklog.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.netlog.networklog.R
import com.netlog.networklog.model.NetworkRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NetworkLogAdapter(
    private val onItemClick: (NetworkRecord) -> Unit
) : ListAdapter<NetworkRecord, NetworkLogAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NetworkRecord>() {
            override fun areItemsTheSame(old: NetworkRecord, new: NetworkRecord) = old.id == new.id
            override fun areContentsTheSame(old: NetworkRecord, new: NetworkRecord) = old == new
        }
        private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_network_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMethod: TextView = itemView.findViewById(R.id.tv_method)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvUrl: TextView = itemView.findViewById(R.id.tv_url)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_size)

        fun bind(record: NetworkRecord) {
            tvMethod.text = record.method
            tvStatus.text = record.statusText
            tvUrl.text = record.url
            tvDuration.text = if (record.duration >= 0) "${record.duration}ms" else "-"
            tvTime.text = TIME_FORMAT.format(Date(record.startTime))
            tvSize.text = formatSize(record.responseBodySize)

            // 状态颜色
            val ctx = itemView.context
            val statusColor = when {
                record.error != null -> ContextCompat.getColor(ctx, R.color.net_error)
                record.statusCode == 0 -> ContextCompat.getColor(ctx, R.color.net_pending)
                record.isSuccess -> ContextCompat.getColor(ctx, R.color.net_success)
                else -> ContextCompat.getColor(ctx, R.color.net_error)
            }
            tvStatus.setTextColor(statusColor)

            // 方法颜色
            val methodColor = when (record.method.uppercase()) {
                "GET" -> ContextCompat.getColor(ctx, R.color.net_method_get)
                "POST" -> ContextCompat.getColor(ctx, R.color.net_method_post)
                "PUT" -> ContextCompat.getColor(ctx, R.color.net_method_put)
                "DELETE" -> ContextCompat.getColor(ctx, R.color.net_method_delete)
                else -> ContextCompat.getColor(ctx, R.color.net_method_other)
            }
            tvMethod.setTextColor(methodColor)

            itemView.setOnClickListener { onItemClick(record) }
        }

        private fun formatSize(bytes: Long): String {
            return when {
                bytes <= 0 -> ""
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024f)}KB"
                else -> "${"%.1f".format(bytes / (1024f * 1024f))}MB"
            }
        }
    }
}
