package co.median.android

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

class ExperienceDialog(
    context: Context,
    private val onSaved: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_experience)

        val titleInput = findViewById<TextInputEditText>(R.id.expTitleInput)
        val companyInput = findViewById<TextInputEditText>(R.id.expCompanyInput)
        val locationInput = findViewById<TextInputEditText>(R.id.expLocationInput)
        val startDateInput = findViewById<TextInputEditText>(R.id.expStartDateInput)
        val endDateInput = findViewById<TextInputEditText>(R.id.expEndDateInput)
        val currentCheck = findViewById<CheckBox>(R.id.expCurrentCheck)
        val descInput = findViewById<TextInputEditText>(R.id.expDescInput)
        val saveBtn = findViewById<Button>(R.id.expSaveBtn)
        val cancelBtn = findViewById<Button>(R.id.expCancelBtn)

        currentCheck?.setOnCheckedChangeListener { _, isChecked ->
            endDateInput?.isEnabled = !isChecked
            if (isChecked) endDateInput?.setText("Present")
        }

        cancelBtn?.setOnClickListener { dismiss() }

        saveBtn?.setOnClickListener {
            val title = titleInput?.text?.toString()?.trim() ?: ""
            val company = companyInput?.text?.toString()?.trim() ?: ""
            if (title.isEmpty() || company.isEmpty()) {
                titleInput?.error = "Job title and company required"
                return@setOnClickListener
            }

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.experience.isNotBlank() && profile.experience != "[]") {
                try { JSONArray(profile.experience) } catch (e: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", "exp_" + System.currentTimeMillis())
                put("title", title)
                put("company", company)
                put("location", locationInput?.text?.toString()?.trim() ?: "")
                put("startDate", startDateInput?.text?.toString()?.trim() ?: "")
                put("endDate", endDateInput?.text?.toString()?.trim() ?: "")
                put("description", descInput?.text?.toString()?.trim() ?: "")
            }

            jsonArray.put(obj)
            val updatedProfile = profile.copy(experience = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { _, _ ->
                onSaved()
                dismiss()
            }
        }
    }
}
