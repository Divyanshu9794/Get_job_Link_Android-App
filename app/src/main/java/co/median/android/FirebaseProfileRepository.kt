package co.median.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val headline: String = "",
    val jobRole: String = "",
    val skills: String = "",
    val location: String = "",
    val openToWork: Boolean = true,
    val bio: String = "",
    val about: String = "",
    val role: String = "user",
    val dob: String = "",
    val linkedInUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = "",
    val websiteUrl: String = "",
    val resumeFileName: String = "",
    val resumeStoragePath: String = "",
    val resumeDownloadUrl: String = "",
    val resumeContentType: String = "",
    val resumeFileSize: Long = 0L,
    val resumeData: String = "",
    val resumeUpdatedAt: String = "",
    val resumeVersion: Int = 1,
    val certifications: String = "",
    val education: String = "",
    val experience: String = "",
    val projects: String = "",
    val languages: String = "",
    val preferences: String = "",
    val careerPreferences: String = "",
    val privacySettings: String = "",
    val lastOpenedAt: Long = 0L
) {
    fun calculateCompletionPercentage(): Int {
        var score = 0
        if (name.isNotBlank() && email.isNotBlank()) score += 10
        if (headline.isNotBlank() || jobRole.isNotBlank()) score += 10
        if (location.isNotBlank()) score += 5
        if (about.isNotBlank() || bio.isNotBlank()) score += 10
        if (skills.isNotBlank()) score += 15
        if (experience.isNotBlank() && experience != "[]" && experience != "null") score += 15
        if (education.isNotBlank() && education != "[]" && education != "null") score += 10
        if (resumeFileName.isNotBlank() || resumeDownloadUrl.isNotBlank() || resumeStoragePath.isNotBlank()) score += 10
        if ((projects.isNotBlank() && projects != "[]") || (certifications.isNotBlank() && certifications != "[]")) score += 10
        if (preferences.isNotBlank() || careerPreferences.isNotBlank()) score += 5
        return if (score > 100) 100 else score
    }
}

object FirebaseProfileRepository {

    private const val TAG = "FirebaseProfileRepo"
    private const val PREFS_NAME = "user_profile_prefs"
    private const val COLLECTION_USERS = "users"
    private const val ADMIN_EMAIL = "sdivyanshu352@gmail.com"

    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "profile_name"
    private const val KEY_EMAIL = "profile_email"
    private const val KEY_PHONE = "profile_phone"
    private const val KEY_PHOTO_URL = "profile_photo_url"
    private const val KEY_HEADLINE = "profile_headline"
    private const val KEY_JOB_ROLE = "profile_job_role"
    private const val KEY_SKILLS = "profile_skills"
    private const val KEY_LOCATION = "profile_location"
    private const val KEY_OPEN_TO_WORK = "profile_open_to_work"
    private const val KEY_BIO = "profile_bio"
    private const val KEY_ABOUT = "profile_about"
    private const val KEY_ROLE = "profile_role"
    private const val KEY_DOB = "profile_dob"
    private const val KEY_LINKEDIN = "profile_linkedin"
    private const val KEY_GITHUB = "profile_github"
    private const val KEY_PORTFOLIO = "profile_portfolio"
    private const val KEY_WEBSITE = "profile_website"
    private const val KEY_RESUME_NAME = "profile_resume_name"
    private const val KEY_RESUME_STORAGE_PATH = "profile_resume_storage_path"
    private const val KEY_RESUME_DOWNLOAD_URL = "profile_resume_download_url"
    private const val KEY_RESUME_CONTENT_TYPE = "profile_resume_content_type"
    private const val KEY_RESUME_FILE_SIZE = "profile_resume_file_size"
    private const val KEY_RESUME_DATA = "profile_resume_data"
    private const val KEY_RESUME_UPDATED = "profile_resume_updated"
    private const val KEY_RESUME_VERSION = "profile_resume_version"
    private const val KEY_CERTIFICATIONS = "profile_certifications"
    private const val KEY_EDUCATION = "profile_education"
    private const val KEY_EXPERIENCE = "profile_experience"
    private const val KEY_PROJECTS = "profile_projects"
    private const val KEY_LANGUAGES = "profile_languages"
    private const val KEY_PREFERENCES = "profile_preferences"
    private const val KEY_CAREER_PREFERENCES = "profile_career_preferences"
    private const val KEY_PRIVACY = "profile_privacy"
    private const val KEY_LAST_OPENED = "profile_last_opened"

    @JvmStatic
    fun getCurrentUid(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun isAdminEmail(email: String?): Boolean {
        if (email.isNullOrEmpty()) return false
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

    @JvmStatic
    fun getOrCreateUserId(context: Context): String {
        val uid = getCurrentUid()
        if (!uid.isNullOrBlank()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_USER_ID, uid).apply()
            return uid
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrEmpty()) {
            userId = "user_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    @JvmStatic
    fun clearLocalProfile(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @JvmStatic
    fun loadLocalProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = getOrCreateUserId(context)
        val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }

        var email = prefs.getString(KEY_EMAIL, "") ?: ""
        if (email.isBlank() && currentUser?.email != null) {
            email = currentUser.email ?: ""
        }

        var name = prefs.getString(KEY_NAME, "") ?: ""
        if (name.isBlank() && currentUser?.displayName != null) {
            name = currentUser.displayName ?: ""
        }

        var photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: ""
        if (photoUrl.isBlank() && currentUser?.photoUrl != null) {
            photoUrl = currentUser.photoUrl.toString()
        }

        val role = if (isAdminEmail(email)) "admin" else (prefs.getString(KEY_ROLE, "user") ?: "user")

        return UserProfile(
            id = userId,
            name = name,
            email = email,
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            photoUrl = photoUrl,
            headline = prefs.getString(KEY_HEADLINE, "") ?: "",
            jobRole = prefs.getString(KEY_JOB_ROLE, "") ?: "",
            skills = prefs.getString(KEY_SKILLS, "") ?: "",
            location = prefs.getString(KEY_LOCATION, "") ?: "",
            openToWork = prefs.getBoolean(KEY_OPEN_TO_WORK, true),
            bio = prefs.getString(KEY_BIO, "") ?: "",
            about = prefs.getString(KEY_ABOUT, "") ?: "",
            role = role,
            dob = prefs.getString(KEY_DOB, "") ?: "",
            linkedInUrl = prefs.getString(KEY_LINKEDIN, "") ?: "",
            githubUrl = prefs.getString(KEY_GITHUB, "") ?: "",
            portfolioUrl = prefs.getString(KEY_PORTFOLIO, "") ?: "",
            websiteUrl = prefs.getString(KEY_WEBSITE, "") ?: "",
            resumeFileName = prefs.getString(KEY_RESUME_NAME, "") ?: "",
            resumeStoragePath = prefs.getString(KEY_RESUME_STORAGE_PATH, "") ?: "",
            resumeDownloadUrl = prefs.getString(KEY_RESUME_DOWNLOAD_URL, "") ?: "",
            resumeContentType = prefs.getString(KEY_RESUME_CONTENT_TYPE, "") ?: "",
            resumeFileSize = prefs.getLong(KEY_RESUME_FILE_SIZE, 0L),
            resumeData = prefs.getString(KEY_RESUME_DATA, "") ?: "",
            resumeUpdatedAt = prefs.getString(KEY_RESUME_UPDATED, "") ?: "",
            resumeVersion = prefs.getInt(KEY_RESUME_VERSION, 1),
            certifications = prefs.getString(KEY_CERTIFICATIONS, "") ?: "",
            education = prefs.getString(KEY_EDUCATION, "") ?: "",
            experience = prefs.getString(KEY_EXPERIENCE, "") ?: "",
            projects = prefs.getString(KEY_PROJECTS, "") ?: "",
            languages = prefs.getString(KEY_LANGUAGES, "") ?: "",
            preferences = prefs.getString(KEY_PREFERENCES, "") ?: "",
            careerPreferences = prefs.getString(KEY_CAREER_PREFERENCES, "") ?: "",
            privacySettings = prefs.getString(KEY_PRIVACY, "") ?: "",
            lastOpenedAt = prefs.getLong(KEY_LAST_OPENED, 0L)
        )
    }

    @JvmStatic
    fun saveProfileLocally(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val resolvedRole = if (isAdminEmail(profile.email)) "admin" else profile.role

        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_PHOTO_URL, profile.photoUrl)
            .putString(KEY_HEADLINE, profile.headline)
            .putString(KEY_JOB_ROLE, profile.jobRole)
            .putString(KEY_SKILLS, profile.skills)
            .putString(KEY_LOCATION, profile.location)
            .putBoolean(KEY_OPEN_TO_WORK, profile.openToWork)
            .putString(KEY_BIO, profile.bio)
            .putString(KEY_ABOUT, profile.about)
            .putString(KEY_ROLE, resolvedRole)
            .putString(KEY_DOB, profile.dob)
            .putString(KEY_LINKEDIN, profile.linkedInUrl)
            .putString(KEY_GITHUB, profile.githubUrl)
            .putString(KEY_PORTFOLIO, profile.portfolioUrl)
            .putString(KEY_WEBSITE, profile.websiteUrl)
            .putString(KEY_RESUME_NAME, profile.resumeFileName)
            .putString(KEY_RESUME_STORAGE_PATH, profile.resumeStoragePath)
            .putString(KEY_RESUME_DOWNLOAD_URL, profile.resumeDownloadUrl)
            .putString(KEY_RESUME_CONTENT_TYPE, profile.resumeContentType)
            .putLong(KEY_RESUME_FILE_SIZE, profile.resumeFileSize)
            .putString(KEY_RESUME_DATA, profile.resumeData)
            .putString(KEY_RESUME_UPDATED, profile.resumeUpdatedAt)
            .putInt(KEY_RESUME_VERSION, profile.resumeVersion)
            .putString(KEY_CERTIFICATIONS, profile.certifications)
            .putString(KEY_EDUCATION, profile.education)
            .putString(KEY_EXPERIENCE, profile.experience)
            .putString(KEY_PROJECTS, profile.projects)
            .putString(KEY_LANGUAGES, profile.languages)
            .putString(KEY_PREFERENCES, profile.preferences)
            .putString(KEY_CAREER_PREFERENCES, profile.careerPreferences)
            .putString(KEY_PRIVACY, profile.privacySettings)
            .putLong(KEY_LAST_OPENED, profile.lastOpenedAt)
            .apply()
    }

    @JvmStatic
    fun ensureUserProfileExists(context: Context, onComplete: ((UserProfile) -> Unit)? = null) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val local = loadLocalProfile(context)
            onComplete?.invoke(local)
            return
        }

        val uid = currentUser.uid
        val docRef = FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)

        Log.d(TAG, "Diagnostic 1: Checking profile document users/$uid")

        docRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.d(TAG, "Diagnostic 2: Document users/$uid does not exist. Creating new profile.")
                val initialProfile = UserProfile(
                    id = uid,
                    name = currentUser.displayName ?: "Job Seeker",
                    email = currentUser.email ?: "",
                    photoUrl = currentUser.photoUrl?.toString() ?: "",
                    role = if (isAdminEmail(currentUser.email)) "admin" else "user"
                )
                saveProfileLocally(context, initialProfile)

                val data = hashMapOf(
                    "uid" to uid,
                    "name" to initialProfile.name,
                    "email" to initialProfile.email,
                    "photoUrl" to initialProfile.photoUrl,
                    "role" to initialProfile.role,
                    "openToWork" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data, SetOptions.merge()).addOnSuccessListener {
                    Log.d(TAG, "Diagnostic 3: Created users/$uid successfully.")
                    onComplete?.invoke(initialProfile)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create users/$uid: ${e.localizedMessage}", e)
                    onComplete?.invoke(initialProfile)
                }
            } else {
                Log.d(TAG, "Diagnostic 2: Document users/$uid exists. Fetching profile.")
                val profile = parseSnapshotToUserProfile(snapshot, context, currentUser)
                saveProfileLocally(context, profile)

                val updates = mutableMapOf<String, Any>()
                val existingName = snapshot.getString("name") ?: ""
                val existingEmail = snapshot.getString("email") ?: ""
                val existingPhoto = snapshot.getString("photoUrl") ?: ""

                if (existingName.isBlank() && !currentUser.displayName.isNullOrBlank()) {
                    updates["name"] = currentUser.displayName ?: ""
                }
                if (existingEmail.isBlank() && !currentUser.email.isNullOrBlank()) {
                    updates["email"] = currentUser.email ?: ""
                }
                if (existingPhoto.isBlank() && !currentUser.photoUrl?.toString().isNullOrBlank()) {
                    updates["photoUrl"] = currentUser.photoUrl.toString()
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    docRef.set(updates, SetOptions.merge())
                }

                onComplete?.invoke(profile)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to fetch users/$uid from Firestore: ${e.localizedMessage}", e)
            val local = loadLocalProfile(context)
            onComplete?.invoke(local)
        }
    }

    @JvmStatic
    fun fetchProfileFromFirebase(context: Context, onComplete: (UserProfile) -> Unit) {
        val uid = getCurrentUid()
        if (uid.isNullOrBlank()) {
            onComplete(loadLocalProfile(context))
            return
        }

        Log.d(TAG, "Fetching profile users/$uid")
        FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
                    val profile = parseSnapshotToUserProfile(snapshot, context, currentUser)
                    saveProfileLocally(context, profile)
                    onComplete(profile)
                } else {
                    ensureUserProfileExists(context, onComplete)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching users/$uid: ${e.localizedMessage}", e)
                onComplete(loadLocalProfile(context))
            }
    }

    private fun parseSnapshotToUserProfile(
        snapshot: DocumentSnapshot,
        context: Context,
        currentUser: FirebaseUser?
    ): UserProfile {
        val uid = snapshot.getString("uid") ?: currentUser?.uid ?: getOrCreateUserId(context)
        var name = snapshot.getString("name") ?: ""
        if (name.isBlank() && currentUser?.displayName != null) name = currentUser.displayName ?: ""

        var email = snapshot.getString("email") ?: ""
        if (email.isBlank() && currentUser?.email != null) email = currentUser.email ?: ""

        var photoUrl = snapshot.getString("photoUrl") ?: ""
        if (photoUrl.isBlank() && currentUser?.photoUrl != null) photoUrl = currentUser.photoUrl.toString()

        val role = if (isAdminEmail(email)) "admin" else (snapshot.getString("role") ?: "user")

        val resumeMap = snapshot.get("resume") as? Map<*, *>
        val resumeFileName = resumeMap?.get("fileName") as? String ?: snapshot.getString("resumeFileName") ?: ""
        val resumeStoragePath = resumeMap?.get("storagePath") as? String ?: snapshot.getString("resumeStoragePath") ?: ""
        val resumeDownloadUrl = resumeMap?.get("downloadUrl") as? String ?: snapshot.getString("resumeDownloadUrl") ?: ""
        val resumeContentType = resumeMap?.get("contentType") as? String ?: snapshot.getString("resumeContentType") ?: ""
        val resumeFileSize = (resumeMap?.get("fileSize") as? Number)?.toLong() ?: snapshot.getLong("resumeFileSize") ?: 0L
        val resumeVersion = (resumeMap?.get("version") as? Number)?.toInt() ?: snapshot.getLong("resumeVersion")?.toInt() ?: 1
        val resumeUpdatedAt = resumeMap?.get("updatedAt")?.toString() ?: snapshot.getString("resumeUpdatedAt") ?: ""

        return UserProfile(
            id = uid,
            name = name,
            email = email,
            phone = snapshot.getString("phone") ?: "",
            photoUrl = photoUrl,
            headline = snapshot.getString("headline") ?: "",
            jobRole = snapshot.getString("jobRole") ?: "",
            skills = snapshot.getString("skills") ?: "",
            location = snapshot.getString("location") ?: "",
            openToWork = snapshot.getBoolean("openToWork") ?: true,
            bio = snapshot.getString("bio") ?: "",
            about = snapshot.getString("about") ?: "",
            role = role,
            dob = snapshot.getString("dob") ?: "",
            linkedInUrl = snapshot.getString("linkedInUrl") ?: "",
            githubUrl = snapshot.getString("githubUrl") ?: "",
            portfolioUrl = snapshot.getString("portfolioUrl") ?: "",
            websiteUrl = snapshot.getString("websiteUrl") ?: "",
            resumeFileName = resumeFileName,
            resumeStoragePath = resumeStoragePath,
            resumeDownloadUrl = resumeDownloadUrl,
            resumeContentType = resumeContentType,
            resumeFileSize = resumeFileSize,
            resumeData = snapshot.getString("resumeData") ?: "",
            resumeUpdatedAt = resumeUpdatedAt,
            resumeVersion = resumeVersion,
            certifications = snapshot.getString("certifications") ?: "",
            education = snapshot.getString("education") ?: "",
            experience = snapshot.getString("experience") ?: "",
            projects = snapshot.getString("projects") ?: "",
            languages = snapshot.getString("languages") ?: "",
            preferences = snapshot.getString("preferences") ?: "",
            careerPreferences = snapshot.getString("careerPreferences") ?: "",
            privacySettings = snapshot.getString("privacySettings") ?: "",
            lastOpenedAt = snapshot.getLong("lastOpenedAt") ?: 0L
        )
    }

    @JvmStatic
    fun saveProfileToFirebase(
        context: Context,
        profile: UserProfile,
        onResult: (Boolean, String) -> Unit
    ) {
        saveProfileLocally(context, profile)
        val uid = getCurrentUid() ?: profile.id.ifBlank { null }
        if (uid.isNullOrBlank()) {
            onResult(false, "User not authenticated")
            return
        }

        val resolvedRole = if (isAdminEmail(profile.email)) "admin" else profile.role

        val data = mutableMapOf<String, Any>(
            "uid" to uid,
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "photoUrl" to profile.photoUrl,
            "headline" to profile.headline,
            "jobRole" to profile.jobRole,
            "skills" to profile.skills,
            "location" to profile.location,
            "openToWork" to profile.openToWork,
            "bio" to profile.bio,
            "about" to profile.about,
            "role" to resolvedRole,
            "dob" to profile.dob,
            "linkedInUrl" to profile.linkedInUrl,
            "githubUrl" to profile.githubUrl,
            "portfolioUrl" to profile.portfolioUrl,
            "websiteUrl" to profile.websiteUrl,
            "certifications" to profile.certifications,
            "education" to profile.education,
            "experience" to profile.experience,
            "projects" to profile.projects,
            "languages" to profile.languages,
            "preferences" to profile.preferences,
            "careerPreferences" to profile.careerPreferences,
            "privacySettings" to profile.privacySettings,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (profile.resumeFileName.isNotBlank() || profile.resumeStoragePath.isNotBlank()) {
            data["resume"] = mapOf(
                "fileName" to profile.resumeFileName,
                "storagePath" to profile.resumeStoragePath,
                "downloadUrl" to profile.resumeDownloadUrl,
                "contentType" to profile.resumeContentType,
                "fileSize" to profile.resumeFileSize,
                "updatedAt" to FieldValue.serverTimestamp(),
                "version" to profile.resumeVersion
            )
        }

        Log.d(TAG, "Saving profile users/$uid to Firestore")
        FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Successfully saved profile users/$uid to Firestore")
                onResult(true, "")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save users/$uid to Firestore: ${e.localizedMessage}", e)
                onResult(false, e.localizedMessage ?: "Failed to save profile")
            }
    }

    @JvmStatic
    fun saveResumeMetadataToFirebase(
        context: Context,
        profile: UserProfile,
        fileName: String,
        storagePath: String,
        downloadUrl: String,
        contentType: String,
        fileSize: Long,
        newVersion: Int,
        resumeData: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = getCurrentUid() ?: profile.id.ifBlank { null }
        if (uid.isNullOrBlank()) {
            onResult(false, "User not authenticated")
            return
        }

        val updatedProfile = profile.copy(
            resumeFileName = fileName,
            resumeStoragePath = storagePath,
            resumeDownloadUrl = downloadUrl,
            resumeContentType = contentType,
            resumeFileSize = fileSize,
            resumeVersion = newVersion,
            resumeData = resumeData.ifBlank { profile.resumeData },
            resumeUpdatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        saveProfileLocally(context, updatedProfile)

        val resumeMap = mapOf(
            "fileName" to fileName,
            "storagePath" to storagePath,
            "downloadUrl" to downloadUrl,
            "contentType" to contentType,
            "fileSize" to fileSize,
            "uploadedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "version" to newVersion
        )

        val updateData = mutableMapOf<String, Any>(
            "uid" to uid,
            "resume" to resumeMap,
            "resumeFileName" to fileName,
            "resumeStoragePath" to storagePath,
            "resumeDownloadUrl" to downloadUrl,
            "resumeContentType" to contentType,
            "resumeFileSize" to fileSize,
            "resumeVersion" to newVersion,
            "resumeUpdatedAt" to updatedProfile.resumeUpdatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (updatedProfile.resumeData.isNotBlank()) {
            updateData["resumeData"] = updatedProfile.resumeData
        }

        Log.d(TAG, "Saving resume metadata users/$uid/resume to Firestore")
        FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .set(updateData as Map<String, Any>, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Successfully saved resume metadata users/$uid to Firestore")
                onResult(true, "")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save resume metadata: ${e.localizedMessage}", e)
                onResult(false, e.localizedMessage ?: "Failed to save resume metadata")
            }
    }

    @JvmStatic
    fun deleteResumeMetadataFromFirebase(
        context: Context,
        profile: UserProfile,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = getCurrentUid() ?: profile.id.ifBlank { null }
        if (uid.isNullOrBlank()) {
            onResult(false, "User not authenticated")
            return
        }

        val updatedProfile = profile.copy(
            resumeFileName = "",
            resumeStoragePath = "",
            resumeDownloadUrl = "",
            resumeContentType = "",
            resumeFileSize = 0L,
            resumeUpdatedAt = ""
        )
        saveProfileLocally(context, updatedProfile)

        val updateData = mapOf(
            "resume" to FieldValue.delete(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance().collection(COLLECTION_USERS).document(uid)
            .update(updateData)
            .addOnSuccessListener {
                onResult(true, "")
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage ?: "Failed to delete resume metadata")
            }
    }
}
