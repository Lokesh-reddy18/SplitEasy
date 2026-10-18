package com.split.easy.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Room stores enums by name on its own, so only dates need a converter.
 * A LocalDate is stored as its epoch day (days since 1970-01-01), which sorts correctly.
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)
}
