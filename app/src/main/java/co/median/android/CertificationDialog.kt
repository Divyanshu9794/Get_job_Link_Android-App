package co.median.android

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject

class CertificationDialog(
    context: Context,
    private val onSaved: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_certification)

        val certNameInput = findViewById<TextInputEditText>(R.id.certNameInput)
        val certOrgInput = findViewById<TextInputEditText>(R.id.certOrgInput)
        val certDateInput = findViewById<TextInputEditText>(R.id.certDateInput)
        val certUrlInput = findViewById<TextInputEditText>(R.id.certUrlInput)
        val saveBtn = findViewById<Button>(R.id.certSaveBtn)
        val cancelBtn = findViewById<Button>(R.id.certCancelBtn)

        cancelBtn?.setOnClickListener { dismiss() }

        saveBtn?.setOnClickListener {
            val name = certNameInput?.text?.toString()?.trim() ?: ""
            val org = certOrgInput?.text?.toString()?.trim() ?: ""
            if (name.isEmpty() || org.isEmpty()) {
                certNameInput?.error = "Certification Name and Organization required"
                return@setOnClickListener
            }

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.certifications.isNotBlank() && profile.certifications != "[]") {
                try { JSONArray(profile.certifications) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", "cert_" + System.currentTimeMillis())
                put("title", name)
                put("organization", org)
                put("issueDate", certDateInput?.text?.toString()?.trim() ?: "")
                put("credentialUrl", certUrlInput?.text?.toString()?.trim() ?: "")
            }

            jsonArray.put(obj)
            val updatedProfile = profile.copy(certifications = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { _, _ ->
                onSaved()
                dismiss()
            }
        }
    }
}
