package com.joelhorrocks.paperclip.ml.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joelhorrocks.paperclip.ml.Language
import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.ml.TranslationModelDownloadStatus

@Entity(tableName = "translationmodels")
data class TranslationModelEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "from_language") val fromLanguage: Language,
    @ColumnInfo(name = "to_language") val toLanguage: Language,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "path") val path: String,
)

fun TranslationModelEntity.toDomain() = TranslationModel(
    this.id,
    this.name,
    this.fromLanguage,
    this.toLanguage,
    this.size,
    TranslationModelDownloadStatus.Downloaded(this.path)
)