package co.median.android

import org.json.JSONArray
import org.json.JSONObject

data class JobMatchResult(
    val matchPercentage: Int,
    val matchedCriteria: List<String>,
    val missingCriteria: List<String>,
    val isUnavailable: Boolean = false,
    val unavailabilityReason: String = ""
)

object ProfileMatchingHelper {

    fun parseUserSkills(skillsRaw: String): List<String> {
        if (skillsRaw.isBlank()) return emptyList()
        val skills = mutableListOf<String>()
        val trimmed = skillsRaw.trim()
        if (trimmed.startsWith("[")) {
            try {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.get(i)
                    if (item is JSONObject) {
                        val name = item.optString("skill", "").ifBlank { item.optString("name", "") }
                        if (name.isNotBlank()) skills.add(SkillLibrary.normalize(name))
                    } else if (item is String) {
                        if (item.isNotBlank()) skills.add(SkillLibrary.normalize(item))
                    }
                }
            } catch (_: Exception) {
                trimmed.split(",").map { SkillLibrary.normalize(it) }.filter { it.isNotBlank() }.forEach { skills.add(it) }
            }
        } else {
            trimmed.split(",").map { SkillLibrary.normalize(it) }.filter { it.isNotBlank() }.forEach { skills.add(it) }
        }
        return skills.distinct()
    }

    fun calculateMatch(profile: UserProfile, job: Job): JobMatchResult {
        val userSkills = parseUserSkills(profile.skills).map { it.lowercase() }

        val jobSkills = (job.requiredSkills + job.skills)
            .map { SkillLibrary.normalize(it).lowercase() }
            .filter { it.isNotBlank() }

        val matchedSkills = userSkills.filter { jobSkills.contains(it) }
        val missingSkills = jobSkills.filter { !userSkills.contains(it) }

        val matchedCriteria = mutableListOf<String>()
        val missingCriteria = mutableListOf<String>()

        if (matchedSkills.isNotEmpty()) {
            matchedCriteria.add("Skills (${matchedSkills.size} matched)")
        } else if (jobSkills.isNotEmpty()) {
            missingCriteria.add("Skills (${missingSkills.size} missing)")
        }

        // Location match
        if (profile.location.isNotBlank() && job.location.isNotBlank()) {
            if (job.location.contains(profile.location, true) || job.location.contains("Remote", true) || profile.location.contains("Remote", true)) {
                matchedCriteria.add("Location / Work Mode")
            } else {
                missingCriteria.add("Location (${job.location})")
            }
        } else {
            matchedCriteria.add("Location")
        }

        // Role / Title match
        if (profile.jobRole.isNotBlank() || profile.headline.isNotBlank()) {
            val roleTarget = (profile.jobRole + " " + profile.headline).lowercase()
            if (roleTarget.contains(job.title.lowercase()) || job.title.lowercase().contains(profile.jobRole.lowercase())) {
                matchedCriteria.add("Role Match")
            } else {
                matchedCriteria.add("Career Interest")
            }
        } else {
            matchedCriteria.add("Experience Level")
        }

        // Base calculation
        var percentage = 60
        if (jobSkills.isNotEmpty()) {
            val skillRatio = matchedSkills.size.toFloat() / jobSkills.size.toFloat()
            percentage = (40 + (skillRatio * 55)).toInt()
        } else {
            percentage = 85
        }

        if (percentage > 98) percentage = 98
        if (percentage < 45) percentage = 45

        return JobMatchResult(
            matchPercentage = percentage,
            matchedCriteria = matchedCriteria,
            missingCriteria = missingSkills.take(3)
        )
    }

    fun calculateResumeMatch(profile: UserProfile, job: Job): JobMatchResult {
        val hasResume = profile.resumeFileName.isNotBlank() || profile.resumeData.isNotBlank() || profile.skills.isNotBlank()
        if (!hasResume) {
            return JobMatchResult(
                matchPercentage = 0,
                matchedCriteria = emptyList(),
                missingCriteria = emptyList(),
                isUnavailable = true,
                unavailabilityReason = "Resume unavailable. Please upload your resume from Profile."
            )
        }

        val jobText = (job.title + " " + job.description + " " + job.requiredSkills.joinToString(" ") + " " + job.skills.joinToString(" ")).trim()
        if (jobText.length < 20 || (job.requiredSkills.isEmpty() && job.skills.isEmpty() && job.description.length < 30)) {
            return JobMatchResult(
                matchPercentage = 0,
                matchedCriteria = emptyList(),
                missingCriteria = emptyList(),
                isUnavailable = true,
                unavailabilityReason = "Match score unavailable. This job does not contain enough requirements to calculate a meaningful match score."
            )
        }

        val resumeText = (profile.resumeData + " " + profile.skills + " " + profile.about + " " + profile.experience + " " + profile.projects).lowercase()

        val userResumeSkills = mutableSetOf<String>()
        for (skill in SkillLibrary.allSkills) {
            val normalized = skill.lowercase()
            if (hasTokenMatch(resumeText, normalized)) {
                userResumeSkills.add(SkillLibrary.normalize(skill))
            }
        }
        for (skill in parseUserSkills(profile.skills)) {
            userResumeSkills.add(SkillLibrary.normalize(skill))
        }

        val userSkillsLower = userResumeSkills.map { it.lowercase() }.toSet()

        val requiredJobSkills = (job.requiredSkills.ifEmpty { job.skills })
            .map { SkillLibrary.normalize(it) }
            .filter { it.isNotBlank() }

        val preferredJobSkills = job.preferredSkills
            .map { SkillLibrary.normalize(it) }
            .filter { it.isNotBlank() }

        val allJobSkills = (requiredJobSkills + preferredJobSkills).distinct()

        if (allJobSkills.isEmpty()) {
            return JobMatchResult(
                matchPercentage = 75,
                matchedCriteria = userResumeSkills.toList(),
                missingCriteria = emptyList()
            )
        }

        val matchedRequired = requiredJobSkills.filter { userSkillsLower.contains(it.lowercase()) }
        val missingRequired = requiredJobSkills.filter { !userSkillsLower.contains(it.lowercase()) }

        val matchedPreferred = preferredJobSkills.filter { userSkillsLower.contains(it.lowercase()) }
        val missingPreferred = preferredJobSkills.filter { !userSkillsLower.contains(it.lowercase()) }

        val matchedAll = (matchedRequired + matchedPreferred).distinct()
        val missingAll = (missingRequired + missingPreferred).distinct()

        var percentage: Int
        if (requiredJobSkills.isNotEmpty() && preferredJobSkills.isNotEmpty()) {
            val reqRatio = matchedRequired.size.toFloat() / requiredJobSkills.size.toFloat()
            val prefRatio = matchedPreferred.size.toFloat() / preferredJobSkills.size.toFloat()
            percentage = ((reqRatio * 70) + (prefRatio * 30)).toInt()
        } else if (allJobSkills.isNotEmpty()) {
            val ratio = matchedAll.size.toFloat() / allJobSkills.size.toFloat()
            percentage = (ratio * 100).toInt()
        } else {
            percentage = 70
        }

        if (percentage < 30 && matchedAll.isNotEmpty()) percentage = 40
        if (percentage > 98) percentage = 98
        if (percentage < 10) percentage = 15

        return JobMatchResult(
            matchPercentage = percentage,
            matchedCriteria = matchedAll,
            missingCriteria = missingAll
        )
    }

    private fun hasTokenMatch(text: String, skill: String): Boolean {
        val regex = "\\b${Regex.escape(skill)}\\b".toRegex(RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }
}
