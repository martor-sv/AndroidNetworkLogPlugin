package com.netlog.networklog.repository

import com.netlog.networklog.model.NetworkRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 网络日志数据仓库（单例）
 * 内存中保存最新 MAX_RECORDS 条记录
 */
object NetworkLogRepository {

    private const val MAX_RECORDS = 500

    private val _records = MutableStateFlow<List<NetworkRecord>>(emptyList())
    val records: StateFlow<List<NetworkRecord>> = _records.asStateFlow()

    // 未读数
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    @Synchronized
    fun addRecord(record: NetworkRecord) {
        val current = _records.value.toMutableList()
        current.add(0, record)
        if (current.size > MAX_RECORDS) {
            current.removeAt(current.lastIndex)
        }
        _records.value = current
        _unreadCount.value = _unreadCount.value + 1
    }

    @Synchronized
    fun updateRecord(id: Long, updater: (NetworkRecord) -> Unit) {
        val current = _records.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            updater(current[index])
            _records.value = current.toList()
        }
    }

    @Synchronized
    fun clearAll() {
        _records.value = emptyList()
        _unreadCount.value = 0
    }

    fun resetUnread() {
        _unreadCount.value = 0
    }

    fun getRecord(id: Long): NetworkRecord? = _records.value.firstOrNull { it.id == id }
}
