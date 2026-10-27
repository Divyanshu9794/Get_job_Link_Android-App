package co.median.android

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

class ProfileBottomSheetDialog @JvmOverloads constructor(
    private val onProfileSaved: ((UserProfile) -> Unit)? = null,
    private val onDismissAction: (() -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var btnClose: ImageView
    private lateinit var editName: TextInputEditText
    private lateinit var editPhone: TextInputEditText
    private lateinit var editLocation: TextInputEditText
    private lateinit var editHeadline: TextInputEditText
    private lateinit var editAbout: TextInputEditText

    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var btnAddSkill: MaterialButton
    private val skillsList = mutableListOf<String>()

    private lateinit var experienceContainer: LinearLayout
    private lateinit var btnAddExperience: MaterialButton

    private lateinit var educationContainer: LinearLayout
    private lateinit var btnAddEducation: MaterialButton

    private lateinit var certificationsChipGroup: ChipGroup
    private lateinit var btnAddCertification: MaterialButton

    private lateinit var openToWorkSwitch: SwitchMaterial
    private lateinit var progressSaving: LinearProgressIndicator
    private lateinit var btnSave: MaterialButton

    private lateinit var currentProfile: UserProfile

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                val layoutParams = it.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                it.layoutParams = layoutParams
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose = view.findViewById(R.id.btnClose)
        editName = view.findViewById(R.id.edit_name)
        editPhone = view.findViewById(R.id.edit_phone)
        editLocation = view.findViewById(R.id.edit_location)
        editHeadline = view.findViewById(R.id.edit_headline)
        editAbout = view.findViewById(R.id.edit_about)

        skillsChipGroup = view.findViewById(R.id.skillsChipGroup)
        btnAddSkill = view.findViewById(R.id.btnAddSkill)

        experienceContainer = view.findViewById(R.id.experienceContainer)
        btnAddExperience = view.findViewById(R.id.btnAddExperience)

        educationContainer = view.findViewById(R.id.educationContainer)
        btnAddEducation = view.findViewById(R.id.btnAddEducation)

        certificationsChipGroup = view.findViewById(R.id.certificationsChipGroup)
        btnAddCertification = view.findViewById(R.id.btnAddCertification)

        openToWorkSwitch = view.findViewById(R.id.openToWorkSwitch)
        progressSaving = view.findViewById(R.id.progress_saving)
        btnSave = view.findViewById(R.id.btn_save_profile)

        btnClose.setOnClickListener { dismiss() }

        currentProfile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        populateProfileData(currentProfile)

        btnAddSkill.setOnClickListener {
            SkillBottomSheetDialog {
                reloadProfileData()
            }.show(parentFragmentManager, "skill_bottom_sheet")
        }

        btnAddExperience.setOnClickListener {
            ExperienceDialog(requireContext()) {
                reloadProfileData()
            }.show()
        }

        btnAddEducation.setOnClickListener {
            EducationDialog(requireContext()) {
                reloadProfileData()
            }.show()
        }

        btnAddCertification.setOnClickListener {
            CertificationDialog(requireContext()) {
                reloadProfileData()
            }.show()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissAction?.invoke()
    }

    private fun reloadProfileData() {
        if (!isAdded) return
        currentProfile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        populateProfileData(currentProfile)
    }

    private fun populateProfileData(profile: UserProfile) {
        editName.setText(profile.name)
        editPhone.setText(profile.phone)
        editLocation.setText(profile.location)
        editHeadline.setText(profile.headline)
        editAbout.setText(profile.about.ifBlank { profile.bio })
        openToWorkSwitch.isChecked = profile.openToWork

        // Parse skills
        skillsList.clear()
        val raw = profile.skills.trim()
        if (raw.isNotBlank()) {
            if (raw.startsWith("[")) {
                try {
                    val array = JSONArray(raw)
                    for (i in 0 until array.length()) {
                        val item = array.get(i)
                        val name = if (item is JSONObject) item.optString("skill", "").ifBlank { item.optString("name", "") } else item.toString()
                        val normalized = SkillLibrary.normalize(name)
                        if (normalized.isNotBlank() && !skillsList.contains(normalized)) {
                            skillsList.add(normalized)
                        }
                    }
                } catch (_: Exception) {
                    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                        val normalized = SkillLibrary.normalize(it)
                        if (!skillsList.contains(normalized)) skillsList.add(normalized)
                    }
                }
            } else {
                raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                    val normalized = SkillLibrary.normalize(it)
                    if (!skillsList.contains(normalized)) skillsList.add(normalized)
                }
            }
        }
        renderSkillsChips()
        renderCertificationsChips(profile)
        renderExperienceCards(profile)
        renderEducationCards(profile)
    }

    private fun renderSkillsChips() {
        skillsChipGroup.removeAllViews()
        for (skill in skillsList) {
            val chip = Chip(requireContext()).apply {
                text = skill
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    skillsList.remove(skill)
                    renderSkillsChips()
                }
            }
            skillsChipGroup.addView(chip)
        }
    }

    private fun renderCertificationsChips(profile: UserProfile) {
        certificationsChipGroup.removeAllViews()
        try {
            if (profile.certifications.isNotBlank() && profile.certifications != "[]") {
                val jsonArray = JSONArray(profile.certifications)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val org = obj.optString("organization", "")
                    if (title.isNotBlank()) {
                        val chip = Chip(requireContext()).apply {
                            text = if (org.isNotBlank()) "$title ($org)" else title
                            isCloseIconVisible = true
                            setOnClickListener {
                                CertificationDialog(requireContext(), i, obj) {
                                    reloadProfileData()
                                }.show()
                            }
                            setOnCloseIconClickListener {
                                confirmDeleteCertification(i, title)
                            }
                        }
                        certificationsChipGroup.addView(chip)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun confirmDeleteCertification(index: Int, name: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Certification?")
            .setMessage("Are you sure you want to delete '$name'?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    val jsonArray = JSONArray(currentProfile.certifications)
                    if (index >= 0 && index < jsonArray.length()) {
                        jsonArray.remove(index)
                        val updated = currentProfile.copy(certifications = jsonArray.toString())
                        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                            reloadProfileData()
                        }
                    }
                } catch (_: Exception) {}
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderExperienceCards(profile: UserProfile) {
        experienceContainer.removeAllViews()
        try {
            val jsonArray = if (profile.experience.isNotBlank() && profile.experience != "[]") {
                JSONArray(profile.experience)
            } else {
                JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "")
                val company = obj.optString("company", "")
                val start = obj.optString("startDate", "")
                val end = obj.optString("endDate", "Present")

                val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, experienceContainer, false)
                val titleTv = card.findViewById<TextView>(R.id.recordTitle)
                val subTv = card.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = card.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = card.findViewById<TextView>(R.id.recordDeleteBtn)

                titleTv.text = "$title at $company"
                subTv.text = "$start - ${end.ifBlank { "Present" }}"

                editBtn.setOnClickListener {
                    ExperienceDialog(requireContext(), i, obj) {
                        reloadProfileData()
                    }.show()
                }

                deleteBtn.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Experience?")
                        .setMessage("Are you sure you want to delete '$title at $company'?")
                        .setPositiveButton("Delete") { _, _ ->
                            try {
                                val currentArr = JSONArray(currentProfile.experience)
                                if (i >= 0 && i < currentArr.length()) {
                                    currentArr.remove(i)
                                    val updated = currentProfile.copy(experience = currentArr.toString())
                                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                                        reloadProfileData()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                experienceContainer.addView(card)
            }
        } catch (_: Exception) {}
    }

    private fun renderEducationCards(profile: UserProfile) {
        educationContainer.removeAllViews()
        try {
            val jsonArray = if (profile.education.isNotBlank() && profile.education != "[]") {
                JSONArray(profile.education)
            } else {
                JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val degree = obj.optString("degree", "")
                val inst = obj.optString("institution", "")
                val start = obj.optString("startYear", "")
                val end = obj.optString("endYear", "")

                val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_inline_record, educationContainer, false)
                val titleTv = card.findViewById<TextView>(R.id.recordTitle)
                val subTv = card.findViewById<TextView>(R.id.recordSubtitle)
                val editBtn = card.findViewById<TextView>(R.id.recordEditBtn)
                val deleteBtn = card.findViewById<TextView>(R.id.recordDeleteBtn)

                titleTv.text = "$degree - $inst"
                subTv.text = if (start.isNotBlank()) "$start - ${end.ifBlank { "Present" }}" else inst

                editBtn.setOnClickListener {
                    EducationDialog(requireContext(), i, obj) {
                        reloadProfileData()
                    }.show()
                }

                deleteBtn.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Education?")
                        .setMessage("Are you sure you want to delete '$degree at $inst'?")
                        .setPositiveButton("Delete") { _, _ ->
                            try {
                                val currentArr = JSONArray(currentProfile.education)
                                if (i >= 0 && i < currentArr.length()) {
                                    currentArr.remove(i)
                                    val updated = currentProfile.copy(education = currentArr.toString())
                                    FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updated) { _, _ ->
                                        reloadProfileData()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                educationContainer.addView(card)
            }
        } catch (_: Exception) {}
    }

    private fun saveProfile() {
        val name = editName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            editName.error = "Full Name is required"
            editName.requestFocus()
            return
        }

        val skillsArray = JSONArray()
        for (skill in skillsList) {
            skillsArray.put(JSONObject().apply {
                put("skill", skill)
                put("level", "Intermediate")
            })
        }

        val updatedProfile = currentProfile.copy(
            name = name,
            phone = editPhone.text?.toString()?.trim() ?: "",
            location = editLocation.text?.toString()?.trim() ?: "",
            headline = editHeadline.text?.toString()?.trim() ?: "",
            about = editAbout.text?.toString()?.trim() ?: "",
            bio = editAbout.text?.toString()?.trim() ?: "",
            skills = skillsArray.toString(),
            openToWork = openToWorkSwitch.isChecked
        )

        progressSaving.visibility = View.VISIBLE
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updatedProfile) { success, message ->
            if (isAdded) {
                progressSaving.visibility = View.GONE
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"

                if (success) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    onProfileSaved?.invoke(updatedProfile)
                    dismiss()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Error Saving")
                        .setMessage(message.ifBlank { "Could not save profile. Please try again." })
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
}
