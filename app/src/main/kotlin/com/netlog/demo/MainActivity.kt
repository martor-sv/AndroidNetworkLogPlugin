package com.netlog.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netlog.networklog.NetworkLogPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {

    // OkHttpClient 添加了 NetworkLogPlugin 的拦截器
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(NetworkLogPlugin.getInterceptor())
            .build()
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val btnGet = findViewById<Button>(R.id.btn_get)
        val btnPost = findViewById<Button>(R.id.btn_post)
        val btnError = findViewById<Button>(R.id.btn_error)
        val btnPermission = findViewById<Button>(R.id.btn_permission)

        btnPermission.setOnClickListener {
            if (NetworkLogPlugin.hasOverlayPermission(this)) {
                NetworkLogPlugin.showFloatingWindow(this)
                Toast.makeText(this, "悬浮窗已显示", Toast.LENGTH_SHORT).show()
            } else {
                NetworkLogPlugin.requestOverlayPermission(this)
            }
        }

        btnGet.setOnClickListener {
            tvStatus.text = "GET 请求中..."
            scope.launch {
                try {
                    val request = Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts/1")
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "GET 完成 [${response.code}]\n${body.take(200)}"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "GET 失败：${e.message}"
                    }
                }
            }
        }

        btnPost.setOnClickListener {
            tvStatus.text = "POST 请求中..."
            scope.launch {
                try {
                    val json = """{"title":"NetworkLog Test","body":"hello world","userId":1}"""
                    val body = json.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts")
                        .post(body)
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "POST 完成 [${response.code}]\n${respBody.take(200)}"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "POST 失败：${e.message}"
                    }
                }
            }
        }

        btnError.setOnClickListener {
            tvStatus.text = "发送 404 请求..."
            scope.launch {
                try {
                    val request = Request.Builder()
                        .url("https://jsonplaceholder.typicode.com/posts/99999")
                        .get()
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "请求完成 [${response.code}]"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "请求失败：${e.message}"
                    }
                }
            }
        }
    }
}
