package co.median.android

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class CertificationDialog @JvmOverloads constructor(
    context: Context,
    private val editIndex: Int = -1,
    private val editItem: JSONObject? = null,
    private val onSaved: () -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_certification)

        val btnClose = findViewById<ImageView>(R.id.btnCloseCert)
        val txtTitle = findViewById<TextView>(R.id.txtCertTitle)
        val layoutName = findViewById<TextInputLayout>(R.id.layoutCertName)
        val layoutOrg = findViewById<TextInputLayout>(R.id.layoutCertOrg)
        val certNameInput = findViewById<TextInputEditText>(R.id.certNameInput)
        val certOrgInput = findViewById<TextInputEditText>(R.id.certOrgInput)
        val certDateInput = findViewById<TextInputEditText>(R.id.certDateInput)
        val certIdInput = findViewById<TextInputEditText>(R.id.certIdInput)
        val certUrlInput = findViewById<TextInputEditText>(R.id.certUrlInput)
        val saveBtn = findViewById<MaterialButton>(R.id.certSaveBtn)

        btnClose?.setOnClickListener { dismiss() }

        if (editIndex >= 0 && editItem != null) {
            txtTitle?.text = "Edit Certification"
            saveBtn?.text = "Update Certification"
            certNameInput?.setText(editItem.optString("title", ""))
            certOrgInput?.setText(editItem.optString("organization", ""))
            certDateInput?.setText(editItem.optString("issueDate", ""))
            certIdInput?.setText(editItem.optString("credentialId", ""))
            certUrlInput?.setText(editItem.optString("credentialUrl", ""))
        } else {
            txtTitle?.text = "Add Certification"
            saveBtn?.text = "Save Certification"
        }

        saveBtn?.setOnClickListener {
            val name = certNameInput?.text?.toString()?.trim() ?: ""
            val org = certOrgInput?.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                layoutName?.error = "Certification Name is required"
                certNameInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutName?.error = null
            }

            if (org.isEmpty()) {
                layoutOrg?.error = "Issuing Organization is required"
                certOrgInput?.requestFocus()
                return@setOnClickListener
            } else {
                layoutOrg?.error = null
            }

            saveBtn.isEnabled = false
            saveBtn.text = "Saving..."

            val profile = FirebaseProfileRepository.loadLocalProfile(context)
            val jsonArray = if (profile.certifications.isNotBlank() && profile.certifications != "[]") {
                try { JSONArray(profile.certifications) } catch (_: Exception) { JSONArray() }
            } else {
                JSONArray()
            }

            val obj = (editItem ?: JSONObject()).apply {
                if (!has("id")) put("id", "cert_" + System.currentTimeMillis())
                put("title", name)
                put("organization", org)
                put("issueDate", certDateInput?.text?.toString()?.trim() ?: "")
                put("credentialId", certIdInput?.text?.toString()?.trim() ?: "")
                put("credentialUrl", certUrlInput?.text?.toString()?.trim() ?: "")
            }

            if (editIndex >= 0 && editIndex < jsonArray.length()) {
                jsonArray.put(editIndex, obj)
            } else {
                jsonArray.put(obj)
            }

            val updatedProfile = profile.copy(certifications = jsonArray.toString())
            FirebaseProfileRepository.saveProfileToFirebase(context, updatedProfile) { success, msg ->
                saveBtn.isEnabled = true
                saveBtn.text = if (editIndex >= 0) "Update Certification" else "Save Certification"
                if (success) {
                    Toast.makeText(context, if (editIndex >= 0) "Certification updated" else "Certification added", Toast.LENGTH_SHORT).show()
                    onSaved()
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to save certification: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
