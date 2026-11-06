package com.example.model

enum class CourseType(val displayName: String, val shortName: String, val totalSemesters: Int) {
    B_PHARM("B. Pharmacy (Bachelor)", "B.Pharm", 8),
    D_PHARM("D. Pharmacy (Diploma)", "D.Pharm", 2)
}

enum class ResourceCategory(val displayName: String, val iconName: String, val badgeColorHex: Long) {
    HANDWRITTEN_NOTES("Handwritten Notes", "description", 0xFF00897B), // Teal
    QUESTION_PAPER("Previous Year Papers (PYQ)", "quiz", 0xFFE65100), // Amber/Orange
    PCI_SYLLABUS("PCI Syllabus", "menu_book", 0xFF0277BD), // Blue
    IMPORTANT_QUESTIONS("Important Questions", "stars", 0xFFC2185B), // Deep Pink
    PRACTICAL_MANUAL("Practical Lab Manual", "science", 0xFF6A1B9A), // Purple
    BOOK_REFERENCE("Reference Books", "library_books", 0xFF37474F) // Slate
}

data class SubjectResource(
    val id: Long = 0,
    val title: String,
    val subjectName: String,
    val subjectCode: String,
    val course: CourseType,
    val semester: Int, // 1 to 8 for B.Pharm; 1 or 2 for D.Pharm (Years)
    val category: ResourceCategory,
    val unitOrTopic: String,
    val description: String,
    val fileUrl: String,
    val fileSize: String,
    val pageCount: Int,
    val uploadedBy: String = "S Perfect Pharmacy",
    val uploadDate: String = "2024",
    val downloadsCount: Int = 0,
    val isFeatured: Boolean = false,
    val isOwnerUploaded: Boolean = false
)
