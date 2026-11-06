package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CourseType
import com.example.model.ResourceCategory
import com.example.model.SubjectResource

@Entity(tableName = "subject_resources")
data class SubjectResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subjectName: String,
    val subjectCode: String,
    val course: CourseType,
    val semester: Int,
    val category: ResourceCategory,
    val unitOrTopic: String,
    val description: String,
    val fileUrl: String,
    val fileSize: String,
    val pageCount: Int,
    val uploadedBy: String,
    val uploadDate: String,
    val downloadsCount: Int,
    val isFeatured: Boolean,
    val isOwnerUploaded: Boolean
) {
    fun toDomain(): SubjectResource = SubjectResource(
        id = id,
        title = title,
        subjectName = subjectName,
        subjectCode = subjectCode,
        course = course,
        semester = semester,
        category = category,
        unitOrTopic = unitOrTopic,
        description = description,
        fileUrl = fileUrl,
        fileSize = fileSize,
        pageCount = pageCount,
        uploadedBy = uploadedBy,
        uploadDate = uploadDate,
        downloadsCount = downloadsCount,
        isFeatured = isFeatured,
        isOwnerUploaded = isOwnerUploaded
    )

    companion object {
        fun fromDomain(resource: SubjectResource): SubjectResourceEntity = SubjectResourceEntity(
            id = resource.id,
            title = resource.title,
            subjectName = resource.subjectName,
            subjectCode = resource.subjectCode,
            course = resource.course,
            semester = resource.semester,
            category = resource.category,
            unitOrTopic = resource.unitOrTopic,
            description = resource.description,
            fileUrl = resource.fileUrl,
            fileSize = resource.fileSize,
            pageCount = resource.pageCount,
            uploadedBy = resource.uploadedBy,
            uploadDate = resource.uploadDate,
            downloadsCount = resource.downloadsCount,
            isFeatured = resource.isFeatured,
            isOwnerUploaded = resource.isOwnerUploaded
        )
    }
}
