package co.median.android

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

object JobFetchEngine {
    private const val TAG = "JobFetchEngine"

    suspend fun fetchAndStoreJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        var totalDiscovered = 0
        var totalAdded = 0
        var totalDuplicates = 0

        val defaults = listOf(
            mapOf("name" to "Greenhouse - Stripe", "type" to "greenhouse", "boardName" to "stripe", "enabled" to true),
            mapOf("name" to "Greenhouse - Airbnb", "type" to "greenhouse", "boardName" to "airbnb", "enabled" to true),
            mapOf("name" to "Lever - Spotify", "type" to "lever", "companyName" to "spotify", "enabled" to true),
            mapOf("name" to "Lever - Netflix", "type" to "lever", "companyName" to "netflix", "enabled" to true),
            mapOf("name" to "Ashby - Linear", "type" to "ashby", "organization" to "linear", "enabled" to true),
            mapOf("name" to "SmartRecruiters - Visa", "type" to "smartrecruiters", "company" to "Visa", "enabled" to true),

            // India sources & Top Indian Tech / Unicorns / Startups
            mapOf("name" to "Greenhouse - Razorpay (India)", "type" to "greenhouse", "boardName" to "razorpay", "enabled" to true),
            mapOf("name" to "Greenhouse - Swiggy (India)", "type" to "greenhouse", "boardName" to "swiggy", "enabled" to true),
            mapOf("name" to "Greenhouse - PhonePe (India)", "type" to "greenhouse", "boardName" to "phonepe", "enabled" to true),
            mapOf("name" to "Greenhouse - Meesho (India)", "type" to "greenhouse", "boardName" to "meesho", "enabled" to true),
            mapOf("name" to "Greenhouse - CRED (India)", "type" to "greenhouse", "boardName" to "cred", "enabled" to true),
            mapOf("name" to "Greenhouse - Postman (India)", "type" to "greenhouse", "boardName" to "postman", "enabled" to true),
            mapOf("name" to "Greenhouse - Groww (India)", "type" to "greenhouse", "boardName" to "groww", "enabled" to true),
            mapOf("name" to "Greenhouse - InMobi (India)", "type" to "greenhouse", "boardName" to "inmobi", "enabled" to true),
            mapOf("name" to "Greenhouse - Urban Company (India)", "type" to "greenhouse", "boardName" to "urbancompany", "enabled" to true),
            mapOf("name" to "Greenhouse - Zepto (India)", "type" to "greenhouse", "boardName" to "zepto", "enabled" to true),
            mapOf("name" to "Greenhouse - Blinkit (India)", "type" to "greenhouse", "boardName" to "blinkit", "enabled" to true),
            mapOf("name" to "Greenhouse - Zomato (India)", "type" to "greenhouse", "boardName" to "zomato", "enabled" to true),
            mapOf("name" to "Ashby - Zerodha (India)", "type" to "ashby", "organization" to "zerodha", "enabled" to true),
            mapOf("name" to "Greenhouse - Ola (India)", "type" to "greenhouse", "boardName" to "ola", "enabled" to true),
            mapOf("name" to "Greenhouse - Ola Electric (India)", "type" to "greenhouse", "boardName" to "olaelectric", "enabled" to true),
            mapOf("name" to "Greenhouse - Ather Energy (India)", "type" to "greenhouse", "boardName" to "atherenergy", "enabled" to true),
            mapOf("name" to "Greenhouse - Nykaa (India)", "type" to "greenhouse", "boardName" to "nykaa", "enabled" to true),
            mapOf("name" to "Greenhouse - Dream11 (India)", "type" to "greenhouse", "boardName" to "dream11", "enabled" to true),
            mapOf("name" to "Greenhouse - Pine Labs (India)", "type" to "greenhouse", "boardName" to "pinelabs", "enabled" to true),
            mapOf("name" to "Greenhouse - Cashfree (India)", "type" to "greenhouse", "boardName" to "cashfree", "enabled" to true),
            mapOf("name" to "Greenhouse - Freshworks (India)", "type" to "greenhouse", "boardName" to "freshworks", "enabled" to true),
            mapOf("name" to "Greenhouse - Zoho (India)", "type" to "greenhouse", "boardName" to "zoho", "enabled" to true),
            mapOf("name" to "Greenhouse - BrowserStack (India)", "type" to "greenhouse", "boardName" to "browserstack", "enabled" to true),
            mapOf("name" to "Greenhouse - ClearTax (India)", "type" to "greenhouse", "boardName" to "cleartax", "enabled" to true),
            mapOf("name" to "Greenhouse - Upstox (India)", "type" to "greenhouse", "boardName" to "upstox", "enabled" to true),
            mapOf("name" to "Greenhouse - CoinDCX (India)", "type" to "greenhouse", "boardName" to "coindcx", "enabled" to true),
            mapOf("name" to "Greenhouse - Licious (India)", "type" to "greenhouse", "boardName" to "licious", "enabled" to true),
            mapOf("name" to "Greenhouse - Delhivery (India)", "type" to "greenhouse", "boardName" to "delhivery", "enabled" to true),
            mapOf("name" to "Greenhouse - Porter (India)", "type" to "greenhouse", "boardName" to "porter", "enabled" to true),

            // Remote-First Global Sources
            mapOf("name" to "Ashby - Vercel (Remote)", "type" to "ashby", "organization" to "vercel", "enabled" to true),
            mapOf("name" to "Ashby - Supabase (Remote)", "type" to "ashby", "organization" to "supabase", "enabled" to true),
            mapOf("name" to "Ashby - Notion (Remote)", "type" to "ashby", "organization" to "notion", "enabled" to true),
            mapOf("name" to "Greenhouse - Figma (Remote)", "type" to "greenhouse", "boardName" to "figma", "enabled" to true),
            mapOf("name" to "Greenhouse - Canva (Remote)", "type" to "greenhouse", "boardName" to "canva", "enabled" to true),
            mapOf("name" to "Greenhouse - GitLab (Remote)", "type" to "greenhouse", "boardName" to "gitlab", "enabled" to true),
            mapOf("name" to "Greenhouse - GitHub (Remote)", "type" to "greenhouse", "boardName" to "github", "enabled" to true),
            mapOf("name" to "Greenhouse - Coinbase (Remote)", "type" to "greenhouse", "boardName" to "coinbase", "enabled" to true),
            mapOf("name" to "Lever - Shopify (Remote)", "type" to "lever", "companyName" to "shopify", "enabled" to true),
            mapOf("name" to "Greenhouse - Deel (Remote)", "type" to "greenhouse", "boardName" to "deel", "enabled" to true),
            mapOf("name" to "Greenhouse - Remote.com (Remote)", "type" to "greenhouse", "boardName" to "remotecom", "enabled" to true),
            mapOf("name" to "Greenhouse - Datadog (Remote)", "type" to "greenhouse", "boardName" to "datadog", "enabled" to true),
            mapOf("name" to "Greenhouse - Cloudflare (Remote)", "type" to "greenhouse", "boardName" to "cloudflare", "enabled" to true)
        )

        for (src in defaults) {
            val id = src["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_")
            try {
                firestore.collection("job_sources").document(id).set(src, SetOptions.merge()).await()
            } catch (_: Exception) {}
        }

        var sourcesSnapshot = try {
            firestore.collection("job_sources").get().await()
        } catch (_: Exception) {
            null
        }

        var sources = sourcesSnapshot?.documents?.map { JobSource.fromDocument(it) } ?: emptyList()

        val activeSources = sources.filter { it.enabled }
        val existingKeys = fetchExistingKeys(firestore)

        for (source in activeSources) {
            val urlStr = buildUrlForSource(source)
            val stats = processSingleSourceInternal(firestore, source.name, source.type, urlStr, existingKeys)
            totalDiscovered += stats["discovered"] as Int
            totalAdded += stats["added"] as Int
            totalDuplicates += stats["duplicates"] as Int

            val sourceId = source.id.ifBlank { source.name.lowercase().replace(Regex("[^a-z0-9]"), "_") }
            try {
                firestore.collection("job_sources").document(sourceId).set(
                    mapOf(
                        "lastSuccessfulFetch" to FieldValue.serverTimestamp(),
                        "lastFetchCount" to stats["added"],
                        "lastFetchStatus" to "SUCCESS"
                    ),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {}
        }

        try {
            firestore.collection("job_fetch_config").document("status").set(
                mapOf(
                    "lastSyncTimestamp" to FieldValue.serverTimestamp(),
                    "lastSyncStatus" to "SUCCESS",
                    "lastDiscovered" to totalDiscovered,
                    "lastAdded" to totalAdded
                ),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) {}

        mapOf("discovered" to totalDiscovered, "added" to totalAdded, "duplicates" to totalDuplicates)
    }

    suspend fun fetchSingleSource(source: JobSource): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val urlStr = buildUrlForSource(source)
        val existingKeys = fetchExistingKeys(firestore)

        val stats = processSingleSourceInternal(firestore, source.name, source.type, urlStr, existingKeys)

        val sourceId = source.id.ifBlank { source.name.lowercase().replace(Regex("[^a-z0-9]"), "_") }
        try {
            firestore.collection("job_sources").document(sourceId).set(
                mapOf(
                    "lastSuccessfulFetch" to FieldValue.serverTimestamp(),
                    "lastFetchCount" to stats["added"],
                    "lastFetchStatus" to "SUCCESS"
                ),
                SetOptions.merge()
            ).await()
        } catch (_: Exception) {}

        stats
    }

    private fun buildUrlForSource(source: JobSource): String {
        val companyClean = source.name
            .replace(Regex("^(Greenhouse|Lever|Ashby|SmartRecruiters)\\s*-\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(India\\)$", RegexOption.IGNORE_CASE), "")
            .trim()
        val identifier = companyClean.lowercase().replace(Regex("[^a-z0-9]"), "")

        return when (source.type) {
            "greenhouse" -> {
                val board = source.boardName.ifBlank { identifier.ifBlank { "stripe" } }
                "https://boards-api.greenhouse.io/v1/boards/$board/jobs?content=true"
            }
            "lever" -> {
                val company = source.companyName.ifBlank { identifier.ifBlank { "spotify" } }
                "https://api.lever.co/v0/postings/$company?mode=json"
            }
            "ashby" -> {
                val org = source.organization.ifBlank { identifier.ifBlank { "linear" } }
                "https://jobs.ashbyhq.com/api/non-public/job-board/$org"
            }
            else -> {
                val comp = source.company.ifBlank { companyClean.ifBlank { "Visa" } }
                "https://api.smartrecruiters.com/v1/companies/$comp/postings"
            }
        }
    }

    private fun normalizeKey(str: String): String {
        if (str.isBlank()) return ""
        return str.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun normalizeUrl(url: String): String {
        if (url.isBlank()) return ""
        return try {
            var clean = url.trim().lowercase()
            clean = clean.split("?")[0].split("#")[0].replace(Regex("/+$"), "")
            clean = clean.replace(Regex("^https?://(www\\.)?"), "")
            clean
        } catch (_: Exception) {
            url.trim().lowercase()
        }
    }

    private suspend fun fetchExistingKeys(firestore: FirebaseFirestore): MutableSet<String> {
        val existingKeys = mutableSetOf<String>()

        fun register(doc: com.google.firebase.firestore.DocumentSnapshot) {
            val title = doc.getString("title") ?: doc.getString("jobTitle") ?: doc.getString("name") ?: ""
            val comp = doc.getString("company") ?: doc.getString("companyName") ?: doc.getString("employer") ?: ""
            if (title.isNotBlank() && comp.isNotBlank()) {
                existingKeys.add(normalizeKey("${comp}_${title}"))
            }
            listOf("applicationUrl", "url", "link", "applyUrl").forEach { field ->
                doc.getString(field)?.let { if (it.isNotBlank()) existingKeys.add(normalizeUrl(it)) }
            }
            listOf("sourceJobId", "source_job_id").forEach { field ->
                doc.getString(field)?.let { if (it.isNotBlank()) existingKeys.add(it.trim()) }
            }
        }

        try {
            val publishedSnap = firestore.collection("jobs").get().await()
            publishedSnap.documents.forEach { register(it) }
        } catch (_: Exception) {}

        try {
            val pendingSnap = firestore.collection("pending_jobs").get().await()
            pendingSnap.documents.forEach { register(it) }
        } catch (_: Exception) {}

        return existingKeys
    }

    private suspend fun processSingleSourceInternal(
        firestore: FirebaseFirestore,
        sourceName: String,
        type: String,
        urlStr: String,
        existingKeys: MutableSet<String>
    ): Map<String, Any> {
        var discovered = 0
        var added = 0
        var duplicates = 0

        try {
            val jsonStr = fetchUrl(urlStr) ?: return mapOf("discovered" to 0, "added" to 0, "duplicates" to 0)
            val jobsList = parseJobs(type, sourceName, jsonStr)

            discovered = jobsList.size

            for (job in jobsList) {
                val key1 = normalizeKey("${job.company}_${job.title}")
                val key2 = normalizeUrl(job.applicationUrl)
                val key3 = job.sourceJobId.trim()

                if (existingKeys.contains(key1) || (key2.isNotBlank() && existingKeys.contains(key2)) || (key3.isNotBlank() && existingKeys.contains(key3))) {
                    duplicates++
                    continue
                }

                existingKeys.add(key1)
                if (key2.isNotBlank()) existingKeys.add(key2)
                if (key3.isNotBlank()) existingKeys.add(key3)

                val map = mapOf(
                    "title" to job.title,
                    "company" to job.company.trim(),
                    "location" to job.location,
                    "jobType" to job.jobType,
                    "experience" to job.experience,
                    "salary" to job.salary,
                    "jd" to job.description,
                    "description" to job.description,
                    "skills" to job.requiredSkills,
                    "requiredSkills" to job.requiredSkills,
                    "preferredSkills" to job.preferredSkills,
                    "url" to job.applicationUrl,
                    "applicationUrl" to job.applicationUrl,
                    "applyUrl" to job.applicationUrl,
                    "link" to job.applicationUrl,
                    "sourceJobId" to job.sourceJobId,
                    "isRemote" to job.location.contains("Remote", true),
                    "remote" to job.location.contains("Remote", true),
                    "source" to job.source,
                    "status" to "PENDING_APPROVAL",
                    "postedDate" to job.postedDate,
                    "date" to LocalDate.now().toString(),
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                try {
                    firestore.collection("pending_jobs").add(map).await()
                    added++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write job ${job.title}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from source $sourceName", e)
        }

        return mapOf("discovered" to discovered, "added" to added, "duplicates" to duplicates)
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP fetch failed for $urlString", e)
            null
        }
    }

    private fun cleanHtmlContent(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""
        val unescaped = rawHtml
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

        return try {
            val decoded = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(unescaped, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(unescaped).toString()
            }
            decoded.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
        } catch (_: Exception) {
            unescaped.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ").trim()
        }
    }

    private fun extractExperience(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("0-1 year") || lower.contains("entry level") || lower.contains("junior") || lower.contains("0 to 1") -> "0 - 1 Years"
            lower.contains("1-3 year") || lower.contains("1+ year") || lower.contains("1 to 3") -> "1 - 3 Years"
            lower.contains("2-5 year") || lower.contains("2+ year") || lower.contains("2 to 5") -> "More than 2 years"
            lower.contains("3-5 year") || lower.contains("3+ year") || lower.contains("3 to 5") -> "More than 3 years"
            lower.contains("5+ year") || lower.contains("5+ years") || lower.contains("5-8 year") || lower.contains("senior") -> "More than 4 years"
            else -> "More than 2 years"
        }
    }

    private fun extractSkills(text: String): List<String> {
        val skillKeywords = listOf(
            "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "React", "Node.js",
            "Flutter", "Android", "iOS", "Swift", "SQL", "NoSQL", "Firebase",
            "AWS", "GCP", "Docker", "Kubernetes", "Git", "GraphQL", "REST API",
            "Machine Learning", "AI", "Agile", "Scrum", "CI/CD", "Spring Boot",
            "Sales", "Account Management", "CRM", "Negotiation", "Analytics",
            "Customer Success", "Go-To-Market", "GTM", "Business Development",
            "Strategy", "Operations", "Finance", "Crypto", "Fintech", "Excel"
        )
        val found = mutableListOf<String>()
        val lower = text.lowercase()
        for (skill in skillKeywords) {
            if (lower.contains(skill.lowercase())) {
                found.add(skill)
            }
        }
        return if (found.isNotEmpty()) found.take(6) else listOf("Problem Solving", "Communication", "Teamwork")
    }

    private class ParsedJob(
        val title: String,
        val company: String,
        val location: String,
        val jobType: String,
        val experience: String,
        val salary: String,
        val description: String,
        val requiredSkills: List<String>,
        val preferredSkills: List<String>,
        val applicationUrl: String,
        val source: String,
        val postedDate: String,
        val sourceJobId: String = ""
    )

    private fun parseJobs(type: String, sourceName: String, jsonStr: String): List<ParsedJob> {
        val result = mutableListOf<ParsedJob>()
        try {
            if (type == "greenhouse") {
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("jobs") ?: return emptyList()
                val companyName = sourceName.replace("Greenhouse - ", "").replace(" (India)", "")
                val maxLimit = minOf(arr.length(), 50)
                for (i in 0 until maxLimit) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", "")
                    val location = obj.optJSONObject("location")?.optString("name", "Remote") ?: "Remote"
                    val rawDesc = obj.optString("content", title)
                    val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now." }
                    val url = obj.optString("absolute_url", "").ifBlank {
                        obj.optString("url", "").ifBlank {
                            obj.optString("apply_url", "")
                        }
                    }
                    val experience = extractExperience(desc + " " + title)
                    val skills = extractSkills(desc + " " + title)

                    if (title.isNotBlank() && url.isNotBlank()) {
                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = experience,
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = skills,
                            preferredSkills = listOf("Git", "Agile", "Problem Solving"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = "Recently posted",
                            sourceJobId = id
                        ))
                    }
                }
            } else if (type == "lever") {
                val arr = JSONArray(jsonStr)
                val companyName = sourceName.replace("Lever - ", "")
                val maxLimit = minOf(arr.length(), 50)
                for (i in 0 until maxLimit) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val title = obj.optString("text", "")
                    val location = obj.optJSONObject("categories")?.optString("location", "Remote") ?: "Remote"
                    val rawDesc = obj.optString("descriptionPlain", title)
                    val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now." }
                    val url = obj.optString("hostedUrl", "").ifBlank {
                        obj.optString("applyUrl", "").ifBlank {
                            obj.optString("url", "")
                        }
                    }
                    val experience = extractExperience(desc + " " + title)
                    val skills = extractSkills(desc + " " + title)

                    if (title.isNotBlank() && url.isNotBlank()) {
                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = experience,
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = skills,
                            preferredSkills = listOf("AWS", "CI/CD", "Teamwork"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = "Recently posted",
                            sourceJobId = id
                        ))
                    }
                }
            } else if (type == "ashby") {
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("jobs") ?: return emptyList()
                val companyName = sourceName.replace("Ashby - ", "")
                val maxLimit = minOf(arr.length(), 50)
                for (i in 0 until maxLimit) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", "")
                    val location = obj.optJSONObject("location")?.optString("name", "Remote") ?: "Remote"
                    val rawDesc = obj.optString("descriptionHtml", title)
                    val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now." }
                    val url = obj.optString("applyUrl", "").ifBlank {
                        obj.optString("jobUrl", "").ifBlank {
                            obj.optString("url", "")
                        }
                    }
                    val experience = extractExperience(desc + " " + title)
                    val skills = extractSkills(desc + " " + title)

                    if (title.isNotBlank() && url.isNotBlank()) {
                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = experience,
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = skills,
                            preferredSkills = listOf("GraphQL", "Docker", "Agile"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = "Recently posted",
                            sourceJobId = id
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON for $type", e)
        }
        return result
    }
}
