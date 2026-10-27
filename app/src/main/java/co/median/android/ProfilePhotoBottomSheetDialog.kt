package co.median.android

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfilePhotoBottomSheetDialog(
    private val onPhotoUpdated: () -> Unit
) : BottomSheetDialogFragment() {

    private var selectedImageUri: Uri? = null

    private lateinit var layoutAction: View
    private lateinit var layoutPreview: View

    // Action views
    private lateinit var btnChooseFromGallery: MaterialButton
    private lateinit var btnRemovePhoto: MaterialButton
    private lateinit var btnCancelPhotoAction: MaterialButton

    // Preview views
    private lateinit var imgPhotoPreview: ShapeableImageView
    private lateinit var layoutUploadProgress: LinearLayout
    private lateinit var txtUploadProgressStatus: TextView
    private lateinit var progressUploadPhoto: LinearProgressIndicator
    private lateinit var btnConfirmUploadPhoto: MaterialButton
    private lateinit var btnCancelUploadPhoto: MaterialButton

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            showPreviewMode(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val actionView = inflater.inflate(R.layout.dialog_photo_action, root, false)
        val previewView = inflater.inflate(R.layout.dialog_photo_preview, root, false)

        root.addView(actionView)
        root.addView(previewView)

        layoutAction = actionView
        layoutPreview = previewView

        previewView.visibility = View.GONE

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Action views
        btnChooseFromGallery = layoutAction.findViewById(R.id.btnChooseFromGallery)
        btnRemovePhoto = layoutAction.findViewById(R.id.btnRemovePhoto)
        btnCancelPhotoAction = layoutAction.findViewById(R.id.btnCancelPhotoAction)

        // Bind Preview views
        imgPhotoPreview = layoutPreview.findViewById(R.id.imgPhotoPreview)
        layoutUploadProgress = layoutPreview.findViewById(R.id.layoutUploadProgress)
        txtUploadProgressStatus = layoutPreview.findViewById(R.id.txtUploadProgressStatus)
        progressUploadPhoto = layoutPreview.findViewById(R.id.progressUploadPhoto)
        btnConfirmUploadPhoto = layoutPreview.findViewById(R.id.btnConfirmUploadPhoto)
        btnCancelUploadPhoto = layoutPreview.findViewById(R.id.btnCancelUploadPhoto)

        val profile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        if (profile.photoUrl.isBlank()) {
            btnRemovePhoto.visibility = View.GONE
        } else {
            btnRemovePhoto.visibility = View.VISIBLE
        }

        btnChooseFromGallery.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnRemovePhoto.setOnClickListener {
            showRemovePhotoConfirmation()
        }

        btnCancelPhotoAction.setOnClickListener {
            dismiss()
        }

        btnCancelUploadPhoto.setOnClickListener {
            selectedImageUri = null
            layoutPreview.visibility = View.GONE
            layoutAction.visibility = View.VISIBLE
        }

        btnConfirmUploadPhoto.setOnClickListener {
            val uri = selectedImageUri
            if (uri != null) {
                uploadPhoto(uri)
            }
        }
    }

    private fun showPreviewMode(uri: Uri) {
        layoutAction.visibility = View.GONE
        layoutPreview.visibility = View.VISIBLE
        imgPhotoPreview.setImageURI(uri)
    }

    private fun showRemovePhotoConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Profile Photo?")
            .setMessage("Are you sure you want to remove your profile photo?")
            .setPositiveButton("Remove") { _, _ ->
                removePhoto()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removePhoto() {
        val user = FirebaseAuth.getInstance().currentUser
        val context = context ?: return
        val profile = FirebaseProfileRepository.loadLocalProfile(context)

        if (user == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val oldStoragePath = profile.photoStoragePath

        val updated = profile.copy(photoUrl = "", photoStoragePath = "")
        FirebaseProfileRepository.saveProfileLocally(context, updated)

        val updateData = mapOf(
            "photoUrl" to "",
            "photoStoragePath" to "",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                if (oldStoragePath.isNotBlank()) {
                    try {
                        FirebaseStorage.getInstance().getReference(oldStoragePath).delete()
                    } catch (_: Exception) {}
                }
                onPhotoUpdated()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun uploadPhoto(uri: Uri) {
        val context = context ?: return
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = user.uid
        val storagePath = "users/$uid/profile/profile_photo_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)

        // UI state for upload
        layoutUploadProgress.visibility = View.VISIBLE
        btnConfirmUploadPhoto.isEnabled = false
        btnCancelUploadPhoto.isEnabled = false
        progressUploadPhoto.progress = 0

        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnProgressListener { taskSnapshot ->
            val total = taskSnapshot.totalByteCount
            if (total > 0) {
                val progress = (100.0 * taskSnapshot.bytesTransferred / total).toInt()
                txtUploadProgressStatus.text = "Uploading photo... $progress%"
                progressUploadPhoto.progress = progress
            }
        }.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: Exception("Photo upload failed")
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUrl = task.result.toString()
                val currentProfile = FirebaseProfileRepository.loadLocalProfile(context)
                val updated = currentProfile.copy(photoUrl = downloadUrl, photoStoragePath = storagePath)

                FirebaseProfileRepository.saveProfileToFirebase(context, updated) { success, msg ->
                    if (isAdded) {
                        if (success) {
                            Toast.makeText(context, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
                            onPhotoUpdated()
                            dismiss()
                        } else {
                            layoutUploadProgress.visibility = View.GONE
                            btnConfirmUploadPhoto.isEnabled = true
                            btnCancelUploadPhoto.isEnabled = true
                            Toast.makeText(context, "Failed to save photo metadata: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                if (isAdded) {
                    layoutUploadProgress.visibility = View.GONE
                    btnConfirmUploadPhoto.isEnabled = true
                    btnCancelUploadPhoto.isEnabled = true
                    val e = task.exception
                    Toast.makeText(context, "Unable to upload profile photo. Please check your internet connection and try again. (${e?.localizedMessage})", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
