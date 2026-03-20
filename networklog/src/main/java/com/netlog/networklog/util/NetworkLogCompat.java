package com.netlog.networklog.util;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import java.lang.reflect.Method;

/**
 * 跨版本兼容性辅助类
 * 解决 OkHttp 3.x/4.x 和 Okio 1.x/2.x 之间的方法重命名导致的 NoSuchMethodError
 */
public class NetworkLogCompat {

    public static ResponseBody createResponseBody(MediaType contentType, String content) {
        try {
            // 在 OkHttp 4.0+ 中底层会映射到 Companion，但 Java 可直接匹配静态方法
            return ResponseBody.create(contentType, content);
        } catch (Throwable e) {
            // 反射强制调用以防万一
            try {
                Method method = ResponseBody.class.getMethod("create", MediaType.class, String.class);
                return (ResponseBody) method.invoke(null, contentType, content);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static String header(Request request, String name) {
        try { return request.header(name); } catch (NoSuchMethodError e) { return (String) invokeMethod(request, "header", new Class[]{String.class}, new Object[]{name}); }
    }

    public static String header(Response response, String name) {
        try { return response.header(name); } catch (NoSuchMethodError e) { return (String) invokeMethod(response, "header", new Class[]{String.class}, new Object[]{name}); }
    }

    public static Request request(Response response) {
        try { return response.request(); } catch (NoSuchMethodError e) { return (Request) invokeMethod(response, "request"); }
    }

    public static Object requestBody(Request request) {
        try { return request.body(); } catch (NoSuchMethodError e) { return invokeMethod(request, "body"); }
    }

    public static String requestMethod(Request request) {
        try { return request.method(); } catch (NoSuchMethodError e) { return (String) invokeMethod(request, "method"); }
    }

    public static HttpUrl requestUrl(Request request) {
        try { return request.url(); } catch (NoSuchMethodError e) { return (HttpUrl) invokeMethod(request, "url"); }
    }

    public static ResponseBody responseBody(Response response) {
        try { return response.body(); } catch (NoSuchMethodError e) { return (ResponseBody) invokeMethod(response, "body"); }
    }

    public static int responseCode(Response response) {
        try { return response.code(); } catch (NoSuchMethodError e) { return (int) invokeMethod(response, "code"); }
    }

    public static Headers requestHeaders(Request request) {
        try { return request.headers(); } catch (NoSuchMethodError e) { return (Headers) invokeMethod(request, "headers"); }
    }

    public static Headers responseHeaders(Response response) {
        try { return response.headers(); } catch (NoSuchMethodError e) { return (Headers) invokeMethod(response, "headers"); }
    }

    public static int headersSize(Headers headers) {
        try { return headers.size(); } catch (NoSuchMethodError e) { return (int) invokeMethod(headers, "size"); }
    }

    public static long bufferSize(Buffer buffer) {
        try { return buffer.size(); } catch (NoSuchMethodError e) { return (long) invokeMethod(buffer, "size"); }
    }

    public static Buffer getBuffer(BufferedSource source) {
        try {
            Method method = source.getClass().getMethod("buffer");
            return (Buffer) method.invoke(source);
        } catch (Exception e1) {
            try {
                Method method = source.getClass().getMethod("getBuffer");
                return (Buffer) method.invoke(source);
            } catch (Exception e2) {
                return source.buffer();
            }
        }
    }

    public static Object invokeMethod(Object target, String methodName) {
        return invokeMethod(target, methodName, new Class[0], new Object[0]);
    }

    public static Object invokeMethod(Object target, String methodName, Class[] parameterTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }
}
