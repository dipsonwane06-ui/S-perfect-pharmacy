package com.example.data

import androidx.room.TypeConverter
import com.example.model.CourseType
import com.example.model.ResourceCategory

class PharmacyConverters {
    @TypeConverter
    fun fromCourseType(course: CourseType?): String? = course?.name

    @TypeConverter
    fun toCourseType(value: String?): CourseType? = value?.let {
        try {
            CourseType.valueOf(it)
        } catch (_: Exception) {
            CourseType.B_PHARM
        }
    }

    @TypeConverter
    fun fromCategory(category: ResourceCategory?): String? = category?.name

    @TypeConverter
    fun toCategory(value: String?): ResourceCategory? = value?.let {
        try {
            ResourceCategory.valueOf(it)
        } catch (_: Exception) {
            ResourceCategory.HANDWRITTEN_NOTES
        }
    }
}
