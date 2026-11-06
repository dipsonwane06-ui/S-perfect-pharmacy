package com.example

import com.example.model.CourseType
import com.example.model.ResourceCategory
import com.example.model.SubjectResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun courseType_semesters_areCorrect() {
        assertEquals(8, CourseType.B_PHARM.totalSemesters)
        assertEquals(2, CourseType.D_PHARM.totalSemesters)
        assertEquals("B.Pharm", CourseType.B_PHARM.shortName)
        assertEquals("D.Pharm", CourseType.D_PHARM.shortName)
    }

    @Test
    fun resourceCategory_properties_areCorrect() {
        assertTrue(ResourceCategory.values().isNotEmpty())
        assertEquals("Handwritten Notes", ResourceCategory.HANDWRITTEN_NOTES.displayName)
        assertEquals("Previous Year Papers (PYQ)", ResourceCategory.QUESTION_PAPER.displayName)
        assertEquals("PCI Syllabus", ResourceCategory.PCI_SYLLABUS.displayName)
    }

    @Test
    fun subjectResource_creation_isCorrect() {
        val resource = SubjectResource(
            id = 1,
            title = "Human Anatomy and Physiology Notes",
            subjectName = "HAP-I",
            subjectCode = "BP101T",
            course = CourseType.B_PHARM,
            semester = 1,
            category = ResourceCategory.HANDWRITTEN_NOTES,
            unitOrTopic = "Unit 1",
            description = "Introduction to human body",
            fileUrl = "https://example.com/notes.pdf",
            fileSize = "4.5 MB",
            pageCount = 45,
            uploadedBy = "S Perfect Pharmacy",
            uploadDate = "Nov 2026",
            downloadsCount = 100,
            isFeatured = true,
            isOwnerUploaded = true
        )
        assertEquals(1L, resource.id)
        assertEquals("BP101T", resource.subjectCode)
        assertTrue(resource.isOwnerUploaded)
    }
}
