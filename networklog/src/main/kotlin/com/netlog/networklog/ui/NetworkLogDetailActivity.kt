package com.netlog.networklog.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.netlog.networklog.R
import com.netlog.networklog.model.NetworkRecord
import com.netlog.networklog.repository.NetworkLogRepository

class NetworkLogDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"

        fun newIntent(context: Context, recordId: Long): Intent {
            return Intent(context, NetworkLogDetailActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_log_detail)

        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val record = NetworkLogRepository.getRecord(recordId) ?: run {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "${record.method} ${truncateUrl(record.url)}"
            setDisplayHomeAsUpEnabled(true)
        }

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)

        val tabs = listOf("概览", "请求头", "请求体", "响应头", "响应体")
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = tabs.size
            override fun createFragment(position: Int): Fragment {
                return NetworkLogDetailFragment.newInstance(recordId, position)
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun truncateUrl(url: String): String {
        return if (url.length > 50) url.takeLast(50) else url
    }
}
