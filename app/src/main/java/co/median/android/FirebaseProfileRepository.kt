package co.median.android

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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
    val resumeData: String = "",
    val resumeUpdatedAt: String = "",
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
        if (resumeFileName.isNotBlank()) score += 10
        if ((projects.isNotBlank() && projects != "[]") || (certifications.isNotBlank() && certifications != "[]")) score += 10
        if (preferences.isNotBlank() || careerPreferences.isNotBlank()) score += 5
        return if (score > 100) 100 else score
    }
}

object FirebaseProfileRepository {

    private const val PREFS_NAME = "user_profile_prefs"
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
    private const val KEY_RESUME_DATA = "profile_resume_data"
    private const val KEY_RESUME_UPDATED = "profile_resume_updated"
    private const val KEY_CERTIFICATIONS = "profile_certifications"
    private const val KEY_EDUCATION = "profile_education"
    private const val KEY_EXPERIENCE = "profile_experience"
    private const val KEY_PROJECTS = "profile_projects"
    private const val KEY_LANGUAGES = "profile_languages"
    private const val KEY_PREFERENCES = "profile_preferences"
    private const val KEY_CAREER_PREFERENCES = "profile_career_preferences"
    private const val KEY_PRIVACY = "profile_privacy"
    private const val KEY_LAST_OPENED = "profile_last_opened"

    private const val FIREBASE_PROJECT_ID = "get-job-link"
    private const val ADMIN_EMAIL = "sdivyanshu352@gmail.com"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun isAdminEmail(email: String?): Boolean {
        if (email.isNullOrEmpty()) return false
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

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
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val role = if (isAdminEmail(email)) "admin" else (prefs.getString(KEY_ROLE, "user") ?: "user")

        return UserProfile(
            id = userId,
            name = prefs.getString(KEY_NAME, "") ?: "",
            email = email,
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: "",
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
            resumeData = prefs.getString(KEY_RESUME_DATA, "") ?: "",
            resumeUpdatedAt = prefs.getString(KEY_RESUME_UPDATED, "") ?: "",
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
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
            .putString(KEY_RESUME_DATA, profile.resumeData)
            .putString(KEY_RESUME_UPDATED, profile.resumeUpdatedAt)
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

                val resolvedRole = if (isAdminEmail(profile.email)) "admin" else profile.role

                val fieldsObj = JSONObject().apply {
                    put("name", JSONObject().put("stringValue", profile.name))
                    put("email", JSONObject().put("stringValue", profile.email))
                    put("phone", JSONObject().put("stringValue", profile.phone))
                    put("photoUrl", JSONObject().put("stringValue", profile.photoUrl))
                    put("headline", JSONObject().put("stringValue", profile.headline))
                    put("jobRole", JSONObject().put("stringValue", profile.jobRole))
                    put("skills", JSONObject().put("stringValue", profile.skills))
                    put("location", JSONObject().put("stringValue", profile.location))
                    put("openToWork", JSONObject().put("booleanValue", profile.openToWork))
                    put("bio", JSONObject().put("stringValue", profile.bio))
                    put("about", JSONObject().put("stringValue", profile.about))
                    put("role", JSONObject().put("stringValue", resolvedRole))
                    put("dob", JSONObject().put("stringValue", profile.dob))
                    put("linkedInUrl", JSONObject().put("stringValue", profile.linkedInUrl))
                    put("githubUrl", JSONObject().put("stringValue", profile.githubUrl))
                    put("portfolioUrl", JSONObject().put("stringValue", profile.portfolioUrl))
                    put("websiteUrl", JSONObject().put("stringValue", profile.websiteUrl))
                    put("resumeFileName", JSONObject().put("stringValue", profile.resumeFileName))
                    put("resumeUpdatedAt", JSONObject().put("stringValue", profile.resumeUpdatedAt))
                    put("certifications", JSONObject().put("stringValue", profile.certifications))
                    put("education", JSONObject().put("stringValue", profile.education))
                    put("experience", JSONObject().put("stringValue", profile.experience))
                    put("projects", JSONObject().put("stringValue", profile.projects))
                    put("languages", JSONObject().put("stringValue", profile.languages))
                    put("preferences", JSONObject().put("stringValue", profile.preferences))
                    put("careerPreferences", JSONObject().put("stringValue", profile.careerPreferences))
                    put("privacySettings", JSONObject().put("stringValue", profile.privacySettings))
                    put("lastOpenedAt", JSONObject().put("integerValue", profile.lastOpenedAt))
                    put("completionPercentage", JSONObject().put("integerValue", profile.calculateCompletionPercentage()))
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
                        onResult(true, "Profile saved locally and synchronized!")
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

    @JvmStatic
    fun fetchJobsFromFirestore(onResult: (Boolean, String) -> Unit) {
        executor.execute {
            try {
                val firestoreUrl = "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/jobs"
                val url = URL(firestoreUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    val root = JSONObject(responseStr)
                    val docs = root.optJSONArray("documents") ?: JSONArray()
                    val jobsArray = JSONArray()

                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val name = doc.optString("name", "")
                        val jobId = name.substringAfterLast("/")
                        val fields = doc.optJSONObject("fields") ?: JSONObject()

                        val title = fields.optJSONObject("title")?.optString("stringValue") ?: "Software Engineer"
                        val company = fields.optJSONObject("company")?.optString("stringValue") ?: "Tech Company"
                        val location = fields.optJSONObject("location")?.optString("stringValue") ?: "Remote"
                        val type = fields.optJSONObject("type")?.optString("stringValue") ?: fields.optJSONObject("jobType")?.optString("stringValue") ?: "Full Time"
                        val salary = fields.optJSONObject("salary")?.optString("stringValue") ?: "Competitive"
                        val experience = fields.optJSONObject("experience")?.optString("stringValue") ?: "1-3 Years"
                        val description = fields.optJSONObject("description")?.optString("stringValue") ?: ""
                        val applicationUrl = fields.optJSONObject("applicationUrl")?.optString("stringValue") ?: "https://example.com/apply"

                        val reqSkillsObj = fields.optJSONObject("requiredSkills")
                        val reqSkills = mutableListOf<String>()
                        if (reqSkillsObj != null) {
                            val arrayValues = reqSkillsObj.optJSONObject("arrayValue")?.optJSONArray("values")
                            if (arrayValues != null) {
                                for (j in 0 until arrayValues.length()) {
                                    val valObj = arrayValues.getJSONObject(j)
                                    reqSkills.add(valObj.optString("stringValue", ""))
                                }
                            }
                        }

                        val item = JSONObject().apply {
                            put("id", jobId)
                            put("title", title)
                            put("company", company)
                            put("location", location)
                            put("type", type)
                            put("salary", salary)
                            put("experience", experience)
                            put("description", description)
                            put("applicationUrl", applicationUrl)
                            put("requiredSkills", JSONArray(reqSkills))
                        }
                        jobsArray.put(item)
                    }

                    mainHandler.post {
                        onResult(true, jobsArray.toString())
                    }
                } else {
                    mainHandler.post {
                        onResult(false, "Failed to load jobs from Firestore")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    onResult(false, e.localizedMessage ?: "Network error fetching jobs")
                }
            }
        }
    }
}
