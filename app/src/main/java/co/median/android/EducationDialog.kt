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

class EducationDialog @JvmOverloads constructor(
    context: Context,
    private val editIndex: Int = -1,
    private val editItem: JSONObject? = null,
    private val onSaved: () -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_education)

        val btnClose = findViewById<ImageView>(R.id.btnCloseEdu)
        val txtTitle = findViewById<TextView>(R.id.txtEduTitle)
        val layoutDegree = findViewById<TextInputLayout>(R.id.layoutEduDegree)
        val layoutInst = findViewById<TextInputLayout>(R.id.layoutEduInst)
        val degreeInput = findViewById<TextInputEditText>(R.id.eduDegreeInput)
        val instInput = findViewById<TextInputEditText>(R.id.eduInstInput)
        val fieldInput = findViewById<TextInputEditText>(R.id.eduFieldInput)
        val startInput = findViewById<TextInputEditText>(R.id.eduStartInput)
        val endInput = findViewById<TextInputEditText>(R.id.eduEndInput)
        val layoutEnd = findViewById<TextInputLayout>(R.id.layoutEduEnd)
        val currentCheck = findViewById<MaterialCheckBox>(R.id.eduCurrentCheck)
        val gradeInput = findViewById<TextInputEditText>(R.id.eduGradeInput)
        val descInput = findViewById<TextInputEditText>(R.id.eduDescInput)
        val saveBtn = findViewById<MaterialButton>(R.id.eduSaveBtn)

        btnClose?.setOnClickListener { dismiss() }

        currentCheck?.setOnCheckedChangeListener { _, isChecked ->
            layoutEnd?.isEnabled = !isChecked
            if (isChecked) {
                endInput?.setText("Present")
            } else if (endInput?.text?.toString() == "Present") {
                endInput?.setText("")
            }
        }

        // Pre-fill if editing
        if (editIndex >= 0 && editItem != null) {
            txtTitle?.text = "Edit Education"
            saveBtn?.text = "Update Education"
            degreeInput?.setText(editItem.optString("degree", ""))
            instInput?.setText(editItem.optString("institution", ""))
            fieldInput?.setText(editItem.optString("field", ""))
            startInput?.setText(editItem.optString("startYear", ""))
            val endVal = editItem.optString("endYear", "")
            if (endVal.equals("Present", ignoreCase = true) || editItem.optBoolean("current", false)) {
                currentCheck?.isChecked = true
                endInput?.setText("Present")
                layoutEnd?.isEnabled = false
            } else {
                endInput?.setText(endVal)
            }
            gradeInput?.setText(editItem.optString("grade", ""))
            descInput?.setText(editItem.optString("description", ""))
        } else {
            txtTitle?.text = "Add Education"
            saveBtn?.text = "Save Education"
        }

        saveBtn?.setOnClickListener {
            val degree = degreeInput?.text?.toString()?.trim() ?: ""
            val inst = instInput?.text?.toString()?.trim() ?: ""

            if (degree.isEmpty()) {
                layoutDegree?.error = "Degree is required"
                degreeInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutDegree?.error = null
            }

            if (inst.isEmpty()) {
                layoutInst?.error = "Institution is required"
                instInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutInst?.error = null
            }

            saveBtn.isEnabled = false
            saveBtn.text = "Saving..."

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.education.isNotBlank() && profile.education != "[]") {
                try { JSONArray(profile.education) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val isCurrent = currentCheck?.isChecked == true
            val endYearVal = if (isCurrent) "Present" else (endInput?.text?.toString()?.trim() ?: "")

            val obj = (editItem ?: JSONObject()).apply {
                if (!has("id")) put("id", "edu_" + System.currentTimeMillis())
                put("degree", degree)
                put("institution", inst)
                put("field", fieldInput?.text?.toString()?.trim() ?: "")
                put("startYear", startInput?.text?.toString()?.trim() ?: "")
                put("endYear", endYearVal)
                put("current", isCurrent)
                put("grade", gradeInput?.text?.toString()?.trim() ?: "")
                put("description", descInput?.text?.toString()?.trim() ?: "")
            }

            if (editIndex >= 0 && editIndex < jsonArray.length()) {
                jsonArray.put(editIndex, obj)
            } else {
                jsonArray.put(obj)
            }

            val updatedProfile = profile.copy(education = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { success, msg ->
                saveBtn.isEnabled = true
                saveBtn.text = if (editIndex >= 0) "Update Education" else "Save Education"
                if (success) {
                    Toast.makeText(context, if (editIndex >= 0) "Education updated" else "Education added", Toast.LENGTH_SHORT).show()
                    onSaved()
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to save education: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
