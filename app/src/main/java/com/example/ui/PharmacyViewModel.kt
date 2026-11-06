package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PharmacyRepository
import com.example.model.CourseType
import com.example.model.ResourceCategory
import com.example.model.SubjectResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    HOME("Home"),
    SEMESTERS("Semesters"),
    CATEGORIES("Categories"),
    DOWNLOADS("My Downloads")
}

class PharmacyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PharmacyRepository(application.applicationContext)

    val isOwnerMode = repository.isOwnerAuthenticated
    val downloadedIds = repository.downloadedIds
    val bookmarkedIds = repository.bookmarkedIds

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCourse = MutableStateFlow(CourseType.B_PHARM)
    val selectedCourse = _selectedCourse.asStateFlow()

    private val _selectedSemester = MutableStateFlow<Int?>(null)
    val selectedSemester = _selectedSemester.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ResourceCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _showOwnerLoginDialog = MutableStateFlow(false)
    val showOwnerLoginDialog = _showOwnerLoginDialog.asStateFlow()

    private val _ownerLoginError = MutableStateFlow<String?>(null)
    val ownerLoginError = _ownerLoginError.asStateFlow()

    private val _showUploadDialog = MutableStateFlow(false)
    val showUploadDialog = _showUploadDialog.asStateFlow()

    private val _resourceToEdit = MutableStateFlow<SubjectResource?>(null)
    val resourceToEdit = _resourceToEdit.asStateFlow()

    private val _selectedDetailResource = MutableStateFlow<SubjectResource?>(null)
    val selectedDetailResource = _selectedDetailResource.asStateFlow()

    private val _showChangePinDialog = MutableStateFlow(false)
    val showChangePinDialog = _showChangePinDialog.asStateFlow()

    private val _changePinError = MutableStateFlow<String?>(null)
    val changePinError = _changePinError.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage = _snackBarMessage.asStateFlow()

    val allResources: StateFlow<List<SubjectResource>> = repository.getAllResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredResources: StateFlow<List<SubjectResource>> = combine(
        allResources,
        _searchQuery,
        _selectedCourse,
        _selectedSemester,
        _selectedCategory,
        _currentTab,
        downloadedIds,
        bookmarkedIds
    ) { params ->
        val list = params[0] as List<SubjectResource>
        val query = (params[1] as String).trim().lowercase()
        val course = params[2] as CourseType
        val sem = params[3] as Int?
        val cat = params[4] as ResourceCategory?
        val tab = params[5] as AppTab
        val downloaded = params[6] as Set<Long>
        val bookmarked = params[7] as Set<Long>

        var result = list

        when (tab) {
            AppTab.DOWNLOADS -> {
                result = result.filter { downloaded.contains(it.id) || bookmarked.contains(it.id) }
            }
            AppTab.SEMESTERS -> {
                result = result.filter { it.course == course }
                if (sem != null) {
                    result = result.filter { it.semester == sem }
                }
            }
            AppTab.CATEGORIES -> {
                if (cat != null) {
                    result = result.filter { it.category == cat }
                }
            }
            AppTab.HOME -> {
                // Filter by course if selected in Home
                result = result.filter { it.course == course }
                if (sem != null) {
                    result = result.filter { it.semester == sem }
                }
                if (cat != null) {
                    result = result.filter { it.category == cat }
                }
            }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.title.lowercase().contains(query) ||
                it.subjectName.lowercase().contains(query) ||
                it.subjectCode.lowercase().contains(query) ||
                it.unitOrTopic.lowercase().contains(query) ||
                it.category.displayName.lowercase().contains(query)
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCourse(course: CourseType) {
        _selectedCourse.value = course
        // Reset semester if out of bounds for the selected course
        if (_selectedSemester.value != null && _selectedSemester.value!! > course.totalSemesters) {
            _selectedSemester.value = null
        }
    }

    fun selectSemester(semester: Int?) {
        _selectedSemester.value = if (_selectedSemester.value == semester) null else semester
    }

    fun selectCategory(category: ResourceCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openOwnerLogin() {
        _ownerLoginError.value = null
        _showOwnerLoginDialog.value = true
    }

    fun dismissOwnerLogin() {
        _showOwnerLoginDialog.value = false
        _ownerLoginError.value = null
    }

    fun authenticateOwner(pin: String) {
        if (repository.verifyOwnerPin(pin)) {
            _showOwnerLoginDialog.value = false
            _ownerLoginError.value = null
            _snackBarMessage.value = "Owner Mode Activated! You can now upload, edit, and delete files."
        } else {
            _ownerLoginError.value = "Incorrect PIN. Default is 1234"
        }
    }

    fun exitOwnerMode() {
        repository.logoutOwner()
        _snackBarMessage.value = "Switched to Student Mode (View & Download Only)"
    }

    fun openChangePinDialog() {
        _changePinError.value = null
        _showChangePinDialog.value = true
    }

    fun dismissChangePinDialog() {
        _showChangePinDialog.value = false
        _changePinError.value = null
    }

    fun changeOwnerPin(currentPin: String, newPin: String, confirmPin: String) {
        if (newPin.trim().length < 4) {
            _changePinError.value = "New PIN must be at least 4 digits"
            return
        }
        if (newPin.trim() != confirmPin.trim()) {
            _changePinError.value = "New PIN and Confirm PIN do not match"
            return
        }
        val success = repository.changeOwnerPin(currentPin, newPin)
        if (success) {
            _showChangePinDialog.value = false
            _changePinError.value = null
            _snackBarMessage.value = "Owner PIN changed successfully! New PIN is now active."
        } else {
            _changePinError.value = "Incorrect current PIN. Please try again."
        }
    }

    fun openUploadDialog(resource: SubjectResource? = null) {
        _resourceToEdit.value = resource
        _showUploadDialog.value = true
    }

    fun dismissUploadDialog() {
        _showUploadDialog.value = false
        _resourceToEdit.value = null
    }

    fun saveResource(
        id: Long,
        title: String,
        subjectName: String,
        subjectCode: String,
        course: CourseType,
        semester: Int,
        category: ResourceCategory,
        unitOrTopic: String,
        description: String,
        fileUrl: String,
        fileSize: String,
        pageCount: Int,
        isFeatured: Boolean
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                val newResource = SubjectResource(
                    id = 0,
                    title = title.trim(),
                    subjectName = subjectName.trim(),
                    subjectCode = subjectCode.trim().uppercase(),
                    course = course,
                    semester = semester,
                    category = category,
                    unitOrTopic = unitOrTopic.trim(),
                    description = description.trim(),
                    fileUrl = fileUrl.trim(),
                    fileSize = fileSize.trim(),
                    pageCount = pageCount,
                    uploadedBy = "S Perfect Pharmacy (Owner)",
                    uploadDate = "Nov 2026",
                    isFeatured = isFeatured,
                    isOwnerUploaded = true
                )
                repository.addResource(newResource)
                _snackBarMessage.value = "New study material uploaded successfully!"
            } else {
                val existing = _resourceToEdit.value
                val updatedResource = SubjectResource(
                    id = id,
                    title = title.trim(),
                    subjectName = subjectName.trim(),
                    subjectCode = subjectCode.trim().uppercase(),
                    course = course,
                    semester = semester,
                    category = category,
                    unitOrTopic = unitOrTopic.trim(),
                    description = description.trim(),
                    fileUrl = fileUrl.trim(),
                    fileSize = fileSize.trim(),
                    pageCount = pageCount,
                    uploadedBy = existing?.uploadedBy ?: "S Perfect Pharmacy (Owner)",
                    uploadDate = existing?.uploadDate ?: "Nov 2026",
                    downloadsCount = existing?.downloadsCount ?: 0,
                    isFeatured = isFeatured,
                    isOwnerUploaded = true
                )
                repository.updateResource(updatedResource)
                _snackBarMessage.value = "Study material updated successfully!"
            }
            dismissUploadDialog()
        }
    }

    fun deleteResource(id: Long) {
        viewModelScope.launch {
            repository.deleteResource(id)
            if (_selectedDetailResource.value?.id == id) {
                _selectedDetailResource.value = null
            }
            _snackBarMessage.value = "Study material deleted."
        }
    }

    fun downloadResource(resource: SubjectResource) {
        repository.downloadResource(resource)
    }

    fun openResourceLink(resource: SubjectResource) {
        repository.openResourceLink(resource)
    }

    fun toggleBookmark(id: Long) {
        repository.toggleBookmark(id)
    }

    fun showResourceDetail(resource: SubjectResource) {
        _selectedDetailResource.value = resource
    }

    fun dismissResourceDetail() {
        _selectedDetailResource.value = null
    }

    fun clearSnackBarMessage() {
        _snackBarMessage.value = null
    }
}
