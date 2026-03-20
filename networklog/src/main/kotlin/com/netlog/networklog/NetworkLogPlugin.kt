package com.netlog.networklog

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.netlog.networklog.interceptor.NetworkLogInterceptor
import com.netlog.networklog.repository.NetworkLogRepository
import com.netlog.networklog.ui.FloatingWindowService

/**
 * 网络日志插件入口
 *
 * 使用方法：
 * 1. Application.onCreate() 中调用 NetworkLogPlugin.install(this)
 * 2. OkHttpClient.Builder 中添加 NetworkLogPlugin.getInterceptor()
 */
object NetworkLogPlugin {

    private val _interceptor by lazy { NetworkLogInterceptor() }
    private var applicationContext: Context? = null

    /**
     * 初始化插件，并启动悬浮窗
     * @param application Application 实例
     * @param showFloating 是否立即显示悬浮窗（默认 true）
     */
    @JvmStatic
    fun install(application: Application, showFloating: Boolean = true) {
        applicationContext = application.applicationContext
        if (showFloating) {
            showFloatingWindow(applicationContext!!)
        }
    }

    /**
     * 一键式启动：权限检查 -> 申请（如有必要） -> 显示悬浮控
     * 适合在“开启日志”按钮的点击事件中一行代码调用
     */
    @JvmStatic
    fun open(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
        showFloatingWindow(context)
    }

    /**
     * 获取 OkHttp 拦截器，添加到 OkHttpClient.Builder
     */
    @JvmStatic
    fun getInterceptor(): NetworkLogInterceptor = _interceptor

    /**
     * 尝试显示悬浮窗
     * 如果没有权限，弹出 toast 提示
     */
    fun showFloatingWindow(context: Context) {
        if (hasOverlayPermission(context)) {
            val intent = Intent(context, FloatingWindowService::class.java)
            context.startService(intent)
        } else {
            Toast.makeText(context, "请授予悬浮窗权限以使用网络日志查看功能", Toast.LENGTH_LONG).show()
            requestOverlayPermission(context)
        }
    }

    /**
     * 跳转到悬浮窗权限设置页面
     */
    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * 检查是否有悬浮窗权限
     */
    @JvmStatic
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 清除所有日志
     */
    @JvmStatic
    fun clearLogs() {
        NetworkLogRepository.clearAll()
    }

    /**
     * 停止悬浮窗
     */
    @JvmStatic
    fun dismiss(context: Context) {
        val intent = Intent(context, FloatingWindowService::class.java)
        context.stopService(intent)
    }
}
