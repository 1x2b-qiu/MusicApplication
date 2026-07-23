package com.leo.lune.data.util

// 解析登录响应中的 Cookie 字符串，供请求头使用
// 兼容字符串、列表等格式，并过滤掉 expires/path 等非会话字段
fun parseNeteaseCookie(raw: Any?): String {    val cookieString = when (raw) {
        null -> ""
        is String -> raw
        is List<*> -> raw.filterIsInstance<String>().joinToString("; ")
        else -> raw.toString()
    }
    if (cookieString.isBlank()) return ""

    return cookieString.split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { part ->
            val lower = part.lowercase()
            lower.startsWith("expires=") ||
                lower.startsWith("path=") ||
                lower.startsWith("domain=") ||
                lower.startsWith("max-age=") ||
                lower.startsWith("samesite=") ||
                lower == "httponly" ||
                lower == "secure"
        }
        .joinToString("; ")
}
