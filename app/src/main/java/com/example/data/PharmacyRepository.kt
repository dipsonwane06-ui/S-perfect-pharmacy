package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.model.CourseType
import com.example.model.ResourceCategory
import com.example.model.SubjectResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PharmacyRepository(private val context: Context) {

    private val db = PharmacyDatabase.getInstance(context)
    private val dao = db.subjectResourceDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("yes_pharmacy_prefs", Context.MODE_PRIVATE)

    private val _downloadedIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedIds = _downloadedIds.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())
    val bookmarkedIds = _bookmarkedIds.asStateFlow()

    private val _isOwnerAuthenticated = MutableStateFlow(false)
    val isOwnerAuthenticated = _isOwnerAuthenticated.asStateFlow()

    companion object {
        private const val KEY_OWNER_PIN = "owner_pin"
        private const val DEFAULT_PIN = "1234"
        private const val KEY_DOWNLOADED_IDS = "downloaded_ids"
        private const val KEY_BOOKMARKED_IDS = "bookmarked_ids"
        private const val KEY_INITIAL_SEEDED = "initial_seeded_v1"
    }

    init {
        loadPreferences()
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    private fun loadPreferences() {
        val downloaded = prefs.getStringSet(KEY_DOWNLOADED_IDS, emptySet()) ?: emptySet()
        _downloadedIds.value = downloaded.mapNotNull { it.toLongOrNull() }.toSet()

        val bookmarked = prefs.getStringSet(KEY_BOOKMARKED_IDS, emptySet()) ?: emptySet()
        _bookmarkedIds.value = bookmarked.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun getAllResources(): Flow<List<SubjectResource>> {
        return dao.getAllResources().map { list -> list.map { it.toDomain() } }
    }

    fun searchResources(query: String): Flow<List<SubjectResource>> {
        return dao.searchResources(query.trim()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addResource(resource: SubjectResource): Long = withContext(Dispatchers.IO) {
        dao.insert(SubjectResourceEntity.fromDomain(resource.copy(isOwnerUploaded = true)))
    }

    suspend fun updateResource(resource: SubjectResource) = withContext(Dispatchers.IO) {
        dao.update(SubjectResourceEntity.fromDomain(resource))
    }

    suspend fun deleteResource(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    fun verifyOwnerPin(pin: String): Boolean {
        val savedPin = prefs.getString(KEY_OWNER_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        val matches = (pin.trim() == savedPin)
        if (matches) {
            _isOwnerAuthenticated.value = true
        }
        return matches
    }

    fun logoutOwner() {
        _isOwnerAuthenticated.value = false
    }

    fun changeOwnerPin(currentPin: String, newPin: String): Boolean {
        if (verifyOwnerPin(currentPin) && newPin.isNotBlank()) {
            prefs.edit().putString(KEY_OWNER_PIN, newPin.trim()).apply()
            return true
        }
        return false
    }

    fun toggleBookmark(id: Long) {
        val current = _bookmarkedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _bookmarkedIds.value = current
        prefs.edit().putStringSet(KEY_BOOKMARKED_IDS, current.map { it.toString() }.toSet()).apply()
    }

    fun downloadResource(resource: SubjectResource) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.incrementDownloadCount(resource.id)
            val current = _downloadedIds.value.toMutableSet()
            current.add(resource.id)
            _downloadedIds.value = current
            prefs.edit().putStringSet(KEY_DOWNLOADED_IDS, current.map { it.toString() }.toSet()).apply()

            withContext(Dispatchers.Main) {
                try {
                    val uri = Uri.parse(resource.fileUrl)
                    if (resource.fileUrl.startsWith("http://") || resource.fileUrl.startsWith("https://")) {
                        val request = DownloadManager.Request(uri)
                            .setTitle("${resource.title}.pdf")
                            .setDescription("Downloading study material from S Perfect Pharmacy")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "YesPerfectPharmacy/${resource.subjectCode}_${resource.id}.pdf"
                            )
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)

                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        downloadManager?.enqueue(request)
                        Toast.makeText(context, "Download started for: ${resource.title}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Offline material saved to My Downloads: ${resource.title}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Saved to your offline library: ${resource.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openResourceLink(resource: SubjectResource) {
        try {
            val uri = Uri.parse(resource.fileUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Viewing: ${resource.title} (${resource.fileSize})", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val count = dao.getCount()
        if (count > 0 && prefs.getBoolean(KEY_INITIAL_SEEDED, false)) {
            return
        }

        val seedList = getInitialPharmacyResources()
        dao.insertAll(seedList.map { SubjectResourceEntity.fromDomain(it) })
        prefs.edit().putBoolean(KEY_INITIAL_SEEDED, true).apply()
    }

    private fun getInitialPharmacyResources(): List<SubjectResource> {
        return listOf(
            // === B.PHARM SEMESTER 1 ===
            SubjectResource(
                id = 1,
                title = "HAP-I Complete Unit 1 to 5 Handwritten Notes",
                subjectName = "Human Anatomy and Physiology I",
                subjectCode = "BP101T",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1-5 Complete Course",
                description = "Clear handwritten notes covering cellular organization, tissues, skeletal & muscular systems, cardiovascular & lymphatic system with well-labeled diagrams.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem1_hap1.pdf",
                fileSize = "6.4 MB",
                pageCount = 68,
                downloadsCount = 1420,
                isFeatured = true
            ),
            SubjectResource(
                id = 2,
                title = "Pharmaceutical Analysis I - Volumetric & Electrochemical Titrations",
                subjectName = "Pharmaceutical Analysis I",
                subjectCode = "BP102T",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1, 2 & 3: Titrimetry & Conductometry",
                description = "In-depth explanations of acid-base titration, non-aqueous titration, precipitation and complexometric titration techniques as per PCI standard.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem1_analysis.pdf",
                fileSize = "4.8 MB",
                pageCount = 52,
                downloadsCount = 980,
                isFeatured = false
            ),
            SubjectResource(
                id = 3,
                title = "Pharmaceutics I - Prescription, Posology & Liquid Dosage Forms",
                subjectName = "Pharmaceutics I",
                subjectCode = "BP103T",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 & 2: Historical Background & Liquids",
                description = "Prescription parts, posology calculation formulas, syrups, elixirs, suspensions and emulsions stability parameters.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem1_pharmaceutics.pdf",
                fileSize = "5.1 MB",
                pageCount = 56,
                downloadsCount = 1150,
                isFeatured = true
            ),
            SubjectResource(
                id = 4,
                title = "Inorganic Chemistry - Limit Tests & Gastrointestinal Agents",
                subjectName = "Pharmaceutical Inorganic Chemistry",
                subjectCode = "BP104T",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1: Limit Test of Chloride, Sulfate, Iron & Arsenic",
                description = "Complete principle, apparatus and chemical reactions for all limit tests, antacids, cathartics and antimicrobials.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem1_inorganic.pdf",
                fileSize = "3.9 MB",
                pageCount = 44,
                downloadsCount = 890,
                isFeatured = false
            ),
            SubjectResource(
                id = 5,
                title = "B.Pharm 1st Semester Solved Previous Year Question Papers (2020-2024)",
                subjectName = "All 1st Sem Subjects",
                subjectCode = "BP101-104T",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.QUESTION_PAPER,
                unitOrTopic = "University Exams (PYQ 5 Years)",
                description = "Curated previous year question papers with model solution keys from leading pharmacy universities following PCI scheme.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem1_pyq.pdf",
                fileSize = "8.2 MB",
                pageCount = 84,
                downloadsCount = 2340,
                isFeatured = true
            ),
            SubjectResource(
                id = 6,
                title = "PCI Official Syllabus - B.Pharm 1st & 2nd Semester",
                subjectName = "Curriculum Guidelines",
                subjectCode = "PCI-BP-Y1",
                course = CourseType.B_PHARM,
                semester = 1,
                category = ResourceCategory.PCI_SYLLABUS,
                unitOrTopic = "Course Regulations & Examination Scheme",
                description = "Official Pharmacy Council of India syllabus outlining credit points, course objectives, theory and practical hours.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/pci_bpharm_syllabus.pdf",
                fileSize = "2.1 MB",
                pageCount = 38,
                downloadsCount = 1890,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 2 ===
            SubjectResource(
                id = 7,
                title = "HAP-II Nervous, Digestive & Endocrine System Notes",
                subjectName = "Human Anatomy and Physiology II",
                subjectCode = "BP201T",
                course = CourseType.B_PHARM,
                semester = 2,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 to 4: CNS, ANS, GI Tract & Endocrine",
                description = "Detailed pathways of nervous system, brain structures, digestive secretions, pituitary hormones and reproductive physiology.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem2_hap2.pdf",
                fileSize = "5.8 MB",
                pageCount = 62,
                downloadsCount = 870,
                isFeatured = false
            ),
            SubjectResource(
                id = 8,
                title = "Organic Chemistry I - IUPAC Nomenclature & Alkane/Alkene Mechanisms",
                subjectName = "Pharmaceutical Organic Chemistry I",
                subjectCode = "BP202T",
                course = CourseType.B_PHARM,
                semester = 2,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 & 2: SN1, SN2, E1, E2 Mechanisms",
                description = "Reaction mechanisms, stereochemistry basics, Markovnikov and anti-Markovnikov additions with easy handwritten steps.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem2_org1.pdf",
                fileSize = "7.1 MB",
                pageCount = 74,
                downloadsCount = 1260,
                isFeatured = true
            ),
            SubjectResource(
                id = 9,
                title = "Biochemistry - Glycolysis, Kreb's Cycle & Enzyme Kinetics",
                subjectName = "Biochemistry",
                subjectCode = "BP203T",
                course = CourseType.B_PHARM,
                semester = 2,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Carbohydrate & Lipid Metabolism, Enzymes",
                description = "Step-by-step metabolic cycle diagrams, ATP generation yields, Michaelis-Menten enzyme kinetics and inhibition models.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem2_biochem.pdf",
                fileSize = "5.3 MB",
                pageCount = 58,
                downloadsCount = 940,
                isFeatured = false
            ),
            SubjectResource(
                id = 10,
                title = "Pathophysiology - Essential Questions & Flowcharts",
                subjectName = "Pathophysiology",
                subjectCode = "BP204T",
                course = CourseType.B_PHARM,
                semester = 2,
                category = ResourceCategory.IMPORTANT_QUESTIONS,
                unitOrTopic = "Atherosclerosis, Diabetes, Cancer & Asthma",
                description = "Exam-oriented flowcharts and etiopathogenesis of cardiovascular, respiratory, renal and endocrine diseases.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem2_patho.pdf",
                fileSize = "4.2 MB",
                pageCount = 46,
                downloadsCount = 820,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 3 ===
            SubjectResource(
                id = 11,
                title = "Organic Chemistry II - Benzene, Phenols & Aromatic Amines",
                subjectName = "Pharmaceutical Organic Chemistry II",
                subjectCode = "BP301T",
                course = CourseType.B_PHARM,
                semester = 3,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 & 2: Electrophilic Aromatic Substitution",
                description = "Huckel's rule, orientation in disubstituted benzenes, acidity of phenols, basicity of amines, fats and oils analytical values.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem3_org2.pdf",
                fileSize = "6.9 MB",
                pageCount = 70,
                downloadsCount = 1310,
                isFeatured = true
            ),
            SubjectResource(
                id = 12,
                title = "Physical Pharmaceutics I - Solubility & Distribution Notes",
                subjectName = "Physical Pharmaceutics I",
                subjectCode = "BP302T",
                course = CourseType.B_PHARM,
                semester = 3,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 & 2: States of Matter & Surface Tension",
                description = "Ideal and real solutions, Raoult's law, adsorption isotherms, spreading coefficient, surfactants and HLB scale.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem3_pp1.pdf",
                fileSize = "4.5 MB",
                pageCount = 50,
                downloadsCount = 780,
                isFeatured = false
            ),
            SubjectResource(
                id = 13,
                title = "Pharmaceutical Microbiology - Sterilization & Culture Techniques",
                subjectName = "Pharmaceutical Microbiology",
                subjectCode = "BP303T",
                course = CourseType.B_PHARM,
                semester = 3,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 to 3: Gram Staining, Autoclaving & Media",
                description = "Morphological classification of bacteria, bacterial growth curve, aseptic area design, disinfectant evaluation and sterility testing.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem3_micro.pdf",
                fileSize = "5.5 MB",
                pageCount = 60,
                downloadsCount = 1050,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 4 ===
            SubjectResource(
                id = 14,
                title = "Medicinal Chemistry I - SAR & Drug Synthesis Formulas",
                subjectName = "Medicinal Chemistry I",
                subjectCode = "BP402T",
                course = CourseType.B_PHARM,
                semester = 4,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Structure Activity Relationships & Synthesis",
                description = "Complete SAR diagrams of sympathomimetics, parasympathomimetics, sedatives, hypnotics, anticonvulsants and NSAIDs.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem4_medchem1.pdf",
                fileSize = "7.8 MB",
                pageCount = 78,
                downloadsCount = 1620,
                isFeatured = true
            ),
            SubjectResource(
                id = 15,
                title = "Pharmacology I - General Pharmacology & ANS Drug Classifications",
                subjectName = "Pharmacology I",
                subjectCode = "BP404T",
                course = CourseType.B_PHARM,
                semester = 4,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 & 2: Pharmacokinetics & Receptors",
                description = "Absorption, distribution, CYP450 metabolism, elimination kinetics, G-protein coupled receptors, adrenergic & cholinergic agonists.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem4_col1.pdf",
                fileSize = "6.2 MB",
                pageCount = 66,
                downloadsCount = 1480,
                isFeatured = true
            ),
            SubjectResource(
                id = 16,
                title = "Pharmacognosy & Phytochemistry I - Extraction & Plant Constituents",
                subjectName = "Pharmacognosy and Phytochemistry I",
                subjectCode = "BP405T",
                course = CourseType.B_PHARM,
                semester = 4,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Alkaloids, Glycosides, Tannins & Volatile Oils",
                description = "Cultivation and collection, adulteration detection, Soxhlet extraction procedure and phytochemical screening assays.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem4_cog1.pdf",
                fileSize = "4.9 MB",
                pageCount = 54,
                downloadsCount = 760,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 5 ===
            SubjectResource(
                id = 17,
                title = "Medicinal Chemistry II - Antihistamines, Antineoplastic & Cardiovascular Drugs",
                subjectName = "Medicinal Chemistry II",
                subjectCode = "BP501T",
                course = CourseType.B_PHARM,
                semester = 5,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 to 5: H1/H2 Blockers, Alkylating Agents & Antiarrhythmics",
                description = "Chemical structures, synthesis of Methotrexate, Chlorpheniramine, Procainamide, Nitroglycerin and SAR of dihydropyridines.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem5_medchem2.pdf",
                fileSize = "6.5 MB",
                pageCount = 72,
                downloadsCount = 1290,
                isFeatured = true
            ),
            SubjectResource(
                id = 18,
                title = "Industrial Pharmacy I - Tablets, Capsules & Parenterals Formulation",
                subjectName = "Industrial Pharmacy I",
                subjectCode = "BP502T",
                course = CourseType.B_PHARM,
                semester = 5,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1, 2 & 3: Tablet Defects, Granulation & Hard Gelatin",
                description = "Compression defects like capping and lamination, tablet tooling, enteric coating polymers, preformulation and sterile production area.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem5_ip1.pdf",
                fileSize = "5.7 MB",
                pageCount = 64,
                downloadsCount = 1180,
                isFeatured = false
            ),
            SubjectResource(
                id = 19,
                title = "Pharmacology II - CVS & Renal Pharmacology Exam Notes",
                subjectName = "Pharmacology II",
                subjectCode = "BP503T",
                course = CourseType.B_PHARM,
                semester = 5,
                category = ResourceCategory.IMPORTANT_QUESTIONS,
                unitOrTopic = "Antihypertensives, Antianginals & Diuretics",
                description = "Mechanism of action, therapeutic uses and adverse effects of ACE inhibitors, Beta blockers, Loop diuretics, and Cardiac glycosides.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem5_col2.pdf",
                fileSize = "4.6 MB",
                pageCount = 50,
                downloadsCount = 990,
                isFeatured = false
            ),
            SubjectResource(
                id = 20,
                title = "Pharmaceutical Jurisprudence - Pharmacy Act, D&C Act & Schedules",
                subjectName = "Pharmaceutical Jurisprudence",
                subjectCode = "BP505T",
                course = CourseType.B_PHARM,
                semester = 5,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "PCI Constitution, Schedule H, X, M, Y & Narcotic Drugs Act",
                description = "Complete guide to legal obligations, registration of pharmacists, prohibited advertisements and loan licensing regulations.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem5_juris.pdf",
                fileSize = "4.3 MB",
                pageCount = 48,
                downloadsCount = 1120,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 6 ===
            SubjectResource(
                id = 21,
                title = "Medicinal Chemistry III - Beta Lactam, Macrolide & Fluoroquinolone Antibiotics",
                subjectName = "Medicinal Chemistry III",
                subjectCode = "BP601T",
                course = CourseType.B_PHARM,
                semester = 6,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Antibiotics, Antifungals & Antivirals SAR",
                description = "Penicillin core structure degradation, cephalosporin generations, tetracyclines epimerization, chloramphenicol synthesis.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem6_medchem3.pdf",
                fileSize = "7.3 MB",
                pageCount = 76,
                downloadsCount = 1350,
                isFeatured = true
            ),
            SubjectResource(
                id = 22,
                title = "Biopharmaceutics & Pharmacokinetics - Compartment Models & Clearance",
                subjectName = "Biopharmaceutics and Pharmacokinetics",
                subjectCode = "BP604T",
                course = CourseType.B_PHARM,
                semester = 6,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "One Compartment Open Model & Bioequivalence",
                description = "Derivations for IV bolus, elimination rate constant, AUC determination by trapezoidal rule, in vitro in vivo correlation (IVIVC).",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem6_biopharm.pdf",
                fileSize = "5.6 MB",
                pageCount = 60,
                downloadsCount = 1040,
                isFeatured = false
            ),

            // === B.PHARM SEMESTER 7 ===
            SubjectResource(
                id = 23,
                title = "Instrumental Methods of Analysis - UV-Vis, IR, NMR & HPLC Notes",
                subjectName = "Instrumental Methods of Analysis",
                subjectCode = "BP701T",
                course = CourseType.B_PHARM,
                semester = 7,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Unit 1 to 4: Spectroscopy & Chromatography",
                description = "Beer-Lambert's law deviations, chromophores, Woodward-Fieser rules, vibrational modes in IR, shielding in NMR, and reverse-phase HPLC columns.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem7_instrumental.pdf",
                fileSize = "8.1 MB",
                pageCount = 82,
                downloadsCount = 1710,
                isFeatured = true
            ),
            SubjectResource(
                id = 24,
                title = "Novel Drug Delivery Systems (NDDS) - Liposomes, Niosomes & Transdermal Patches",
                subjectName = "Novel Drug Delivery Systems",
                subjectCode = "BP704T",
                course = CourseType.B_PHARM,
                semester = 7,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Targeted Drug Delivery & Controlled Release",
                description = "Preparation methods of liposomes, osmotically controlled oral delivery systems, polymers used in matrix patches and ocular inserts.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem7_ndds.pdf",
                fileSize = "6.1 MB",
                pageCount = 68,
                downloadsCount = 1430,
                isFeatured = true
            ),

            // === B.PHARM SEMESTER 8 ===
            SubjectResource(
                id = 25,
                title = "Biostatistics & Research Methodology - Regression, ANOVA & Hypothesis Testing",
                subjectName = "Biostatistics and Research Methodology",
                subjectCode = "BP801T",
                course = CourseType.B_PHARM,
                semester = 8,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Parametric and Non-Parametric Tests",
                description = "Student's t-test, Chi-square test, One-way ANOVA, research protocol design, sample size calculation and clinical trial phases.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/bpharm_sem8_biostat.pdf",
                fileSize = "4.7 MB",
                pageCount = 52,
                downloadsCount = 890,
                isFeatured = false
            ),

            // === D.PHARM 1ST YEAR ===
            SubjectResource(
                id = 26,
                title = "D.Pharm 1st Year Pharmaceutics ER-20 Complete Guide",
                subjectName = "Pharmaceutics (Theory & Lab)",
                subjectCode = "ER20-11T",
                course = CourseType.D_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Packaging, Size Reduction, Mixing & Filtration",
                description = "Simplified notes strictly aligned with Pharmacy Council of India ER-20 syllabus for diploma students. Includes formulas and short definitions.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/dpharm1_pharmaceutics.pdf",
                fileSize = "5.9 MB",
                pageCount = 64,
                downloadsCount = 1530,
                isFeatured = true
            ),
            SubjectResource(
                id = 27,
                title = "D.Pharm 1st Year Pharmaceutical Chemistry Notes & Structures",
                subjectName = "Pharmaceutical Chemistry",
                subjectCode = "ER20-12T",
                course = CourseType.D_PHARM,
                semester = 1,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Sources of Impurities, Medicinal Compounds",
                description = "Chemical nomenclature, chemical structure formulas, brand names, storage conditions and therapeutic uses of major drugs.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/dpharm1_chem.pdf",
                fileSize = "6.3 MB",
                pageCount = 70,
                downloadsCount = 1390,
                isFeatured = true
            ),
            SubjectResource(
                id = 28,
                title = "D.Pharm 1st Year Previous Year Model Question Papers",
                subjectName = "All D.Pharm 1st Year Subjects",
                subjectCode = "ER20-Y1-PYQ",
                course = CourseType.D_PHARM,
                semester = 1,
                category = ResourceCategory.QUESTION_PAPER,
                unitOrTopic = "Annual Board Examination Solved Papers",
                description = "Board of Technical Education question papers with solved 3-mark and 5-mark model answers.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/dpharm1_pyq.pdf",
                fileSize = "7.5 MB",
                pageCount = 78,
                downloadsCount = 2100,
                isFeatured = true
            ),

            // === D.PHARM 2ND YEAR ===
            SubjectResource(
                id = 29,
                title = "D.Pharm 2nd Year Pharmacology - Drug Classification & Dose Guide",
                subjectName = "Pharmacology",
                subjectCode = "ER20-21T",
                course = CourseType.D_PHARM,
                semester = 2,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Complete Drug Classification Charts",
                description = "High-yield memorization tables for antihypertensives, antibiotics, analgesics, antidiabetics, route of administration and side effects.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/dpharm2_col.pdf",
                fileSize = "5.8 MB",
                pageCount = 62,
                downloadsCount = 1760,
                isFeatured = true
            ),
            SubjectResource(
                id = 30,
                title = "Pharmacy Law and Ethics (Jurisprudence) D.Pharm 2nd Year Notes",
                subjectName = "Pharmacy Law & Ethics",
                subjectCode = "ER20-26T",
                course = CourseType.D_PHARM,
                semester = 2,
                category = ResourceCategory.HANDWRITTEN_NOTES,
                unitOrTopic = "Code of Ethics, D&C Act, Poison Act & MTP Act",
                description = "Pharmacist oath, code of ethics towards patients and physicians, retail and wholesale drug store licensing regulations.",
                fileUrl = "https://raw.githubusercontent.com/example/pharmacy-notes/main/dpharm2_law.pdf",
                fileSize = "4.4 MB",
                pageCount = 48,
                downloadsCount = 1240,
                isFeatured = false
            )
        )
    }
}
