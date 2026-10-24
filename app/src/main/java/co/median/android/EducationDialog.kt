package co.median.android

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

class EducationDialog(
    context: Context,
    private val onSaved: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_education)

        val degreeInput = findViewById<TextInputEditText>(R.id.eduDegreeInput)
        val instInput = findViewById<TextInputEditText>(R.id.eduInstInput)
        val fieldInput = findViewById<TextInputEditText>(R.id.eduFieldInput)
        val startInput = findViewById<TextInputEditText>(R.id.eduStartInput)
        val endInput = findViewById<TextInputEditText>(R.id.eduEndInput)
        val gradeInput = findViewById<TextInputEditText>(R.id.eduGradeInput)
        val saveBtn = findViewById<Button>(R.id.eduSaveBtn)
        val cancelBtn = findViewById<Button>(R.id.eduCancelBtn)

        cancelBtn?.setOnClickListener { dismiss() }

        saveBtn?.setOnClickListener {
            val degree = degreeInput?.text?.toString()?.trim() ?: ""
            val inst = instInput?.text?.toString()?.trim() ?: ""
            if (degree.isEmpty() || inst.isEmpty()) {
                degreeInput?.error = "Degree and Institution required"
                return@setOnClickListener
            }

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.education.isNotBlank() && profile.education != "[]") {
                try { JSONArray(profile.education) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", "edu_" + System.currentTimeMillis())
                put("degree", degree)
                put("institution", inst)
                put("field", fieldInput?.text?.toString()?.trim() ?: "")
                put("startYear", startInput?.text?.toString()?.trim() ?: "")
                put("endYear", endInput?.text?.toString()?.trim() ?: "")
                put("grade", gradeInput?.text?.toString()?.trim() ?: "")
            }

            jsonArray.put(obj)
            val updatedProfile = profile.copy(education = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { _, _ ->
                onSaved()
                dismiss()
            }
        }
    }
}
