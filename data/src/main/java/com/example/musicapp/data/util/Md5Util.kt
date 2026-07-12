package com.example.musicapp.data.util

import java.security.MessageDigest

// 将字符串转为小写十六进制 MD5，用于网易云密码登录
fun String.toMd5(): String {
    val digest = MessageDigest.getInstance("MD5")
    val bytes = digest.digest(toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
