package com.netlog.networklog.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.netlog.networklog.R
import com.netlog.networklog.model.NetworkRecord
import com.netlog.networklog.repository.NetworkLogRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NetworkLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var adapter: NetworkLogAdapter

    private var allRecords: List<NetworkRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_log)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "网络日志"
            setDisplayHomeAsUpEnabled(true)
        }

        recyclerView = findViewById(R.id.recycler_view)
        etSearch = findViewById(R.id.et_search)

        adapter = NetworkLogAdapter { record ->
            val intent = NetworkLogDetailActivity.newIntent(this, record.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 搜索过滤
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        // 观察数据
        lifecycleScope.launch {
            NetworkLogRepository.records.collectLatest { records ->
                allRecords = records
                filter(etSearch.text?.toString() ?: "")
            }
        }
    }

    private fun filter(keyword: String) {
        val filtered = if (keyword.isBlank()) {
            allRecords
        } else {
            allRecords.filter { it.url.contains(keyword, ignoreCase = true) }
        }
        adapter.submitList(filtered.toList())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_network_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear -> {
                NetworkLogRepository.clearAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
