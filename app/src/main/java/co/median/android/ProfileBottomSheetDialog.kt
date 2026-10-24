package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class ProfileBottomSheetDialog(
    private val onProfileSaved: ((UserProfile) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var editName: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var editHeadline: TextInputEditText
    private lateinit var editPhone: TextInputEditText
    private lateinit var editJobRole: TextInputEditText
    private lateinit var editSkills: TextInputEditText
    private lateinit var editLocation: TextInputEditText
    private lateinit var editAbout: TextInputEditText
    private lateinit var progressSaving: LinearProgressIndicator
    private lateinit var btnSave: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.edit_name)
        editEmail = view.findViewById(R.id.edit_email)
        editHeadline = view.findViewById(R.id.edit_headline)
        editPhone = view.findViewById(R.id.edit_phone)
        editJobRole = view.findViewById(R.id.edit_job_role)
        editSkills = view.findViewById(R.id.edit_skills)
        editLocation = view.findViewById(R.id.edit_location)
        editAbout = view.findViewById(R.id.edit_about)
        progressSaving = view.findViewById(R.id.progress_saving)
        btnSave = view.findViewById(R.id.btn_save_profile)

        // Load existing profile
        val currentProfile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        editName.setText(currentProfile.name)
        editEmail.setText(currentProfile.email)
        editHeadline.setText(currentProfile.headline)
        editPhone.setText(currentProfile.phone)
        editJobRole.setText(currentProfile.jobRole)
        editSkills.setText(currentProfile.skills)
        editLocation.setText(currentProfile.location)
        editAbout.setText(currentProfile.about.ifBlank { currentProfile.bio })

        btnSave.setOnClickListener {
            val name = editName.text?.toString()?.trim() ?: ""
            val email = editEmail.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                editName.error = "Please enter your name"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                editEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            val updatedProfile = currentProfile.copy(
                name = name,
                email = email,
                headline = editHeadline.text?.toString()?.trim() ?: "",
                phone = editPhone.text?.toString()?.trim() ?: "",
                jobRole = editJobRole.text?.toString()?.trim() ?: "",
                skills = editSkills.text?.toString()?.trim() ?: "",
                location = editLocation.text?.toString()?.trim() ?: "",
                about = editAbout.text?.toString()?.trim() ?: "",
                bio = editAbout.text?.toString()?.trim() ?: ""
            )

            progressSaving.visibility = View.VISIBLE
            btnSave.isEnabled = false

            FirebaseProfileRepository.saveProfileToFirebase(
                requireContext(),
                updatedProfile
            ) { success, message ->
                if (isAdded) {
                    progressSaving.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    onProfileSaved?.invoke(updatedProfile)
                    dismiss()
                }
            }
        }
    }
}
