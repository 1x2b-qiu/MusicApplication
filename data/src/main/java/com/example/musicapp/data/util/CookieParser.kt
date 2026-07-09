package com.example.musicapp.data.util

fun parseNeteaseCookie(raw: Any?): String {
    val cookieString = when (raw) {
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
