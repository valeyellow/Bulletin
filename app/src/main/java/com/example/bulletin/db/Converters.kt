package com.example.bulletin.db

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.bulletin.model.Source

class Converters {

    @TypeConverter
    fun fromSource(source: Source): String = source.name

    @TypeConverter
    fun toSource(name: String) = Source(name, name)
}