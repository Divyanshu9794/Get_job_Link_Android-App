package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONArray
import org.json.JSONObject

class SkillBottomSheetDialog(
    private val onSkillsSaved: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var btnClose: ImageView
    private lateinit var skillAutoComplete: AutoCompleteTextView
    private lateinit var suggestedSkillsChipGroup: ChipGroup
    private lateinit var selectedSkillsChipGroup: ChipGroup
    private lateinit var btnAddSkillAction: MaterialButton

    private val selectedSkills = mutableListOf<String>()
    private lateinit var currentProfile: UserProfile

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_skill, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose = view.findViewById(R.id.btnCloseSkill)
        skillAutoComplete = view.findViewById(R.id.skillSearchAutoComplete)
        suggestedSkillsChipGroup = view.findViewById(R.id.suggestedSkillsChipGroup)
        selectedSkillsChipGroup = view.findViewById(R.id.selectedSkillsChipGroup)
        btnAddSkillAction = view.findViewById(R.id.btnAddSkillAction)

        btnClose.setOnClickListener { dismiss() }

        currentProfile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        loadExistingSkills()
        setupAutoComplete()
        renderSuggestedSkills()
        renderSelectedSkills()

        skillAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position)?.toString() ?: ""
            if (selected.isNotBlank()) {
                addSkill(selected)
                skillAutoComplete.setText("")
            }
        }

        skillAutoComplete.setOnEditorActionListener { _, _, _ ->
            val text = skillAutoComplete.text.toString().trim()
            if (text.isNotBlank()) {
                addSkill(text)
                skillAutoComplete.setText("")
                true
            } else {
                false
            }
        }

        btnAddSkillAction.setOnClickListener {
            val text = skillAutoComplete.text.toString().trim()
            if (text.isNotBlank()) {
                addSkill(text)
                skillAutoComplete.setText("")
            }
            saveSkillsToFirestore()
        }
    }

    private fun loadExistingSkills() {
        selectedSkills.clear()
        val raw = currentProfile.skills
        if (raw.isNotBlank()) {
            try {
                if (raw.startsWith("[")) {
                    val array = JSONArray(raw)
                    for (i in 0 until array.length()) {
                        val item = array.get(i)
                        val name = if (item is JSONObject) item.optString("skill", "").ifBlank { item.optString("name", "") } else item.toString()
                        if (name.isNotBlank() && selectedSkills.none { it.equals(name, ignoreCase = true) }) {
                            selectedSkills.add(name)
                        }
                    }
                } else {
                    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { skill ->
                        if (selectedSkills.none { it.equals(skill, ignoreCase = true) }) selectedSkills.add(skill)
                    }
                }
            } catch (_: Exception) {
                raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { skill ->
                    if (selectedSkills.none { it.equals(skill, ignoreCase = true) }) selectedSkills.add(skill)
                }
            }
        }
    }

    private fun setupAutoComplete() {
        val allSkills = SkillLibrary.allSkills
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allSkills)
        skillAutoComplete.setAdapter(adapter)
    }

    private fun renderSuggestedSkills() {
        suggestedSkillsChipGroup.removeAllViews()
        val popular = listOf("React.js", "JavaScript", "Node.js", "SQL", "Python", "Java", "Kotlin", "TypeScript", "AWS", "Docker", "Power Apps")
        for (skill in popular) {
            if (selectedSkills.none { it.equals(skill, ignoreCase = true) }) {
                val chip = Chip(requireContext()).apply {
                    text = skill
                    isCheckable = false
                    setOnClickListener {
                        addSkill(skill)
                    }
                }
                suggestedSkillsChipGroup.addView(chip)
            }
        }
    }

    private fun renderSelectedSkills() {
        selectedSkillsChipGroup.removeAllViews()
        for (skill in selectedSkills.toList()) {
            val chip = Chip(requireContext()).apply {
                text = skill
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedSkills.removeAll { it.equals(skill, ignoreCase = true) }
                    renderSuggestedSkills()
                    renderSelectedSkills()
                }
            }
            selectedSkillsChipGroup.addView(chip)
        }
    }

    private fun addSkill(skill: String) {
        val normalized = SkillLibrary.normalize(skill)
        if (normalized.isNotBlank() && selectedSkills.none { it.equals(normalized, ignoreCase = true) }) {
            selectedSkills.add(normalized)
            renderSuggestedSkills()
            renderSelectedSkills()
        }
    }

    private fun saveSkillsToFirestore() {
        val array = JSONArray()
        for (skill in selectedSkills) {
            val obj = JSONObject().apply {
                put("skill", skill)
                put("level", "Intermediate")
            }
            array.put(obj)
        }
        val skillsJsonStr = array.toString()
        val updatedProfile = currentProfile.copy(skills = skillsJsonStr)

        btnAddSkillAction.isEnabled = false
        btnAddSkillAction.text = "Saving..."

        FirebaseProfileRepository.saveProfileToFirebase(requireContext(), updatedProfile) { success, msg ->
            if (isAdded) {
                btnAddSkillAction.isEnabled = true
                btnAddSkillAction.text = "Add Skills"
                if (success) {
                    Toast.makeText(requireContext(), "Skills updated successfully", Toast.LENGTH_SHORT).show()
                    onSkillsSaved()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to save skills: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
