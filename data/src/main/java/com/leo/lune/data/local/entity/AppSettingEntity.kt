package com.leo.lune.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// App 通用设置：单行 key-value，后续新配置直接加 key 即可
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
