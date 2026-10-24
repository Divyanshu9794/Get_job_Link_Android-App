package co.median.android

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editNameInput: TextInputEditText
    private lateinit var editHeadlineInput: TextInputEditText
    private lateinit var editLocationInput: TextInputEditText
    private lateinit var editPhoneInput: TextInputEditText
    private lateinit var editEmailInput: TextInputEditText
    private lateinit var editAboutInput: TextInputEditText
    private lateinit var editSkillsInput: TextInputEditText
    private lateinit var editRoleInput: TextInputEditText
    private lateinit var editLinkedinInput: TextInputEditText
    private lateinit var saveChangesBtn: Button
    private lateinit var saveProgressIndicator: LinearProgressIndicator

    private lateinit var currentProfile: UserProfile
    private var hasChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        editNameInput = findViewById(R.id.editNameInput)
        editHeadlineInput = findViewById(R.id.editHeadlineInput)
        editLocationInput = findViewById(R.id.editLocationInput)
        editPhoneInput = findViewById(R.id.editPhoneInput)
        editEmailInput = findViewById(R.id.editEmailInput)
        editAboutInput = findViewById(R.id.editAboutInput)
        editSkillsInput = findViewById(R.id.editSkillsInput)
        editRoleInput = findViewById(R.id.editRoleInput)
        editLinkedinInput = findViewById(R.id.editLinkedinInput)
        saveChangesBtn = findViewById(R.id.saveChangesBtn)
        saveProgressIndicator = findViewById(R.id.saveProgressIndicator)

        currentProfile = FirebaseProfileRepository.loadLocalProfile(this)

        editNameInput.setText(currentProfile.name)
        editHeadlineInput.setText(currentProfile.headline)
        editLocationInput.setText(currentProfile.location)
        editPhoneInput.setText(currentProfile.phone)
        editEmailInput.setText(currentProfile.email)
        editAboutInput.setText(currentProfile.about.ifBlank { currentProfile.bio })
        editSkillsInput.setText(currentProfile.skills)
        editRoleInput.setText(currentProfile.jobRole)
        editLinkedinInput.setText(currentProfile.linkedInUrl)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hasChanges = true
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editNameInput.addTextChangedListener(watcher)
        editHeadlineInput.addTextChangedListener(watcher)
        editLocationInput.addTextChangedListener(watcher)
        editPhoneInput.addTextChangedListener(watcher)
        editAboutInput.addTextChangedListener(watcher)
        editSkillsInput.addTextChangedListener(watcher)
        editRoleInput.addTextChangedListener(watcher)
        editLinkedinInput.addTextChangedListener(watcher)

        saveChangesBtn.setOnClickListener {
            saveProfile()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasChanges) {
                    AlertDialog.Builder(this@EditProfileActivity)
                        .setTitle("Discard changes?")
                        .setMessage("You have unsaved changes. What would you like to do?")
                        .setPositiveButton("Discard") { _, _ -> finish() }
                        .setNegativeButton("Keep Editing", null)
                        .show()
                } else {
                    finish()
                }
            }
        })
    }

    private fun saveProfile() {
        val name = editNameInput.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            editNameInput.error = "Full Name is required"
            editNameInput.requestFocus()
            return
        }

        val updated = currentProfile.copy(
            name = name,
            headline = editHeadlineInput.text?.toString()?.trim() ?: "",
            location = editLocationInput.text?.toString()?.trim() ?: "",
            phone = editPhoneInput.text?.toString()?.trim() ?: "",
            about = editAboutInput.text?.toString()?.trim() ?: "",
            bio = editAboutInput.text?.toString()?.trim() ?: "",
            skills = editSkillsInput.text?.toString()?.trim() ?: "",
            jobRole = editRoleInput.text?.toString()?.trim() ?: "",
            linkedInUrl = editLinkedinInput.text?.toString()?.trim() ?: ""
        )

        saveProgressIndicator.visibility = View.VISIBLE
        saveChangesBtn.isEnabled = false
        saveChangesBtn.text = "Saving..."

        FirebaseProfileRepository.saveProfileToFirebase(this, updated) { success, _ ->
            saveProgressIndicator.visibility = View.GONE
            saveChangesBtn.isEnabled = true
            saveChangesBtn.text = "Save Changes"

            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                showErrorDialog()
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Couldn't update your profile. Please try again.")
            .setPositiveButton("Retry") { _, _ -> saveProfile() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
