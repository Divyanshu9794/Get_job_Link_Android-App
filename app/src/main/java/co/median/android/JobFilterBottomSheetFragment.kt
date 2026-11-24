package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

data class JobFilterState(
    val selectedJobTypes: Set<String> = emptySet(),
    val selectedExperience: Set<String> = emptySet(),
    val selectedWorkModes: Set<String> = emptySet(),
    val locationQuery: String = "",
    val selectedSkills: Set<String> = emptySet(),
    val companyQuery: String = "",
    val postedDateFilter: String = "Anytime"
) {
    fun activeFilterCount(): Int {
        var count = 0
        if (selectedJobTypes.isNotEmpty()) count += selectedJobTypes.size
        if (selectedExperience.isNotEmpty()) count += selectedExperience.size
        if (selectedWorkModes.isNotEmpty()) count += selectedWorkModes.size
        if (locationQuery.isNotBlank()) count += 1
        if (selectedSkills.isNotEmpty()) count += selectedSkills.size
        if (companyQuery.isNotBlank()) count += 1
        if (postedDateFilter != "Anytime") count += 1
        return count
    }

    fun isEmpty(): Boolean = activeFilterCount() == 0
}

class JobFilterBottomSheetFragment(
    private val initialState: JobFilterState,
    private val onApply: (JobFilterState) -> Unit
) : BottomSheetDialogFragment() {

    private var temporaryState: JobFilterState = initialState

    private lateinit var filterCloseBtn: ImageView
    private lateinit var filterClearHeaderBtn: TextView
    private lateinit var workModeChipGroup: ChipGroup
    private lateinit var jobTypeChipGroup: ChipGroup
    private lateinit var experienceChipGroup: ChipGroup
    private lateinit var postedDateChipGroup: ChipGroup
    private lateinit var filterLocationInput: EditText
    private lateinit var filterSkillAutoComplete: AutoCompleteTextView
    private lateinit var filterSelectedSkillsChipGroup: ChipGroup
    private lateinit var filterCompanyInput: EditText
    private lateinit var filterFooterClearBtn: MaterialButton
    private lateinit var filterFooterApplyBtn: MaterialButton

    private val temporarySkills = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_job_filter, container, false)

        filterCloseBtn = view.findViewById(R.id.filterCloseBtn)
        filterClearHeaderBtn = view.findViewById(R.id.filterClearHeaderBtn)
        workModeChipGroup = view.findViewById(R.id.workModeChipGroup)
        jobTypeChipGroup = view.findViewById(R.id.jobTypeChipGroup)
        experienceChipGroup = view.findViewById(R.id.experienceChipGroup)
        postedDateChipGroup = view.findViewById(R.id.postedDateChipGroup)
        filterLocationInput = view.findViewById(R.id.filterLocationInput)
        filterSkillAutoComplete = view.findViewById(R.id.filterSkillAutoComplete)
        filterSelectedSkillsChipGroup = view.findViewById(R.id.filterSelectedSkillsChipGroup)
        filterCompanyInput = view.findViewById(R.id.filterCompanyInput)
        filterFooterClearBtn = view.findViewById(R.id.filterFooterClearBtn)
        filterFooterApplyBtn = view.findViewById(R.id.filterFooterApplyBtn)

        temporarySkills.addAll(initialState.selectedSkills)

        setupUIFromState(initialState)

        filterCloseBtn.setOnClickListener { dismiss() }

        val clearAllListener = View.OnClickListener {
            temporaryState = JobFilterState()
            temporarySkills.clear()
            setupUIFromState(temporaryState)
        }
        filterClearHeaderBtn.setOnClickListener(clearAllListener)
        filterFooterClearBtn.setOnClickListener(clearAllListener)

        val skillAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, SkillLibrary.allSkills)
        filterSkillAutoComplete.setAdapter(skillAdapter)
        filterSkillAutoComplete.threshold = 1
        filterSkillAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedSkill = skillAdapter.getItem(position) ?: return@setOnItemClickListener
            val normalized = SkillLibrary.normalize(selectedSkill)
            temporarySkills.add(normalized)
            filterSkillAutoComplete.text = null
            renderSkillsChips()
        }

        filterFooterApplyBtn.setOnClickListener {
            val finalJobTypes = getSelectedChipsText(jobTypeChipGroup)
            val finalExperience = getSelectedChipsText(experienceChipGroup)
            val finalWorkModes = getSelectedChipsText(workModeChipGroup)
            val finalPostedDate = getSelectedSingleChipText(postedDateChipGroup) ?: "Anytime"

            val updatedState = JobFilterState(
                selectedJobTypes = finalJobTypes,
                selectedExperience = finalExperience,
                selectedWorkModes = finalWorkModes,
                locationQuery = filterLocationInput.text.toString().trim(),
                selectedSkills = temporarySkills.toSet(),
                companyQuery = filterCompanyInput.text.toString().trim(),
                postedDateFilter = finalPostedDate
            )
            onApply(updatedState)
            dismiss()
        }

        return view
    }

    private fun setupUIFromState(state: JobFilterState) {
        setSelectedChips(workModeChipGroup, state.selectedWorkModes)
        setSelectedChips(jobTypeChipGroup, state.selectedJobTypes)
        setSelectedChips(experienceChipGroup, state.selectedExperience)
        setSelectedSingleChip(postedDateChipGroup, state.postedDateFilter)

        filterLocationInput.setText(state.locationQuery)
        filterCompanyInput.setText(state.companyQuery)

        renderSkillsChips()
    }

    private fun renderSkillsChips() {
        filterSelectedSkillsChipGroup.removeAllViews()
        for (skill in temporarySkills) {
            val chip = Chip(requireContext()).apply {
                text = skill
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    temporarySkills.remove(skill)
                    renderSkillsChips()
                }
            }
            filterSelectedSkillsChipGroup.addView(chip)
        }
    }

    private fun setSelectedChips(chipGroup: ChipGroup, selectedValues: Set<String>) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.isChecked = selectedValues.any { it.equals(chip.text.toString(), ignoreCase = true) }
        }
    }

    private fun setSelectedSingleChip(chipGroup: ChipGroup, selectedValue: String) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.text.toString().equals(selectedValue, ignoreCase = true)) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun getSelectedChipsText(chipGroup: ChipGroup): Set<String> {
        val result = mutableSetOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) {
                result.add(chip.text.toString())
            }
        }
        return result
    }

    private fun getSelectedSingleChipText(chipGroup: ChipGroup): String? {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) {
                return chip.text.toString()
            }
        }
        return null
    }
}
