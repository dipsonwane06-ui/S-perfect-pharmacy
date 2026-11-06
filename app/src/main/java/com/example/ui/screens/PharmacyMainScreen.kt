package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CourseType
import com.example.model.ResourceCategory
import com.example.model.SubjectResource
import com.example.ui.AppTab
import com.example.ui.PharmacyViewModel
import com.example.ui.components.ChangeOwnerPinDialog
import com.example.ui.components.HeroBanner
import com.example.ui.components.OwnerLoginDialog
import com.example.ui.components.PharmacyTopBar
import com.example.ui.components.ResourceCard
import com.example.ui.components.ResourceDetailDialog
import com.example.ui.components.SearchAndFilterSection
import com.example.ui.components.UploadStudyMaterialDialog
import com.example.ui.theme.PharmacyAmber
import com.example.ui.theme.PharmacyAmberDark
import com.example.ui.theme.PharmacyEmerald
import com.example.ui.theme.PharmacyNavy

@Composable
fun PharmacyMainScreen(viewModel: PharmacyViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isOwnerMode by viewModel.isOwnerMode.collectAsState()
    val resources by viewModel.filteredResources.collectAsState()
    val allResources by viewModel.allResources.collectAsState()
    val downloadedIds by viewModel.downloadedIds.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val selectedCourse by viewModel.selectedCourse.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val showOwnerLoginDialog by viewModel.showOwnerLoginDialog.collectAsState()
    val ownerLoginError by viewModel.ownerLoginError.collectAsState()
    val showChangePinDialog by viewModel.showChangePinDialog.collectAsState()
    val changePinError by viewModel.changePinError.collectAsState()
    val showUploadDialog by viewModel.showUploadDialog.collectAsState()
    val resourceToEdit by viewModel.resourceToEdit.collectAsState()
    val selectedDetailResource by viewModel.selectedDetailResource.collectAsState()
    val snackBarMessage by viewModel.snackBarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMessage) {
        snackBarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackBarMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("pharmacy_main_screen"),
        topBar = {
            PharmacyTopBar(
                isOwnerMode = isOwnerMode,
                onOwnerLoginClick = { viewModel.openOwnerLogin() },
                onOwnerLogoutClick = { viewModel.exitOwnerMode() },
                onChangePinClick = { viewModel.openChangePinDialog() }
            )
        },
        bottomBar = {
            PharmacyBottomBar(
                currentTab = currentTab,
                onTabSelect = { viewModel.setTab(it) },
                downloadedCount = downloadedIds.size
            )
        },
        floatingActionButton = {
            // Upload button ONLY visible to Owner!
            if (isOwnerMode) {
                FloatingActionButton(
                    onClick = { viewModel.openUploadDialog() },
                    containerColor = PharmacyAmber,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("owner_upload_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Upload Study Material")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Upload Material",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.HOME -> {
                    HomeTabContent(
                        resources = resources,
                        selectedCourse = selectedCourse,
                        onCourseSelected = { viewModel.selectCourse(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        selectedSemester = selectedSemester,
                        onSemesterSelect = { viewModel.selectSemester(it) },
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        downloadedIds = downloadedIds,
                        bookmarkedIds = bookmarkedIds,
                        isOwnerMode = isOwnerMode,
                        onDownloadClick = { viewModel.downloadResource(it) },
                        onViewClick = { viewModel.openResourceLink(it) },
                        onBookmarkClick = { viewModel.toggleBookmark(it.id) },
                        onEditClick = { viewModel.openUploadDialog(it) },
                        onDeleteClick = { viewModel.deleteResource(it.id) },
                        onCardClick = { viewModel.showResourceDetail(it) }
                    )
                }
                AppTab.SEMESTERS -> {
                    SemestersTabContent(
                        course = selectedCourse,
                        selectedSemester = selectedSemester,
                        onSemesterSelect = { sem ->
                            viewModel.selectSemester(sem)
                            viewModel.setTab(AppTab.HOME)
                        },
                        allResources = allResources,
                        onCourseSelect = { viewModel.selectCourse(it) }
                    )
                }
                AppTab.CATEGORIES -> {
                    CategoriesTabContent(
                        allResources = allResources,
                        onCategorySelect = { cat ->
                            viewModel.selectCategory(cat)
                            viewModel.setTab(AppTab.HOME)
                        }
                    )
                }
                AppTab.DOWNLOADS -> {
                    DownloadsTabContent(
                        downloadedResources = resources,
                        downloadedIds = downloadedIds,
                        bookmarkedIds = bookmarkedIds,
                        isOwnerMode = isOwnerMode,
                        onDownloadClick = { viewModel.downloadResource(it) },
                        onViewClick = { viewModel.openResourceLink(it) },
                        onBookmarkClick = { viewModel.toggleBookmark(it.id) },
                        onEditClick = { viewModel.openUploadDialog(it) },
                        onDeleteClick = { viewModel.deleteResource(it.id) },
                        onCardClick = { viewModel.showResourceDetail(it) },
                        onExploreClick = { viewModel.setTab(AppTab.HOME) }
                    )
                }
            }
        }
    }

    // Owner Login Modal Dialog
    if (showOwnerLoginDialog) {
        OwnerLoginDialog(
            onDismiss = { viewModel.dismissOwnerLogin() },
            onConfirm = { pin -> viewModel.authenticateOwner(pin) },
            errorMessage = ownerLoginError
        )
    }

    // Owner Upload/Edit Modal Dialog
    if (showUploadDialog) {
        UploadStudyMaterialDialog(
            initialResource = resourceToEdit,
            onDismiss = { viewModel.dismissUploadDialog() },
            onSave = { id, title, subject, code, course, sem, cat, unit, desc, url, size, pages, featured ->
                viewModel.saveResource(id, title, subject, code, course, sem, cat, unit, desc, url, size, pages, featured)
            }
        )
    }

    // Owner Change PIN Dialog
    if (showChangePinDialog) {
        ChangeOwnerPinDialog(
            onDismiss = { viewModel.dismissChangePinDialog() },
            onConfirm = { currentPin, newPin, confirmPin ->
                viewModel.changeOwnerPin(currentPin, newPin, confirmPin)
            },
            errorMessage = changePinError
        )
    }

    // Resource Detail BottomSheet/Dialog
    selectedDetailResource?.let { resource ->
        ResourceDetailDialog(
            resource = resource,
            isDownloaded = downloadedIds.contains(resource.id),
            isOwnerMode = isOwnerMode,
            onDismiss = { viewModel.dismissResourceDetail() },
            onDownloadClick = { viewModel.downloadResource(resource) },
            onOpenLinkClick = { viewModel.openResourceLink(resource) },
            onEditClick = { viewModel.openUploadDialog(resource) },
            onDeleteClick = { viewModel.deleteResource(resource.id) }
        )
    }
}

@Composable
fun HomeTabContent(
    resources: List<SubjectResource>,
    selectedCourse: CourseType,
    onCourseSelected: (CourseType) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSemester: Int?,
    onSemesterSelect: (Int?) -> Unit,
    selectedCategory: ResourceCategory?,
    onCategorySelect: (ResourceCategory?) -> Unit,
    downloadedIds: Set<Long>,
    bookmarkedIds: Set<Long>,
    isOwnerMode: Boolean,
    onDownloadClick: (SubjectResource) -> Unit,
    onViewClick: (SubjectResource) -> Unit,
    onBookmarkClick: (SubjectResource) -> Unit,
    onEditClick: (SubjectResource) -> Unit,
    onDeleteClick: (SubjectResource) -> Unit,
    onCardClick: (SubjectResource) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            HeroBanner(
                onCourseSelected = onCourseSelected,
                selectedCourse = selectedCourse
            )
        }

        item {
            SearchAndFilterSection(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                selectedSemester = selectedSemester,
                onSemesterSelect = onSemesterSelect,
                course = selectedCourse,
                selectedCategory = selectedCategory,
                onCategorySelect = onCategorySelect
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Materials (${resources.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (selectedSemester != null || selectedCategory != null) {
                    Text(
                        text = "Filtered Results",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (resources.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No study material found matching your filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(resources, key = { it.id }) { item ->
                ResourceCard(
                    resource = item,
                    isDownloaded = downloadedIds.contains(item.id),
                    isBookmarked = bookmarkedIds.contains(item.id),
                    isOwnerMode = isOwnerMode,
                    onDownloadClick = { onDownloadClick(item) },
                    onViewClick = { onViewClick(item) },
                    onBookmarkClick = { onBookmarkClick(item) },
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) },
                    onCardClick = { onCardClick(item) }
                )
            }
        }
    }
}

@Composable
fun SemestersTabContent(
    course: CourseType,
    selectedSemester: Int?,
    onSemesterSelect: (Int) -> Unit,
    allResources: List<SubjectResource>,
    onCourseSelect: (CourseType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse By Semester / Year",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Select a semester to access complete subject notes, question papers, and syllabus.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Course Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CourseType.values().forEach { c ->
                val isSelected = (c == course)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCourseSelect(c) }
                ) {
                    Text(
                        text = c.displayName,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(course.totalSemesters) { index ->
                val sem = index + 1
                val semResourcesCount = allResources.count { it.course == course && it.semester == sem }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSemesterSelect(sem) }
                        .testTag("semester_grid_card_$sem"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$sem",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (course == CourseType.B_PHARM) "Semester $sem" else "Academic Year $sem",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$semResourcesCount Study Files Available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesTabContent(
    allResources: List<SubjectResource>,
    onCategorySelect: (ResourceCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Resource Categories",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Explore study materials neatly categorized as per Imperfect Pharmacy standards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(ResourceCategory.values()) { category ->
            val count = allResources.count { it.category == category }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelect(category) }
                    .testTag("category_card_${category.name}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(category.badgeColorHex),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (category) {
                                    ResourceCategory.HANDWRITTEN_NOTES -> Icons.Default.MenuBook
                                    ResourceCategory.QUESTION_PAPER -> Icons.Default.School
                                    ResourceCategory.PCI_SYLLABUS -> Icons.Default.MenuBook
                                    ResourceCategory.IMPORTANT_QUESTIONS -> Icons.Default.Category
                                    ResourceCategory.PRACTICAL_MANUAL -> Icons.Default.Folder
                                    ResourceCategory.BOOK_REFERENCE -> Icons.Default.MenuBook
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$count Documents & Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsTabContent(
    downloadedResources: List<SubjectResource>,
    downloadedIds: Set<Long>,
    bookmarkedIds: Set<Long>,
    isOwnerMode: Boolean,
    onDownloadClick: (SubjectResource) -> Unit,
    onViewClick: (SubjectResource) -> Unit,
    onBookmarkClick: (SubjectResource) -> Unit,
    onEditClick: (SubjectResource) -> Unit,
    onDeleteClick: (SubjectResource) -> Unit,
    onCardClick: (SubjectResource) -> Unit,
    onExploreClick: () -> Unit
) {
    if (downloadedResources.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Downloads or Saved Items Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Download or bookmark any handwritten notes or question papers from the home screen to access them quickly offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onExploreClick() }
                ) {
                    Text(
                        text = "Browse Study Notes",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "My Offline Study Materials (${downloadedResources.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Saved on this device for instant access anytime without data connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(downloadedResources, key = { it.id }) { item ->
                ResourceCard(
                    resource = item,
                    isDownloaded = downloadedIds.contains(item.id),
                    isBookmarked = bookmarkedIds.contains(item.id),
                    isOwnerMode = isOwnerMode,
                    onDownloadClick = { onDownloadClick(item) },
                    onViewClick = { onViewClick(item) },
                    onBookmarkClick = { onBookmarkClick(item) },
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) },
                    onCardClick = { onCardClick(item) }
                )
            }
        }
    }
}

@Composable
fun PharmacyBottomBar(
    currentTab: AppTab,
    onTabSelect: (AppTab) -> Unit,
    downloadedCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("pharmacy_bottom_bar")
    ) {
        NavigationBarItem(
            selected = (currentTab == AppTab.HOME),
            onClick = { onTabSelect(AppTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("tab_home")
        )

        NavigationBarItem(
            selected = (currentTab == AppTab.SEMESTERS),
            onClick = { onTabSelect(AppTab.SEMESTERS) },
            icon = { Icon(Icons.Default.School, contentDescription = "Semesters") },
            label = { Text("Semesters") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("tab_semesters")
        )

        NavigationBarItem(
            selected = (currentTab == AppTab.CATEGORIES),
            onClick = { onTabSelect(AppTab.CATEGORIES) },
            icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
            label = { Text("Categories") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("tab_categories")
        )

        NavigationBarItem(
            selected = (currentTab == AppTab.DOWNLOADS),
            onClick = { onTabSelect(AppTab.DOWNLOADS) },
            icon = {
                if (downloadedCount > 0) {
                    BadgedBox(badge = { Badge { Text("$downloadedCount") } }) {
                        Icon(Icons.Default.Download, contentDescription = "Downloads")
                    }
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Downloads")
                }
            },
            label = { Text("Downloads") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("tab_downloads")
        )
    }
}
