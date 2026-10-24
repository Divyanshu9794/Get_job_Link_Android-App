package co.median.android

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val jobType: String = "",
    val experience: String = "",
    val salary: String = "",
    val description: String = "",
    val skills: List<String> = emptyList(),
    val requiredSkills: List<String> = emptyList(),
    val preferredSkills: List<String> = emptyList(),
    val applicationUrl: String = "",
    val postedDate: String = "",
    val createdAt: Long = 0L,
    val source: String = "",
    val logo: String = ""
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Job {
            val id = doc.id
            val title = doc.getString("title")
                ?: doc.getString("jobTitle")
                ?: doc.getString("name")
                ?: ""

            val company = doc.getString("company")
                ?: doc.getString("companyName")
                ?: doc.getString("employer")
                ?: ""

            val location = doc.getString("location")
                ?: doc.getString("jobLocation")
                ?: ""

            val jobType = doc.getString("jobType")
                ?: doc.getString("type")
                ?: doc.getString("employmentType")
                ?: ""

            val experience = doc.getString("experience")
                ?: doc.getString("experienceLevel")
                ?: ""

            val salary = doc.getString("salary")
                ?: doc.getString("compensation")
                ?: doc.getString("pay")
                ?: ""

            val description = doc.getString("description")
                ?: doc.getString("jobDescription")
                ?: doc.getString("details")
                ?: ""

            val applicationUrl = doc.getString("applicationUrl")
                ?: doc.getString("url")
                ?: doc.getString("link")
                ?: doc.getString("applyUrl")
                ?: ""

            val source = doc.getString("source") ?: ""
            val logo = doc.getString("logo") ?: doc.getString("companyLogo") ?: ""

            // Skills parsing
            val skillsList = parseSkillsList(doc, "skills")
            val reqSkillsList = parseSkillsList(doc, "requiredSkills")
            val prefSkillsList = parseSkillsList(doc, "preferredSkills")
            val combinedSkills = (skillsList + reqSkillsList).distinct()

            // Date & Timestamp parsing
            val (createdAt, postedDate) = parseJobDate(doc)

            return Job(
                id = id,
                title = title,
                company = company,
                location = location,
                jobType = jobType,
                experience = experience,
                salary = salary,
                description = description,
                skills = combinedSkills,
                requiredSkills = reqSkillsList.ifEmpty { combinedSkills },
                preferredSkills = prefSkillsList,
                applicationUrl = applicationUrl,
                postedDate = postedDate,
                createdAt = createdAt,
                source = source,
                logo = logo
            )
        }

        private fun parseSkillsList(doc: DocumentSnapshot, fieldName: String): List<String> {
            val raw = doc.get(fieldName) ?: return emptyList()
            if (raw is List<*>) {
                return raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            } else if (raw is String) {
                return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            return emptyList()
        }

        private fun parseJobDate(doc: DocumentSnapshot): Pair<Long, String> {
            val dateFields = listOf("createdAt", "postedDate", "publishedAt", "timestamp", "datePosted")
            for (field in dateFields) {
                val valObj = doc.get(field) ?: continue
                when (valObj) {
                    is Timestamp -> {
                        val millis = valObj.toDate().time
                        val formatted = formatRelativeDate(millis)
                        return Pair(millis, formatted)
                    }
                    is Long -> {
                        val formatted = formatRelativeDate(valObj)
                        return Pair(valObj, formatted)
                    }
                    is Double -> {
                        val millis = valObj.toLong()
                        val formatted = formatRelativeDate(millis)
                        return Pair(millis, formatted)
                    }
                    is Date -> {
                        val millis = valObj.time
                        val formatted = formatRelativeDate(millis)
                        return Pair(millis, formatted)
                    }
                    is String -> {
                        val trimmed = valObj.trim()
                        val parsedMillis = parseDateStringToMillis(trimmed)
                        if (parsedMillis > 0L) {
                            return Pair(parsedMillis, trimmed)
                        } else if (trimmed.isNotBlank()) {
                            return Pair(0L, trimmed)
                        }
                    }
                }
            }
            return Pair(0L, "Recently posted")
        }

        private fun parseDateStringToMillis(dateStr: String): Long {
            if (dateStr.isBlank()) return 0L
            dateStr.toLongOrNull()?.let { if (it > 1000000000L) return it }

            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "dd MMM yyyy",
                "MM/dd/yyyy"
            )
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
            return 0L
        }

        private fun formatRelativeDate(millis: Long): String {
            if (millis <= 0L) return "Recently posted"
            val now = System.currentTimeMillis()
            val diff = now - millis
            val minutes = diff / (60 * 1000L)
            val hours = diff / (60 * 60 * 1000L)
            val days = diff / (24 * 60 * 60 * 1000L)

            return when {
                minutes < 60 -> "Posted ${maxOf(1, minutes)} mins ago"
                hours < 24 -> "Posted $hours hours ago"
                days == 1L -> "Posted yesterday"
                days < 30 -> "Posted $days days ago"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                    "Posted " + sdf.format(Date(millis))
                }
            }
        }
    }
}
