package co.median.android

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class PendingJob(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val jobType: String = "",
    val experience: String = "",
    val salary: String = "",
    val description: String = "",
    val requiredSkills: List<String> = emptyList(),
    val preferredSkills: List<String> = emptyList(),
    val applicationUrl: String = "",
    val source: String = "",
    val sourceId: String = "",
    val sourceJobId: String = "",
    val status: String = "PENDING_APPROVAL",
    val postedDate: String = "",
    val createdAt: Long = 0L,
    val rejectionReason: String = ""
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): PendingJob {
            val id = doc.id
            val title = doc.getString("title") ?: doc.getString("jobTitle") ?: doc.getString("name") ?: ""
            
            var company = doc.getString("company") ?: doc.getString("companyName") ?: doc.getString("employer") ?: ""
            val applicationUrl = doc.getString("applicationUrl") ?: doc.getString("url") ?: doc.getString("link") ?: doc.getString("applyUrl") ?: ""
            if (company.isBlank()) {
                company = extractCompanyFromUrl(applicationUrl)
            }

            val isRemote = doc.getBoolean("isRemote") ?: doc.getBoolean("remote") ?: false
            val rawLocation = doc.getString("location") ?: doc.getString("jobLocation") ?: ""
            val location = rawLocation.ifBlank { if (isRemote) "Remote" else "On-site" }

            val jobType = doc.getString("jobType") ?: doc.getString("type") ?: "Full Time"
            val experience = doc.getString("experience") ?: "Not Specified"
            val salary = doc.getString("salary") ?: "Not Disclosed"
            val description = doc.getString("description") ?: doc.getString("jd") ?: ""
            val source = doc.getString("source") ?: "Manual/LinkedIn"
            val sourceId = doc.getString("sourceId") ?: ""
            val sourceJobId = doc.getString("sourceJobId") ?: ""
            val status = doc.getString("status") ?: "PENDING_APPROVAL"
            val postedDate = doc.getString("postedDate") ?: "Recently posted"
            val rejectionReason = doc.getString("rejectionReason") ?: ""

            val reqSkills = doc.get("requiredSkills") ?: doc.get("skills")
            val reqSkillsList = when (reqSkills) {
                is List<*> -> reqSkills.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                is String -> reqSkills.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            val prefSkills = doc.get("preferredSkills")
            val prefSkillsList = when (prefSkills) {
                is List<*> -> prefSkills.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
                is String -> prefSkills.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            val createdAtLong = when (val createdAtObj = doc.get("createdAt")) {
                is Timestamp -> createdAtObj.toDate().time
                is Long -> createdAtObj
                is Double -> createdAtObj.toLong()
                is Date -> createdAtObj.time
                is String -> createdAtObj.toLongOrNull() ?: 0L
                else -> {
                    try {
                        val method = createdAtObj?.javaClass?.getMethod("toDate")
                        val date = method?.invoke(createdAtObj) as? Date
                        date?.time ?: 0L
                    } catch (_: Exception) {
                        0L
                    }
                }
            }

            return PendingJob(
                id = id,
                title = title,
                company = company,
                location = location,
                jobType = jobType,
                experience = experience,
                salary = salary,
                description = description,
                requiredSkills = reqSkillsList,
                preferredSkills = prefSkillsList,
                applicationUrl = applicationUrl,
                source = source,
                sourceId = sourceId,
                sourceJobId = sourceJobId,
                status = status,
                postedDate = postedDate,
                createdAt = createdAtLong,
                rejectionReason = rejectionReason
            )
        }

        private fun extractCompanyFromUrl(url: String): String {
            if (url.isBlank()) return "Tech Company"
            return try {
                val host = java.net.URL(url).host.lowercase()
                when {
                    host.contains("linkedin") -> "LinkedIn"
                    host.contains("greenhouse") -> "Greenhouse"
                    host.contains("lever") -> "Lever"
                    host.contains("ashby") -> "Ashby"
                    else -> {
                        val parts = host.replace("www.", "").split(".")
                        if (parts.isNotEmpty() && parts[0].length > 2) {
                            parts[0].replaceFirstChar { it.uppercase() }
                        } else {
                            "Tech Company"
                        }
                    }
                }
            } catch (_: Exception) {
                "Tech Company"
            }
        }
    }
}

data class JobSource(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val boardName: String = "",
    val companyName: String = "",
    val organization: String = "",
    val company: String = "",
    val enabled: Boolean = true,
    val lastFetchCount: Int = 0,
    val lastFetchStatus: String = "PENDING",
    val lastError: String = ""
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): JobSource {
            return JobSource(
                id = doc.id,
                name = doc.getString("name") ?: doc.id,
                type = doc.getString("type") ?: "greenhouse",
                boardName = doc.getString("boardName") ?: "",
                companyName = doc.getString("companyName") ?: "",
                organization = doc.getString("organization") ?: "",
                company = doc.getString("company") ?: "",
                enabled = doc.getBoolean("enabled") ?: true,
                lastFetchCount = doc.getLong("lastFetchCount")?.toInt() ?: 0,
                lastFetchStatus = doc.getString("lastFetchStatus") ?: "PENDING",
                lastError = doc.getString("lastError") ?: ""
            )
        }
    }
}
