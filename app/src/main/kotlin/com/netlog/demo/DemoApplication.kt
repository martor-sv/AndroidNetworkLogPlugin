package com.netlog.demo

import android.app.Application
import com.netlog.networklog.NetworkLogPlugin

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 安装插件（仅在 Debug 构建中使用）
        NetworkLogPlugin.install(this, showFloating = true)
    }
}
