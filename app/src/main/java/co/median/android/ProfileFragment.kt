package co.median.android

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

data class SkillEntry(
    val skill: String,
    val level: String
)

class ProfileFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var profileAvatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileHeadline: TextView
    private lateinit var profileLocation: TextView
    private lateinit var openToWorkStatusText: TextView
    private lateinit var openToWorkDescText: TextView
    private lateinit var openToWorkSwitch: SwitchMaterial
    private lateinit var editProfileMainBtn: Button

    private lateinit var completionPercentText: TextView
    private lateinit var completionProgressBar: ProgressBar
    private lateinit var completionAdviceText: TextView

    private lateinit var aboutContentText: TextView
    private lateinit var editAboutBtn: TextView

    private lateinit var experienceContainer: LinearLayout
    private lateinit var addExperienceBtn: TextView

    private lateinit var educationContainer: LinearLayout
    private lateinit var addEducationBtn: TextView

    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var emptySkillsText: TextView
    private lateinit var addSkillBtn: TextView

    private lateinit var resumeUploadedContainer: View
    private lateinit var resumeEmptyContainer: View
    private lateinit var resumeFileNameText: TextView
    private lateinit var resumeUpdatedText: TextView
    private lateinit var previewResumeBtn: Button
    private lateinit var downloadResumeBtn: Button
    private lateinit var replaceResumeBtn: Button
    private lateinit var deleteResumeBtn: Button
    private lateinit var uploadResumeBtn: Button

    private lateinit var certificationsContainer: LinearLayout
    private lateinit var addCertificationBtn: TextView

    private lateinit var projectsContainer: LinearLayout
    private lateinit var addProjectBtn: TextView

    private lateinit var profileLogoutBtn: Button

    private lateinit var currentProfile: UserProfile

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                uploadProfilePhoto(uri)
            }
        }
    }

    private val resumePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                uploadResume(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        swipeRefresh = view.findViewById(R.id.profileSwipeRefresh)
        profileAvatar = view.findViewById(R.id.profileAvatar)
        profileName = view.findViewById(R.id.profileName)
        profileHeadline = view.findViewById(R.id.profileHeadline)
        profileLocation = view.findViewById(R.id.profileLocation)
        openToWorkStatusText = view.findViewById(R.id.openToWorkStatusText)
        openToWorkDescText = view.findViewById(R.id.openToWorkDescText)
        openToWorkSwitch = view.findViewById(R.id.openToWorkSwitch)
        editProfileMainBtn = view.findViewById(R.id.editProfileMainBtn)

        completionPercentText = view.findViewById(R.id.completionPercentText)
        completionProgressBar = view.findViewById(R.id.completionProgressBar)
        completionAdviceText = view.findViewById(R.id.completionAdviceText)

        aboutContentText = view.findViewById(R.id.aboutContentText)
        editAboutBtn = view.findViewById(R.id.editAboutBtn)

        experienceContainer = view.findViewById(R.id.experienceContainer)
        addExperienceBtn = view.findViewById(R.id.addExperienceBtn)

        educationContainer = view.findViewById(R.id.educationContainer)
        addEducationBtn = view.findViewById(R.id.addEducationBtn)

        skillsChipGroup = view.findViewById(R.id.skillsChipGroup)
        emptySkillsText = view.findViewById(R.id.emptySkillsText)
        addSkillBtn = view.findViewById(R.id.addSkillBtn)

        resumeUploadedContainer = view.findViewById(R.id.resumeUploadedContainer)
        resumeEmptyContainer = view.findViewById(R.id.resumeEmptyContainer)
        resumeFileNameText = view.findViewById(R.id.resumeFileNameText)
        resumeUpdatedText = view.findViewById(R.id.resumeUpdatedText)
        previewResumeBtn = view.findViewById(R.id.previewResumeBtn)
        downloadResumeBtn = view.findViewById(R.id.downloadResumeBtn)
        replaceResumeBtn = view.findViewById(R.id.replaceResumeBtn)
        deleteResumeBtn = view.findViewById(R.id.deleteResumeBtn)
        uploadResumeBtn = view.findViewById(R.id.uploadResumeBtn)

        certificationsContainer = view.findViewById(R.id.certificationsContainer)
        addCertificationBtn = view.findViewById(R.id.addCertificationBtn)

        projectsContainer = view.findViewById(R.id.projectsContainer)
        addProjectBtn = view.findViewById(R.id.addProjectBtn)

        profileLogoutBtn = view.findViewById(R.id.profileLogoutBtn)

        swipeRefresh.setOnRefreshListener {
            loadProfileData()
            swipeRefresh.isRefreshing = false
        }

        val launchEditProfile = View.OnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        editProfileMainBtn.setOnClickListener(launchEditProfile)
        editAboutBtn.setOnClickListener(launchEditProfile)

        profileAvatar.setOnClickListener {
            showPhotoActionDialog()
        }

        addExperienceBtn.setOnClickListener {
            ExperienceDialog(requireContext()) {
                loadProfileData()
            }.show()
        }

        addEducationBtn.setOnClickListener {
            EducationDialog(requireContext()) {
                loadProfileData()
            }.show()
        }

        addSkillBtn.setOnClickListener {
            showAddSkillDialog()
        }

        addCertificationBtn.setOnClickListener {
            CertificationDialog(requireContext()) {
                loadProfileData()
            }.show()
        }

        addProjectBtn.setOnClickListener {
            ProjectDialog(requireContext()) {
                loadProfileData()
            }.show()
        }

        openToWorkSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentProfile = currentProfile.copy(openToWork = isChecked)
            FirebaseProfileRepository.saveProfileToFirebase(requireContext(), currentProfile) { _, _ ->
                updateOpenToWorkUI(isChecked)
            }
        }

        val pickResume = View.OnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
            resumePickerLauncher.launch(intent)
        }

        previewResumeBtn.setOnClickListener { viewResume() }
        downloadResumeBtn.setOnClickListener {
            if (currentProfile.resumeFileName.isBlank()) {
                Toast.makeText(requireContext(), "No resume uploaded", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Downloading ${currentProfile.resumeFileName}...", Toast.LENGTH_SHORT).show()
            }
        }
        replaceResumeBtn.setOnClickListener(pickResume)
        uploadResumeBtn.setOnClickListener(pickResume)
        deleteResumeBtn.setOnClickListener { showDeleteResumeDialog() }

        profileLogoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        loadProfileData()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        currentProfile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        val user = FirebaseAuth.getInstance().currentUser
        if (currentProfile.name.isBlank() && user != null) {
            currentProfile = currentProfile.copy(
                name = user.displayName ?: "Divyanshu Singh",
                email = user.email ?: "sdivyanshu352@gmail.com",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            FirebaseProfileRepository.saveProfileLocally(requireContext(), currentProfile)
        }
        bindProfileData(currentProfile)
    }

    private fun bindProfileData(profile: UserProfile) {
        profileName.text = profile.name.ifBlank { "Divyanshu Singh" }
        profileHeadline.text = profile.headline.ifBlank { profile.jobRole.ifBlank { "Software Engineer | React | Node.js" } }
        profileLocation.text = profile.location.ifBlank { "Prayagraj, Uttar Pradesh" }
        
        openToWorkSwitch.setOnCheckedChangeListener(null)
        openToWorkSwitch.isChecked = profile.openToWork
        updateOpenToWorkUI(profile.openToWork)
        openToWorkSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentProfile = currentProfile.copy(openToWork = isChecked)
            FirebaseProfileRepository.saveProfileToFirebase(requireContext(), currentProfile) { _, _ ->
                updateOpenToWorkUI(isChecked)
            }
        }

        val completion = profile.calculateCompletionPercentage()
        completionPercentText.text = "$completion%"
        completionProgressBar.progress = completion

        if (completion >= 80) {
            completionAdviceText.text = "Your profile is stellar! You are ready for top tech job matches."
        } else {
            completionAdviceText.text = "You're almost there. Complete your profile to improve personalized job matching."
        }

        aboutContentText.text = profile.about.ifBlank { profile.bio.ifBlank { "Passionate software engineer with experience building full-stack applications using React, Node.js, and Android." } }
        
        if (profile.resumeFileName.isNotBlank()) {
            resumeUploadedContainer.visibility = View.VISIBLE
            resumeEmptyContainer.visibility = View.GONE
            resumeFileNameText.text = profile.resumeFileName
            resumeUpdatedText.text = if (profile.resumeUpdatedAt.isNotBlank()) "Updated ${profile.resumeUpdatedAt}" else "Updated Today"
        } else {
            resumeUploadedContainer.visibility = View.GONE
            resumeEmptyContainer.visibility = View.VISIBLE
        }

        renderExperienceTable(profile)
        renderEducationTable(profile)
        renderSkillsChips(profile)
        renderCertificationsTable(profile)
        renderProjectsTable(profile)
    }

    private fun updateOpenToWorkUI(isOpen: Boolean) {
        if (isOpen) {
            openToWorkStatusText.text = "Open to Work"
            openToWorkDescText.text = "Available for new opportunities"
        } else {
            openToWorkStatusText.text = "Not Open to Work"
            openToWorkDescText.text = "Currently not looking for new opportunities"
        }
    }

    // --- Experience Table ---
    private fun renderExperienceTable(profile: UserProfile) {
        experienceContainer.removeAllViews()
        try {
            val jsonArray = if (profile.experience.isNotBlank() && profile.experience != "[]") {
                JSONArray(profile.experience)
            } else {
                JSONArray()
            }

            if (jsonArray.length() == 0) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No experience added yet."
                    textSize = 13f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                experienceContainer.addView(emptyTv)
                return
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val company = obj.optString("company", "")
                val title = obj.optString("title", "")
                val start = obj.optString("startDate", "")
                val end = obj.optString("endDate", "")

                val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, experienceContainer, false)
                val titleView = rowView.findViewById<TextView>(R.id.recordTitle)
                val subtitleView = rowView.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = rowView.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = rowView.findViewById<TextView>(R.id.recordDeleteBtn)

                titleView.text = "$title at $company"
                subtitleView.text = if (start.isNotBlank() || end.isNotBlank()) "$start - ${end.ifBlank { "Present" }}" else "Present"

                editBtn.setOnClickListener {
                    showEditExperienceInline(i, company, title, start, end)
                }

                deleteBtn.setOnClickListener {
                    jsonArray.remove(i)
                    val updated = profile.copy(experience = jsonArray.toString())
                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                        loadProfileData()
                    }
                }

                experienceContainer.addView(rowView)
            }
        } catch (_: Exception) {}
    }

    private fun showEditExperienceInline(index: Int, comp: String, role: String, start: String, end: String) {
        experienceContainer.removeViewAt(index)

        val editRowView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.editbox_background_normal)
        }

        val companyEdit = EditText(requireContext()).apply { hint = "Company Name"; setText(comp); textSize = 14f }
        val roleEdit = EditText(requireContext()).apply { hint = "Job Title / Role"; setText(role); textSize = 14f }
        val startEdit = EditText(requireContext()).apply { hint = "Start Date (e.g. Nov 2024)"; setText(start); textSize = 14f }
        val endEdit = EditText(requireContext()).apply { hint = "End Date (e.g. Present)"; setText(end); textSize = 14f }

        val btnLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val saveBtn = Button(requireContext()).apply { text = "Save"; textSize = 12f }
        val cancelBtn = Button(requireContext()).apply { text = "Cancel"; textSize = 12f }

        btnLayout.addView(saveBtn)
        btnLayout.addView(cancelBtn)

        editRowView.addView(companyEdit)
        editRowView.addView(roleEdit)
        editRowView.addView(startEdit)
        editRowView.addView(endEdit)
        editRowView.addView(btnLayout)

        experienceContainer.addView(editRowView, index)

        cancelBtn.setOnClickListener { loadProfileData() }
        saveBtn.setOnClickListener {
            val newComp = companyEdit.text.toString().trim()
            val newRole = roleEdit.text.toString().trim()
            if (newComp.isEmpty() || newRole.isEmpty()) {
                companyEdit.error = "Required"
                return@setOnClickListener
            }

            try {
                val jsonArray = JSONArray(currentProfile.experience)
                val updatedObj = JSONObject().apply {
                    put("id", "exp_" + System.currentTimeMillis())
                    put("company", newComp)
                    put("title", newRole)
                    put("startDate", startEdit.text.toString().trim())
                    put("endDate", endEdit.text.toString().trim())
                }
                jsonArray.put(index, updatedObj)
                val updated = currentProfile.copy(experience = jsonArray.toString())
                FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                    loadProfileData()
                }
            } catch (_: Exception) {}
        }
    }

    // --- Education Table ---
    private fun renderEducationTable(profile: UserProfile) {
        educationContainer.removeAllViews()
        try {
            val jsonArray = if (profile.education.isNotBlank() && profile.education != "[]") {
                JSONArray(profile.education)
            } else {
                JSONArray()
            }

            if (jsonArray.length() == 0) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No education added yet."
                    textSize = 13f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                educationContainer.addView(emptyTv)
                return
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val degree = obj.optString("degree", "")
                val inst = obj.optString("institution", "")
                val startYr = obj.optString("startYear", "")
                val endYr = obj.optString("endYear", "")
                val grade = obj.optString("grade", "")

                val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, educationContainer, false)
                val titleView = rowView.findViewById<TextView>(R.id.recordTitle)
                val subtitleView = rowView.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = rowView.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = rowView.findViewById<TextView>(R.id.recordDeleteBtn)

                titleView.text = "$degree at $inst"
                val yearRange = if (startYr.isNotBlank() || endYr.isNotBlank()) "$startYr${if (endYr.isNotBlank()) " - $endYr" else ""}" else ""
                val subtitleText = buildString {
                    if (yearRange.isNotBlank()) append(yearRange)
                    if (grade.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("Grade: $grade")
                    }
                }
                subtitleView.text = subtitleText.ifBlank { "Education Record" }

                editBtn.setOnClickListener {
                    showEditEducationInline(i, degree, inst, startYr, grade)
                }

                deleteBtn.setOnClickListener {
                    jsonArray.remove(i)
                    val updated = profile.copy(education = jsonArray.toString())
                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                        loadProfileData()
                    }
                }

                educationContainer.addView(rowView)
            }
        } catch (_: Exception) {}
    }

    private fun showEditEducationInline(index: Int, deg: String, inst: String, yr: String, grd: String) {
        educationContainer.removeViewAt(index)

        val editRowView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.editbox_background_normal)
        }

        val degreeEdit = EditText(requireContext()).apply { hint = "Degree (e.g. B.Tech CSE)"; setText(deg); textSize = 14f }
        val instEdit = EditText(requireContext()).apply { hint = "Institution"; setText(inst); textSize = 14f }
        val yearEdit = EditText(requireContext()).apply { hint = "Year (e.g. 2024)"; setText(yr); textSize = 14f }
        val gradeEdit = EditText(requireContext()).apply { hint = "Grade (e.g. 8.36)"; setText(grd); textSize = 14f }

        val btnLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val saveBtn = Button(requireContext()).apply { text = "Save"; textSize = 12f }
        val cancelBtn = Button(requireContext()).apply { text = "Cancel"; textSize = 12f }

        btnLayout.addView(saveBtn)
        btnLayout.addView(cancelBtn)

        editRowView.addView(degreeEdit)
        editRowView.addView(instEdit)
        editRowView.addView(yearEdit)
        editRowView.addView(gradeEdit)
        editRowView.addView(btnLayout)

        educationContainer.addView(editRowView, index)

        cancelBtn.setOnClickListener { loadProfileData() }
        saveBtn.setOnClickListener {
            val newDeg = degreeEdit.text.toString().trim()
            val newInst = instEdit.text.toString().trim()
            if (newDeg.isEmpty() || newInst.isEmpty()) {
                degreeEdit.error = "Required"
                return@setOnClickListener
            }

            try {
                val jsonArray = JSONArray(currentProfile.education)
                val updatedObj = JSONObject().apply {
                    put("id", "edu_" + System.currentTimeMillis())
                    put("degree", newDeg)
                    put("institution", newInst)
                    put("startYear", yearEdit.text.toString().trim())
                    put("grade", gradeEdit.text.toString().trim())
                }
                jsonArray.put(index, updatedObj)
                val updated = currentProfile.copy(education = jsonArray.toString())
                FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                    loadProfileData()
                }
            } catch (_: Exception) {}
        }
    }

    // --- Skills Chips ---
    private fun parseSkillsList(skillsRaw: String): MutableList<SkillEntry> {
        val list = mutableListOf<SkillEntry>()
        if (skillsRaw.isBlank()) return list
        val trimmed = skillsRaw.trim()
        if (trimmed.startsWith("[")) {
            try {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.get(i)
                    if (item is JSONObject) {
                        val name = item.optString("skill", "").ifBlank { item.optString("name", "") }
                        val level = item.optString("level", "Intermediate")
                        if (name.isNotBlank()) list.add(SkillEntry(SkillLibrary.normalize(name), level))
                    } else if (item is String) {
                        if (item.isNotBlank()) list.add(SkillEntry(SkillLibrary.normalize(item), "Intermediate"))
                    }
                }
            } catch (_: Exception) {
                trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                    list.add(SkillEntry(SkillLibrary.normalize(it), "Intermediate"))
                }
            }
        } else {
            trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                list.add(SkillEntry(SkillLibrary.normalize(it), "Intermediate"))
            }
        }
        return list
    }

    private fun serializeSkillsList(list: List<SkillEntry>): String {
        val array = JSONArray()
        for (entry in list) {
            val obj = JSONObject().apply {
                put("skill", entry.skill)
                put("level", entry.level)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun renderSkillsChips(profile: UserProfile) {
        skillsChipGroup.removeAllViews()
        val skillsList = parseSkillsList(profile.skills)

        if (skillsList.isEmpty()) {
            emptySkillsText.visibility = View.VISIBLE
            skillsChipGroup.visibility = View.GONE
            return
        }

        emptySkillsText.visibility = View.GONE
        skillsChipGroup.visibility = View.VISIBLE

        for (entry in skillsList) {
            val chip = Chip(requireContext()).apply {
                text = entry.skill
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    skillsList.remove(entry)
                    val updatedProfile = profile.copy(skills = serializeSkillsList(skillsList))
                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updatedProfile) { _, _ ->
                        loadProfileData()
                    }
                }
            }
            skillsChipGroup.addView(chip)
        }
    }

    private fun showAddSkillDialog() {
        val editContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val titleTv = TextView(requireContext()).apply {
            text = "Add Skill"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.colorPrimary, null))
            setPadding(0, 0, 0, 16)
        }

        val skillAutoComplete = AutoCompleteTextView(requireContext()).apply {
            hint = "Search or enter skill (e.g. React, Kotlin)"
            textSize = 14f
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, SkillLibrary.allSkills)
            setAdapter(adapter)
            threshold = 1
        }

        val levelSpinner = Spinner(requireContext()).apply {
            val levels = arrayOf("Beginner", "Intermediate", "Advanced")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, levels)
            setSelection(1)
        }

        editContainer.addView(titleTv)
        editContainer.addView(skillAutoComplete)
        editContainer.addView(levelSpinner)

        AlertDialog.Builder(requireContext())
            .setView(editContainer)
            .setPositiveButton("Add") { dialog, _ ->
                val sk = skillAutoComplete.text.toString().trim()
                if (sk.isNotBlank()) {
                    val normalized = SkillLibrary.normalize(sk)
                    val selectedLevel = levelSpinner.selectedItem.toString()
                    val skillsList = parseSkillsList(currentProfile.skills)

                    if (skillsList.none { it.skill.equals(normalized, ignoreCase = true) }) {
                        skillsList.add(SkillEntry(normalized, selectedLevel))
                        val updatedProfile = currentProfile.copy(skills = serializeSkillsList(skillsList))
                        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updatedProfile) { _, _ ->
                            loadProfileData()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Skill already added", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Certifications Table ---
    private fun renderCertificationsTable(profile: UserProfile) {
        certificationsContainer.removeAllViews()
        try {
            val jsonArray = if (profile.certifications.isNotBlank() && profile.certifications != "[]") {
                JSONArray(profile.certifications)
            } else {
                JSONArray()
            }

            if (jsonArray.length() == 0) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No certifications added yet."
                    textSize = 13f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                certificationsContainer.addView(emptyTv)
                return
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val org = obj.optString("organization", "")
                val date = obj.optString("issueDate", "")

                val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, certificationsContainer, false)
                val titleView = rowView.findViewById<TextView>(R.id.recordTitle)
                val subtitleView = rowView.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = rowView.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = rowView.findViewById<TextView>(R.id.recordDeleteBtn)

                titleView.text = title
                subtitleView.text = if (date.isNotBlank()) "$org • Issued $date" else org

                editBtn.setOnClickListener {
                    showEditCertificationInline(i, title, org, date)
                }

                deleteBtn.setOnClickListener {
                    jsonArray.remove(i)
                    val updated = profile.copy(certifications = jsonArray.toString())
                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                        loadProfileData()
                    }
                }

                certificationsContainer.addView(rowView)
            }
        } catch (_: Exception) {}
    }

    private fun showEditCertificationInline(index: Int, title: String, org: String, date: String) {
        certificationsContainer.removeViewAt(index)

        val editRowView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.editbox_background_normal)
        }

        val titleEdit = EditText(requireContext()).apply { hint = "Certification Title"; setText(title); textSize = 14f }
        val orgEdit = EditText(requireContext()).apply { hint = "Issuing Organization"; setText(org); textSize = 14f }
        val dateEdit = EditText(requireContext()).apply { hint = "Issue Date (e.g. Aug 2024)"; setText(date); textSize = 14f }

        val btnLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val saveBtn = Button(requireContext()).apply { text = "Save"; textSize = 12f }
        val cancelBtn = Button(requireContext()).apply { text = "Cancel"; textSize = 12f }

        btnLayout.addView(saveBtn)
        btnLayout.addView(cancelBtn)

        editRowView.addView(titleEdit)
        editRowView.addView(orgEdit)
        editRowView.addView(dateEdit)
        editRowView.addView(btnLayout)

        certificationsContainer.addView(editRowView, index)

        cancelBtn.setOnClickListener { loadProfileData() }
        saveBtn.setOnClickListener {
            val newTitle = titleEdit.text.toString().trim()
            val newOrg = orgEdit.text.toString().trim()
            if (newTitle.isEmpty() || newOrg.isEmpty()) {
                titleEdit.error = "Required"
                return@setOnClickListener
            }

            try {
                val jsonArray = JSONArray(currentProfile.certifications)
                val updatedObj = JSONObject().apply {
                    put("id", "cert_" + System.currentTimeMillis())
                    put("title", newTitle)
                    put("organization", newOrg)
                    put("issueDate", dateEdit.text.toString().trim())
                }
                jsonArray.put(index, updatedObj)
                val updated = currentProfile.copy(certifications = jsonArray.toString())
                FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                    loadProfileData()
                }
            } catch (_: Exception) {}
        }
    }

    // --- Projects Table ---
    private fun renderProjectsTable(profile: UserProfile) {
        projectsContainer.removeAllViews()
        try {
            val jsonArray = if (profile.projects.isNotBlank() && profile.projects != "[]") {
                JSONArray(profile.projects)
            } else {
                JSONArray()
            }

            if (jsonArray.length() == 0) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No projects added yet."
                    textSize = 13f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                projectsContainer.addView(emptyTv)
                return
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val role = obj.optString("role", "")
                val desc = obj.optString("description", "")

                val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, projectsContainer, false)
                val titleView = rowView.findViewById<TextView>(R.id.recordTitle)
                val subtitleView = rowView.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = rowView.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = rowView.findViewById<TextView>(R.id.recordDeleteBtn)

                titleView.text = title
                subtitleView.text = if (role.isNotBlank()) "$role • $desc" else desc.ifBlank { "Project Record" }

                editBtn.setOnClickListener {
                    showEditProjectInline(i, title, role, desc)
                }

                deleteBtn.setOnClickListener {
                    jsonArray.remove(i)
                    val updated = profile.copy(projects = jsonArray.toString())
                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                        loadProfileData()
                    }
                }

                projectsContainer.addView(rowView)
            }
        } catch (_: Exception) {}
    }

    private fun showEditProjectInline(index: Int, title: String, role: String, desc: String) {
        projectsContainer.removeViewAt(index)

        val editRowView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.editbox_background_normal)
        }

        val titleEdit = EditText(requireContext()).apply { hint = "Project Title"; setText(title); textSize = 14f }
        val roleEdit = EditText(requireContext()).apply { hint = "Role / Tech Stack"; setText(role); textSize = 14f }
        val descEdit = EditText(requireContext()).apply { hint = "Description"; setText(desc); textSize = 14f }

        val btnLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val saveBtn = Button(requireContext()).apply { text = "Save"; textSize = 12f }
        val cancelBtn = Button(requireContext()).apply { text = "Cancel"; textSize = 12f }

        btnLayout.addView(saveBtn)
        btnLayout.addView(cancelBtn)

        editRowView.addView(titleEdit)
        editRowView.addView(roleEdit)
        editRowView.addView(descEdit)
        editRowView.addView(btnLayout)

        projectsContainer.addView(editRowView, index)

        cancelBtn.setOnClickListener { loadProfileData() }
        saveBtn.setOnClickListener {
            val newTitle = titleEdit.text.toString().trim()
            if (newTitle.isEmpty()) {
                titleEdit.error = "Required"
                return@setOnClickListener
            }

            try {
                val jsonArray = JSONArray(currentProfile.projects)
                val updatedObj = JSONObject().apply {
                    put("id", "proj_" + System.currentTimeMillis())
                    put("title", newTitle)
                    put("role", roleEdit.text.toString().trim())
                    put("description", descEdit.text.toString().trim())
                }
                jsonArray.put(index, updatedObj)
                val updated = currentProfile.copy(projects = jsonArray.toString())
                FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                    loadProfileData()
                }
            } catch (_: Exception) {}
        }
    }

    private fun showPhotoActionDialog() {
        val options = arrayOf("View Photo", "Change Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewPhoto()
                    1 -> {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                        imagePickerLauncher.launch(intent)
                    }
                }
            }
            .show()
    }

    private fun viewPhoto() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setMessage("Viewing profile photo for ${currentProfile.name}")
            .setPositiveButton("Close", null)
            .show()
    }

    private fun uploadProfilePhoto(uri: Uri) {
        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()
        val updated = currentProfile.copy(photoUrl = uri.toString())
        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { success, _ ->
            if (success) {
                currentProfile = updated
                Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show()
                bindProfileData(updated)
            } else {
                Toast.makeText(requireContext(), "Couldn't update profile photo. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun viewResume() {
        if (currentProfile.resumeFileName.isBlank()) {
            Toast.makeText(requireContext(), "No resume uploaded", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(), "Opening resume: ${currentProfile.resumeFileName}", Toast.LENGTH_SHORT).show()
    }

    private fun uploadResume(uri: Uri) {
        Toast.makeText(requireContext(), "Uploading resume...", Toast.LENGTH_SHORT).show()
        val fileName = uri.lastPathSegment ?: "Resume.pdf"
        val updated = currentProfile.copy(resumeFileName = fileName, resumeUpdatedAt = "Today")
        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { success, _ ->
            if (success) {
                currentProfile = updated
                Toast.makeText(requireContext(), "Resume updated successfully", Toast.LENGTH_SHORT).show()
                bindProfileData(updated)
            } else {
                Toast.makeText(requireContext(), "Couldn't update resume. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteResumeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Resume?")
            .setMessage("Your uploaded resume will be removed from your profile.")
            .setPositiveButton("Delete") { _, _ ->
                val updated = currentProfile.copy(resumeFileName = "", resumeUpdatedAt = "")
                FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { success, _ ->
                    if (success) {
                        currentProfile = updated
                        Toast.makeText(requireContext(), "Resume deleted", Toast.LENGTH_SHORT).show()
                        bindProfileData(updated)
                    } else {
                        Toast.makeText(requireContext(), "Couldn't delete your resume. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
