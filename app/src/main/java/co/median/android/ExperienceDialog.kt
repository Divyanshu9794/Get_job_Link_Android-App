package co.median.android

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class ExperienceDialog @JvmOverloads constructor(
    context: Context,
    private val editIndex: Int = -1,
    private val editItem: JSONObject? = null,
    private val onSaved: () -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_experience)

        val btnClose = findViewById<ImageView>(R.id.btnCloseExp)
        val txtTitle = findViewById<TextView>(R.id.txtExpTitle)
        val layoutTitle = findViewById<TextInputLayout>(R.id.layoutExpTitle)
        val layoutCompany = findViewById<TextInputLayout>(R.id.layoutExpCompany)
        val layoutStart = findViewById<TextInputLayout>(R.id.layoutExpStart)
        val layoutEnd = findViewById<TextInputLayout>(R.id.layoutExpEnd)
        val titleInput = findViewById<TextInputEditText>(R.id.expTitleInput)
        val companyInput = findViewById<TextInputEditText>(R.id.expCompanyInput)
        val locationInput = findViewById<TextInputEditText>(R.id.expLocationInput)
        val startDateInput = findViewById<TextInputEditText>(R.id.expStartDateInput)
        val endDateInput = findViewById<TextInputEditText>(R.id.expEndDateInput)
        val currentCheck = findViewById<MaterialCheckBox>(R.id.expCurrentCheck)
        val descInput = findViewById<TextInputEditText>(R.id.expDescInput)
        val saveBtn = findViewById<MaterialButton>(R.id.expSaveBtn)

        btnClose?.setOnClickListener { dismiss() }

        currentCheck?.setOnCheckedChangeListener { _, isChecked ->
            layoutEnd?.isEnabled = !isChecked
            if (isChecked) {
                endDateInput?.setText("Present")
            } else if (endDateInput?.text?.toString() == "Present") {
                endDateInput?.setText("")
            }
        }

        if (editIndex >= 0 && editItem != null) {
            txtTitle?.text = "Edit Experience"
            saveBtn?.text = "Update Experience"
            titleInput?.setText(editItem.optString("title", ""))
            companyInput?.setText(editItem.optString("company", ""))
            locationInput?.setText(editItem.optString("location", ""))
            startDateInput?.setText(editItem.optString("startDate", ""))
            val endVal = editItem.optString("endDate", "")
            if (endVal.equals("Present", ignoreCase = true) || editItem.optBoolean("current", false)) {
                currentCheck?.isChecked = true
                endDateInput?.setText("Present")
                layoutEnd?.isEnabled = false
            } else {
                endDateInput?.setText(endVal)
            }
            descInput?.setText(editItem.optString("description", ""))
        } else {
            txtTitle?.text = "Add Experience"
            saveBtn?.text = "Save Experience"
        }

        saveBtn?.setOnClickListener {
            val title = titleInput?.text?.toString()?.trim() ?: ""
            val company = companyInput?.text?.toString()?.trim() ?: ""
            val startDate = startDateInput?.text?.toString()?.trim() ?: ""

            if (title.isEmpty()) {
                layoutTitle?.error = "Job title is required"
                titleInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutTitle?.error = null
            }

            if (company.isEmpty()) {
                layoutCompany?.error = "Company is required"
                companyInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutCompany?.error = null
            }

            if (startDate.isEmpty()) {
                layoutStart?.error = "Start date is required"
                startDateInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutStart?.error = null
            }

            saveBtn.isEnabled = false
            saveBtn.text = "Saving..."

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.experience.isNotBlank() && profile.experience != "[]") {
                try { JSONArray(profile.experience) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val isCurrent = currentCheck?.isChecked == true
            val endDateVal = if (isCurrent) "Present" else (endDateInput?.text?.toString()?.trim() ?: "")

            val obj = (editItem ?: JSONObject()).apply {
                if (!has("id")) put("id", "exp_" + System.currentTimeMillis())
                put("title", title)
                put("company", company)
                put("location", locationInput?.text?.toString()?.trim() ?: "")
                put("startDate", startDate)
                put("endDate", endDateVal)
                put("current", isCurrent)
                put("description", descInput?.text?.toString()?.trim() ?: "")
            }

            if (editIndex >= 0 && editIndex < jsonArray.length()) {
                jsonArray.put(editIndex, obj)
            } else {
                jsonArray.put(obj)
            }

            val updatedProfile = profile.copy(experience = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { success, msg ->
                saveBtn.isEnabled = true
                saveBtn.text = if (editIndex >= 0) "Update Experience" else "Save Experience"
                if (success) {
                    Toast.makeText(context, if (editIndex >= 0) "Experience updated" else "Experience added", Toast.LENGTH_SHORT).show()
                    onSaved()
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to save experience: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
