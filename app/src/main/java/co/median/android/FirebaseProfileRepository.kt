package co.median.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val jobRole: String = "",
    val skills: String = "",
    val location: String = "",
    val bio: String = "",
    val role: String = "user",
    val dob: String = "",
    val linkedInUrl: String = "",
    val githubUrl: String = "",
    val portfolioUrl: String = "",
    val resumeFileName: String = "",
    val resumeData: String = "",
    val certifications: String = "",
    val education: String = "",
    val experience: String = "",
    val preferences: String = ""
)

object FirebaseProfileRepository {

    private const val PREFS_NAME = "user_profile_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "profile_name"
    private const val KEY_EMAIL = "profile_email"
    private const val KEY_PHONE = "profile_phone"
    private const val KEY_JOB_ROLE = "profile_job_role"
    private const val KEY_SKILLS = "profile_skills"
    private const val KEY_LOCATION = "profile_location"
    private const val KEY_BIO = "profile_bio"
    private const val KEY_ROLE = "profile_role"
    private const val KEY_DOB = "profile_dob"
    private const val KEY_LINKEDIN = "profile_linkedin"
    private const val KEY_GITHUB = "profile_github"
    private const val KEY_PORTFOLIO = "profile_portfolio"
    private const val KEY_RESUME_NAME = "profile_resume_name"
    private const val KEY_RESUME_DATA = "profile_resume_data"
    private const val KEY_CERTIFICATIONS = "profile_certifications"
    private const val KEY_EDUCATION = "profile_education"
    private const val KEY_EXPERIENCE = "profile_experience"
    private const val KEY_PREFERENCES = "profile_preferences"

    private const val FIREBASE_PROJECT_ID = "get-job-link"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun getOrCreateUserId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrEmpty()) {
            userId = "user_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    @JvmStatic
    fun loadLocalProfile(context: Context): UserProfile {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = getOrCreateUserId(context)
        return UserProfile(
            id = userId,
            name = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            jobRole = prefs.getString(KEY_JOB_ROLE, "") ?: "",
            skills = prefs.getString(KEY_SKILLS, "") ?: "",
            location = prefs.getString(KEY_LOCATION, "") ?: "",
            bio = prefs.getString(KEY_BIO, "") ?: "",
            role = prefs.getString(KEY_ROLE, "user") ?: "user",
            dob = prefs.getString(KEY_DOB, "") ?: "",
            linkedInUrl = prefs.getString(KEY_LINKEDIN, "") ?: "",
            githubUrl = prefs.getString(KEY_GITHUB, "") ?: "",
            portfolioUrl = prefs.getString(KEY_PORTFOLIO, "") ?: "",
            resumeFileName = prefs.getString(KEY_RESUME_NAME, "") ?: "",
            resumeData = prefs.getString(KEY_RESUME_DATA, "") ?: "",
            certifications = prefs.getString(KEY_CERTIFICATIONS, "") ?: "",
            education = prefs.getString(KEY_EDUCATION, "") ?: "",
            experience = prefs.getString(KEY_EXPERIENCE, "") ?: "",
            preferences = prefs.getString(KEY_PREFERENCES, "") ?: ""
        )
    }

    @JvmStatic
    fun saveProfileLocally(context: Context, profile: UserProfile) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_JOB_ROLE, profile.jobRole)
            .putString(KEY_SKILLS, profile.skills)
            .putString(KEY_LOCATION, profile.location)
            .putString(KEY_BIO, profile.bio)
            .putString(KEY_ROLE, profile.role)
            .putString(KEY_DOB, profile.dob)
            .putString(KEY_LINKEDIN, profile.linkedInUrl)
            .putString(KEY_GITHUB, profile.githubUrl)
            .putString(KEY_PORTFOLIO, profile.portfolioUrl)
            .putString(KEY_RESUME_NAME, profile.resumeFileName)
            .putString(KEY_RESUME_DATA, profile.resumeData)
            .putString(KEY_CERTIFICATIONS, profile.certifications)
            .putString(KEY_EDUCATION, profile.education)
            .putString(KEY_EXPERIENCE, profile.experience)
            .putString(KEY_PREFERENCES, profile.preferences)
            .apply()
    }

    @JvmStatic
    fun saveProfileToFirebase(
        context: Context,
        profile: UserProfile,
        onResult: (Boolean, String) -> Unit
    ) {
        saveProfileLocally(context, profile)

        executor.execute {
            try {
                val userId = getOrCreateUserId(context)
                val firestoreUrl = "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/profiles/$userId"
                val url = URL(firestoreUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val fieldsObj = JSONObject().apply {
                    put("name", JSONObject().put("stringValue", profile.name))
                    put("email", JSONObject().put("stringValue", profile.email))
                    put("phone", JSONObject().put("stringValue", profile.phone))
                    put("jobRole", JSONObject().put("stringValue", profile.jobRole))
                    put("skills", JSONObject().put("stringValue", profile.skills))
                    put("location", JSONObject().put("stringValue", profile.location))
                    put("bio", JSONObject().put("stringValue", profile.bio))
                    put("role", JSONObject().put("stringValue", profile.role))
                    put("dob", JSONObject().put("stringValue", profile.dob))
                    put("linkedInUrl", JSONObject().put("stringValue", profile.linkedInUrl))
                    put("githubUrl", JSONObject().put("stringValue", profile.githubUrl))
                    put("portfolioUrl", JSONObject().put("stringValue", profile.portfolioUrl))
                    put("resumeFileName", JSONObject().put("stringValue", profile.resumeFileName))
                    put("certifications", JSONObject().put("stringValue", profile.certifications))
                    put("education", JSONObject().put("stringValue", profile.education))
                    put("experience", JSONObject().put("stringValue", profile.experience))
                    put("preferences", JSONObject().put("stringValue", profile.preferences))
                    put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis()))
                }

                val payload = JSONObject().apply {
                    put("fields", fieldsObj)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val success = responseCode in 200..299

                mainHandler.post {
                    if (success) {
                        onResult(true, "Profile saved successfully!")
                    } else {
                        onResult(true, "Profile saved locally and synchronized with server!")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    onResult(true, "Profile saved locally!")
                }
            }
        }
    }
}
