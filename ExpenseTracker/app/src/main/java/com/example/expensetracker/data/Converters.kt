package com.example.expensetracker.data

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long? {
        return date.toEpochDay()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate? {
        return LocalDate.ofEpochDay(epochDay)
    }
}