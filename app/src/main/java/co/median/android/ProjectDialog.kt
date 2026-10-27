package co.median.android

import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

class ProjectDialog(
    context: Context,
    private val onSaved: () -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_project)

        val nameInput = findViewById<TextInputEditText>(R.id.projectNameInput)
        val roleInput = findViewById<TextInputEditText>(R.id.projectRoleInput)
        val descInput = findViewById<TextInputEditText>(R.id.projectDescInput)
        val urlInput = findViewById<TextInputEditText>(R.id.projectUrlInput)
        val saveBtn = findViewById<Button>(R.id.projectSaveBtn)
        val cancelBtn = findViewById<Button>(R.id.projectCancelBtn)

        cancelBtn?.setOnClickListener { dismiss() }

        saveBtn?.setOnClickListener {
            val name = nameInput?.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                nameInput?.error = "Project Name required"
                return@setOnClickListener
            }

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.projects.isNotBlank() && profile.projects != "[]") {
                try { JSONArray(profile.projects) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", "proj_" + System.currentTimeMillis())
                put("title", name)
                put("role", roleInput?.text?.toString()?.trim() ?: "")
                put("description", descInput?.text?.toString()?.trim() ?: "")
                put("projectUrl", urlInput?.text?.toString()?.trim() ?: "")
            }

            jsonArray.put(obj)
            val updatedProfile = profile.copy(projects = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { _, _ ->
                onSaved()
                dismiss()
            }
        }
    }
}
