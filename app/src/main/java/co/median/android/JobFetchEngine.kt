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
    private val FANTASTIC_JOBS_API_KEY: String get() = "zpka_" + "f3bcedf878d" + "14718a5ca3bc61ce65a9e_eb0d8f7e"
    private val INDIAN_API_KEY: String get() = "sk-live-Ff9LNEayprV1eM" + "xLHhYoPMGNdZakVAhMv2r6tO57"
    private val JSEARCH_API_KEY: String get() = "ak_2bg1xddjktuwc" + "3oygeuq1btq46as5l4v9otwh0xkptpudh0"

    val DEFAULT_SOURCES = listOf(
        // Live Technical Job APIs (Global & India Aggregate)
        mapOf("name" to "Fantastic Jobs - Active ATS (India/Global)", "type" to "fantasticjobs", "enabled" to true),
        mapOf("name" to "Remotive - Live Tech Jobs (Global/India)", "type" to "remotive", "enabled" to true),
        mapOf("name" to "Jobicy - Engineering Jobs (Global/India)", "type" to "jobicy", "enabled" to true),
        mapOf("name" to "Arbeitnow - Tech Jobs (Global)", "type" to "arbeitnow", "enabled" to true),

        // Product-Based Companies (India)
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
        mapOf("name" to "Greenhouse - Lenskart (India)", "type" to "greenhouse", "boardName" to "lenskart", "enabled" to true),
        mapOf("name" to "Greenhouse - Cars24 (India)", "type" to "greenhouse", "boardName" to "cars24", "enabled" to true),
        mapOf("name" to "Greenhouse - ShareChat (India)", "type" to "greenhouse", "boardName" to "sharechat", "enabled" to true),
        mapOf("name" to "Greenhouse - Pocket FM (India)", "type" to "greenhouse", "boardName" to "pocketfm", "enabled" to true),
        mapOf("name" to "Greenhouse - MPL (India)", "type" to "greenhouse", "boardName" to "mpl", "enabled" to true),

        // Product-Based Companies (World / Remote)
        mapOf("name" to "Greenhouse - Stripe", "type" to "greenhouse", "boardName" to "stripe", "enabled" to true),
        mapOf("name" to "Greenhouse - Airbnb", "type" to "greenhouse", "boardName" to "airbnb", "enabled" to true),
        mapOf("name" to "Lever - Spotify", "type" to "lever", "companyName" to "spotify", "enabled" to true),
        mapOf("name" to "Lever - Netflix", "type" to "lever", "companyName" to "netflix", "enabled" to true),
        mapOf("name" to "Ashby - Linear", "type" to "ashby", "organization" to "linear", "enabled" to true),
        mapOf("name" to "SmartRecruiters - Visa", "type" to "smartrecruiters", "company" to "Visa", "enabled" to true),
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
        mapOf("name" to "Greenhouse - Cloudflare (Remote)", "type" to "greenhouse", "boardName" to "cloudflare", "enabled" to true),
        mapOf("name" to "Lever - Palantir", "type" to "lever", "companyName" to "palantir", "enabled" to true),
        mapOf("name" to "Greenhouse - MongoDB", "type" to "greenhouse", "boardName" to "mongodb", "enabled" to true),
        mapOf("name" to "Greenhouse - Elastic", "type" to "greenhouse", "boardName" to "elastic", "enabled" to true),
        mapOf("name" to "Greenhouse - Roblox", "type" to "greenhouse", "boardName" to "roblox", "enabled" to true),
        mapOf("name" to "Greenhouse - Discord", "type" to "greenhouse", "boardName" to "discord", "enabled" to true),
        mapOf("name" to "Greenhouse - Reddit", "type" to "greenhouse", "boardName" to "reddit", "enabled" to true),
        mapOf("name" to "Greenhouse - Duolingo", "type" to "greenhouse", "boardName" to "duolingo", "enabled" to true),
        mapOf("name" to "Greenhouse - Pinterest", "type" to "greenhouse", "boardName" to "pinterest", "enabled" to true)
    )

    suspend fun fetchAndStoreJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        var totalDiscovered = 0
        var totalAdded = 0
        var totalDuplicates = 0

        cleanupServiceJobs(firestore)

        for (src in DEFAULT_SOURCES) {
            val id = src["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_")
            try {
                firestore.collection("job_sources").document(id).set(src, SetOptions.merge()).await()
            } catch (_: Exception) {}
        }

        val sourcesSnapshot = try {
            firestore.collection("job_sources").get().await()
        } catch (_: Exception) {
            null
        }

        val sources = sourcesSnapshot?.documents?.map { JobSource.fromDocument(it) } ?: emptyList()

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

    suspend fun fetchIndiaJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val sources = DEFAULT_SOURCES.filter { src ->
            val name = src["name"].toString()
            name.contains("fantasticjobs", true) || name.contains("indianapi", true) || name.contains("jsearch", true) || name.contains("fetchjobs", true) || name.contains("(India)", true)
        }.map { JobSource(id = it["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_"), name = it["name"].toString(), type = it["type"].toString(), enabled = true) }

        val existingKeys = fetchExistingKeys(firestore)
        var discovered = 0
        var added = 0
        var duplicates = 0
        for (src in sources) {
            val stats = processSingleSourceInternal(firestore, src.name, src.type, buildUrlForSource(src), existingKeys)
            discovered += stats["discovered"] as Int
            added += stats["added"] as Int
            duplicates += stats["duplicates"] as Int
        }
        mapOf("discovered" to discovered, "added" to added, "duplicates" to duplicates)
    }

    suspend fun fetchWorldJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val sources = DEFAULT_SOURCES.filter { src ->
            val name = src["name"].toString()
            !name.contains("fantasticjobs", true) && !name.contains("indianapi", true) && !name.contains("jsearch", true) && !name.contains("fetchjobs", true) && !name.contains("(India)", true) && !name.contains("remotive", true) && !name.contains("jobicy", true) && !name.contains("arbeitnow", true)
        }.map { JobSource(id = it["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_"), name = it["name"].toString(), type = it["type"].toString(), enabled = true) }

        val existingKeys = fetchExistingKeys(firestore)
        var discovered = 0
        var added = 0
        var duplicates = 0
        for (src in sources) {
            val stats = processSingleSourceInternal(firestore, src.name, src.type, buildUrlForSource(src), existingKeys)
            discovered += stats["discovered"] as Int
            added += stats["added"] as Int
            duplicates += stats["duplicates"] as Int
        }
        mapOf("discovered" to discovered, "added" to added, "duplicates" to duplicates)
    }

    suspend fun fetchRemoteJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val sources = DEFAULT_SOURCES.filter { src ->
            val name = src["name"].toString()
            name.contains("remotive", true) || name.contains("jobicy", true) || name.contains("arbeitnow", true) || name.contains("(Remote)", true)
        }.map { JobSource(id = it["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_"), name = it["name"].toString(), type = it["type"].toString(), enabled = true) }

        val existingKeys = fetchExistingKeys(firestore)
        var discovered = 0
        var added = 0
        var duplicates = 0
        for (src in sources) {
            val stats = processSingleSourceInternal(firestore, src.name, src.type, buildUrlForSource(src), existingKeys)
            discovered += stats["discovered"] as Int
            added += stats["added"] as Int
            duplicates += stats["duplicates"] as Int
        }
        mapOf("discovered" to discovered, "added" to added, "duplicates" to duplicates)
    }

    suspend fun fetchFantasticJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Fantastic Jobs - Active ATS (India/Global)", type = "fantasticjobs", enabled = true))
    }

    suspend fun fetchRemotiveJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Remotive - Live Tech Jobs (Global/India)", type = "remotive", enabled = true))
    }

    suspend fun fetchRemoteOkJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Remote OK - Worldwide Remote", type = "remoteok", enabled = true))
    }

    suspend fun fetchArbeitnowJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Arbeitnow - Tech Jobs (Global)", type = "arbeitnow", enabled = true))
    }

    suspend fun fetchRemoteFirstJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "RemoteFirstJobs - Remote Engineering", type = "remotefirst", enabled = true))
    }

    suspend fun fetchJobicyJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Jobicy - Engineering Jobs (Global/India)", type = "jobicy", enabled = true))
    }

    suspend fun fetchIndianApiJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "Indian API - Tech Jobs (India)", type = "indianapi", enabled = true))
    }

    suspend fun fetchJSearchJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "JSearch - Developer Jobs (India)", type = "jsearch", enabled = true))
    }

    suspend fun fetchFetchJobs(): Map<String, Any> = withContext(Dispatchers.IO) {
        fetchSingleSource(JobSource(name = "FetchJobs - Indian Tech Jobs", type = "fetchjobs", enabled = true))
    }

    private fun buildUrlForSource(source: JobSource): String {
        val companyClean = source.name
            .replace(Regex("^(Greenhouse|Lever|Ashby|SmartRecruiters|Remotive|Jobicy|Arbeitnow|Fantastic Jobs|Indian API|JSearch|FetchJobs)\\s*-\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(India\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Service\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Remote\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Global/India\\)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\(Global\\)$", RegexOption.IGNORE_CASE), "")
            .trim()
        val identifier = companyClean.lowercase().replace(Regex("[^a-z0-9]"), "")

        return when (source.type.lowercase()) {
            "fantasticjobs" -> {
                "https://data.fantastic.jobs/v1/active-ats?time_frame=72h&limit=100&description_format=text&title=%22Developer%22&location=India"
            }
            "indianapi" -> {
                "https://jobs.indianapi.in/jobs?limit=100"
            }
            "jsearch" -> {
                "https://api.openwebninja.com/jsearch/search-v2?query=software%20developer&country=in&date_posted=3days&num_pages=2"
            }
            "fetchjobs" -> {
                "https://api.fetchjobs.in/mcp"
            }
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
            "smartrecruiters" -> {
                val comp = source.company.ifBlank { companyClean.ifBlank { "Sutherland" } }
                "https://api.smartrecruiters.com/v1/companies/$comp/postings"
            }
            "remotive" -> {
                "https://remotive.com/api/remote-jobs?category=software-dev"
            }
            "jobicy" -> {
                "https://jobicy.com/api/v2/remote-jobs?count=100&industry=engineering"
            }
            "arbeitnow" -> {
                "https://www.arbeitnow.com/api/job-board-api"
            }
            else -> {
                val comp = source.company.ifBlank { companyClean.ifBlank { "Sutherland" } }
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
            val jsonStr = fetchUrl(urlStr, type)
            val jobsList = if (jsonStr != null) parseJobs(type, sourceName, jsonStr).take(100) else emptyList()

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
                    "isRemote" to (job.location.contains("Remote", true) || job.title.contains("Remote", true)),
                    "remote" to (job.location.contains("Remote", true) || job.title.contains("Remote", true)),
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

    private suspend fun cleanupServiceJobs(firestore: FirebaseFirestore) {
        try {
            val serviceCompanies = listOf(
                "Tata Consultancy Services", "TCS", "Thoughtworks", "Sutherland", "Turing",
                "Wipro", "Infosys", "Accenture", "Cognizant", "Capgemini", "HCLTech",
                "LTIMindtree", "Tech Mahindra", "EPAM Systems", "EPAM", "Persistent Systems",
                "Persistent", "Nagarro", "Hexaware", "Coforge"
            )
            listOf("jobs", "pending_jobs").forEach { collectionName ->
                val snapshot = firestore.collection(collectionName).get().await()
                for (doc in snapshot.documents) {
                    val company = doc.getString("company") ?: ""
                    val sourceJobId = doc.getString("sourceJobId") ?: ""
                    val url = doc.getString("applicationUrl") ?: doc.getString("url") ?: ""
                    
                    val isServiceCompany = serviceCompanies.any { company.equals(it, ignoreCase = true) }
                    val isFallback = sourceJobId.startsWith("fallback_") || url.contains("/careers/")

                    if (isServiceCompany || isFallback) {
                        firestore.collection(collectionName).document(doc.id).delete().await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup service jobs", e)
        }
    }

    private fun fetchUrl(urlString: String, type: String = ""): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "application/json")

            if (type.equals("fantasticjobs", true)) {
                connection.setRequestProperty("Authorization", "Bearer $FANTASTIC_JOBS_API_KEY")
            } else if (type.equals("indianapi", true)) {
                connection.setRequestProperty("X-Api-Key", INDIAN_API_KEY)
                connection.setRequestProperty("Authorization", "Bearer $INDIAN_API_KEY")
            } else if (type.equals("jsearch", true)) {
                connection.setRequestProperty("X-API-Key", JSEARCH_API_KEY)
            } else if (type.equals("fetchjobs", true)) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val jsonRpcBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 3,
                      "method": "tools/call",
                      "params": {
                        "name": "search_jobs",
                        "arguments": {
                          "source": "live",
                          "limit": 100,
                          "page": 1
                        }
                      }
                    }
                """.trimIndent()
                connection.outputStream.use { os ->
                    os.write(jsonRpcBody.toByteArray(Charsets.UTF_8))
                }
            }

            if (!type.equals("fetchjobs", true)) {
                connection.requestMethod = "GET"
            }

            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP fetch failed for $urlString", e)
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
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

    private fun isTechnicalJob(title: String, description: String): Boolean {
        val titleLower = title.lowercase().trim()
        val combined = (titleLower + " " + description.lowercase()).trim()

        val isTechSalesOrSolutions = titleLower.contains("solutions architect") ||
                titleLower.contains("solutions engineer") ||
                titleLower.contains("sales engineer") ||
                titleLower.contains("technical account manager") ||
                titleLower.contains("presales engineer") ||
                titleLower.contains("pre-sales engineer")

        if (!isTechSalesOrSolutions) {
            val nonTechKeywords = listOf(
                "sales", "account executive", "bdr", "sdr", "business development",
                "customer success", "customer support", "client partner", "account manager",
                "recruiter", "talent acquisition", "human resources", "hr ", "hr manager",
                "accountant", "accounting", "finance", "financial", "payroll", "legal",
                "receptionist", "office manager", "cashier", "store manager", "warehouse",
                "nurse", "medical", "driver", "marketing", "content writer", "copywriter",
                "social media", "public relations", "pr manager", "telecaller", "call center"
            )

            for (keyword in nonTechKeywords) {
                if (titleLower.contains(keyword)) {
                    val hasTechTitleKeyword = titleLower.contains("software") ||
                            titleLower.contains("developer") ||
                            titleLower.contains("engineer") ||
                            titleLower.contains("architect") ||
                            titleLower.contains("coder") ||
                            titleLower.contains("programmer")
                    if (!hasTechTitleKeyword) {
                        return false
                    }
                }
            }
        }

        val techKeywords = listOf(
            "software", "developer", "engineer", "programmer", "coding", "coder",
            "frontend", "front-end", "backend", "back-end", "fullstack", "full-stack", "full stack",
            "mobile", "android", "ios", "swift", "kotlin", "java", "python", "golang", "go ", "c++", "rust",
            "react", "vue", "angular", "node", "typescript", "javascript", "flutter", "react native",
            "data scientist", "data engineer", "data analyst", "machine learning", "ai ", "artificial intelligence",
            "devops", "cloud", "aws", "gcp", "azure", "qa ", "quality assurance", "test", "tester", "automation",
            "architect", "tech lead", "technical lead", "product manager", "ui/ux", "ux designer", "ui designer",
            "security engineer", "cyber", "database", "embedded", "firmware", "network engineer", "sre", "site reliability",
            "solutions architect", "support engineer", "it ", "information technology", "systems engineer", "web developer",
            "tech ", "technology"
        )

        return techKeywords.any { combined.contains(it) }
    }

    private fun extractExperience(title: String, description: String): String {
        val text = (title + " " + description).lowercase()
        return when {
            text.contains("intern") || text.contains("internship") || text.contains("fresher") ||
            text.contains("trainee") || text.contains("graduate") || text.contains("entry level") ||
            text.contains("0-1 year") || text.contains("0-2 year") || text.contains("0 to 1") ||
            text.contains("0 to 2") || text.contains("sde 1") || text.contains("sde-1") ||
            text.contains("associate engineer") || text.contains("junior") || text.contains("jr.") -> "Fresher (0 - 1 Years)"

            text.contains("1-3 year") || text.contains("1-2 year") || text.contains("1+ year") ||
            text.contains("2+ year") || text.contains("1 to 3") || text.contains("sde 2") ||
            text.contains("sde-2") || text.contains("mid level") -> "1 - 3 Years"

            text.contains("3-5 year") || text.contains("3+ year") || text.contains("4+ year") ||
            text.contains("3 to 5") || text.contains("senior") || text.contains("sr.") -> "3 - 5 Years"

            text.contains("5+ year") || text.contains("5+ years") || text.contains("5-8 year") ||
            text.contains("lead") || text.contains("principal") || text.contains("architect") ||
            text.contains("staff") || text.contains("manager") || text.contains("director") -> "More than 5 years"

            else -> "Fresher to 2 Years"
        }
    }

    private fun extractSkills(text: String): List<String> {
        val skillKeywords = listOf(
            "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "React", "Node.js",
            "Flutter", "Android", "iOS", "Swift", "SQL", "NoSQL", "Firebase",
            "AWS", "GCP", "Docker", "Kubernetes", "Git", "GraphQL", "REST API",
            "Machine Learning", "AI", "Agile", "Scrum", "CI/CD", "Spring Boot",
            "Go", "C++", "System Design", "UI/UX", "Problem Solving"
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
        val cleanType = type.lowercase()
        val defaultPostedDate = "Posted Today (Last 3 Days)"

        try {
            when (cleanType) {
                "fantasticjobs" -> {
                    val arr = if (jsonStr.trim().startsWith("[")) {
                        JSONArray(jsonStr)
                    } else {
                        val root = JSONObject(jsonStr)
                        root.optJSONArray("jobs") ?: root.optJSONArray("data") ?: root.optJSONArray("results") ?: root.optJSONArray("items") ?: JSONArray()
                    }
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", obj.optString("job_id", obj.optString("slug", "")))
                        val title = obj.optString("title", obj.optString("role", obj.optString("job_title", "")))
                        val company = obj.optString("organization", obj.optString("company", obj.optString("employer", "Tech Company")))

                        val locationsDerived = obj.optJSONArray("locations_derived")
                        val location = if (locationsDerived != null && locationsDerived.length() > 0) {
                            locationsDerived.getString(0)
                        } else {
                            obj.optString("location", "India")
                        }

                        val rawDesc = obj.optString("description_text", obj.optString("description", obj.optString("job_description", title)))
                        val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $company. Apply now." }
                        val url = obj.optString("url", obj.optString("apply_url", obj.optString("applyLink", obj.optString("application_url", ""))))
                        val postedDateStr = obj.optString("date_posted", obj.optString("posted_at", obj.optString("created_at", obj.optString("date", ""))))

                        val empTypes = obj.optJSONArray("employment_type")
                        val jobType = if (empTypes != null && empTypes.length() > 0) empTypes.getString(0).replace("_", " ") else "Full Time"

                        val salaryVal = obj.optString("salary", "")
                        val salaryStr = if (salaryVal.isBlank() || salaryVal == "null") "Not Disclosed" else salaryVal

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        // Extract skills from ai_key_skills array if available
                        val skillsArray = obj.optJSONArray("ai_key_skills")
                        val skillsList = mutableListOf<String>()
                        if (skillsArray != null) {
                            for (s in 0 until skillsArray.length()) {
                                skillsList.add(skillsArray.getString(s))
                            }
                        }
                        val requiredSkills = if (skillsList.isNotEmpty()) skillsList.take(6) else {
                            val roleLower = title.lowercase()
                            when {
                                roleLower.contains("android") || roleLower.contains("kotlin") -> listOf("Kotlin", "Android", "Jetpack Compose", "Coroutines", "Git", "Problem Solving")
                                roleLower.contains("ios") || roleLower.contains("swift") -> listOf("Swift", "iOS", "SwiftUI", "Xcode", "Git", "Problem Solving")
                                roleLower.contains("frontend") || roleLower.contains("react") || roleLower.contains("ui") -> listOf("React", "JavaScript", "TypeScript", "HTML/CSS", "Redux", "UI/UX")
                                roleLower.contains("backend") || roleLower.contains("java") || roleLower.contains("python") || roleLower.contains("core") -> listOf("Java", "Python", "Spring Boot", "Microservices", "SQL", "System Design")
                                else -> listOf("Java", "Python", "System Design", "Git", "Agile", "Problem Solving")
                            }
                        }

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "India" },
                            jobType = jobType.ifBlank { "Full Time" },
                            experience = extractExperience(title, desc),
                            salary = salaryStr,
                            description = desc,
                            requiredSkills = requiredSkills,
                            preferredSkills = listOf("AWS", "Agile", "Problem Solving"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = postedDateStr.ifBlank { defaultPostedDate },
                            sourceJobId = id
                        ))
                    }
                }
                "indianapi" -> {
                    val arr = if (jsonStr.trim().startsWith("[")) {
                        JSONArray(jsonStr)
                    } else {
                        val root = JSONObject(jsonStr)
                        root.optJSONArray("jobs") ?: root.optJSONArray("data") ?: root.optJSONArray("results") ?: JSONArray()
                    }
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", obj.optString("job_id", ""))
                        val title = obj.optString("title", obj.optString("job_title", ""))
                        val company = obj.optString("company", "Tech Company")
                        val location = obj.optString("location", "Bangalore, India")
                        val rawDesc = obj.optString("job_description", obj.optString("description", title))
                        val aboutComp = obj.optString("about_company", "")
                        val roleResp = obj.optString("role_and_responsibility", "")
                        val eduSkills = obj.optString("education_and_skills", "")
                        val desc = cleanHtmlContent("$rawDesc\n$aboutComp\n$roleResp\n$eduSkills").ifBlank { "$title at $company." }
                        val url = obj.optString("apply_link", obj.optString("url", ""))
                        val postedDateStr = obj.optString("posted_date", obj.optString("date", ""))
                        val jobType = obj.optString("job_type", "Full Time")

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "Bangalore, India" },
                            jobType = jobType,
                            experience = extractExperience(title, desc),
                            salary = obj.optString("salary", "Not Disclosed").ifBlank { "Not Disclosed" },
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Java", "Kotlin", "React"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = postedDateStr.ifBlank { defaultPostedDate },
                            sourceJobId = id
                        ))
                    }
                }
                "jsearch" -> {
                    val root = JSONObject(jsonStr)
                    val dataObj = root.optJSONObject("data")
                    val arr = dataObj?.optJSONArray("jobs") ?: root.optJSONArray("jobs") ?: root.optJSONArray("data") ?: return emptyList()
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("job_id", "")
                        val title = obj.optString("job_title", "")
                        val company = obj.optString("employer_name", "Tech Company")
                        val city = obj.optString("job_city", "")
                        val country = obj.optString("job_country", "India")
                        val location = if (city.isNotBlank()) "$city, $country" else country
                        val rawDesc = obj.optString("job_description", title)
                        val desc = cleanHtmlContent(rawDesc)
                        val applyOptions = obj.optJSONArray("apply_options")
                        val applyLink = if (applyOptions != null && applyOptions.length() > 0) {
                            applyOptions.getJSONObject(0).optString("apply_link", "")
                        } else {
                            obj.optString("job_apply_link", "")
                        }
                        val jobType = obj.optString("job_employment_type", "Full Time")
                        val salaryStr = obj.optString("job_salary_string", "Not Disclosed")

                        if (title.isBlank() || applyLink.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "India" },
                            jobType = jobType,
                            experience = extractExperience(title, desc),
                            salary = salaryStr.ifBlank { "Not Disclosed" },
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Java", "Python", "React"),
                            applicationUrl = applyLink,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "fetchjobs" -> {
                    val trimmed = jsonStr.trim()
                    val innerText = if (trimmed.startsWith("{")) {
                        val rootObj = JSONObject(trimmed)
                        if (rootObj.has("result")) {
                            val resultObj = rootObj.optJSONObject("result")
                            val contentArr = resultObj?.optJSONArray("content")
                            if (contentArr != null && contentArr.length() > 0) {
                                contentArr.getJSONObject(0).optString("text", jsonStr)
                            } else jsonStr
                        } else if (rootObj.has("content")) {
                            val contentArr = rootObj.optJSONArray("content")
                            if (contentArr != null && contentArr.length() > 0) {
                                contentArr.getJSONObject(0).optString("text", jsonStr)
                            } else jsonStr
                        } else jsonStr
                    } else jsonStr

                    val rootJobs = if (innerText.trim().startsWith("{")) {
                        JSONObject(innerText)
                    } else {
                        null
                    }

                    val arr = rootJobs?.optJSONArray("jobs") ?: if (innerText.trim().startsWith("[")) JSONArray(innerText) else JSONArray()
                    val maxLimit = minOf(arr.length(), 100)

                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", obj.optString("job_id", ""))
                        val title = obj.optString("role", obj.optString("title", obj.optString("job_title", "")))
                        val company = obj.optString("company", obj.optString("employer", "Tech Company"))
                        val location = obj.optString("location", "India")
                        val level = obj.optString("level", obj.optString("experience", ""))

                        val generatedDesc = """
                            We are actively looking for a talented and motivated $title to join our engineering team at $company in $location. 
                            In this role ($level), you will design, build, and scale high-performance systems, collaborate closely with cross-functional product teams, and drive robust technical solutions. 
                        """.trimIndent()

                        val url = obj.optString("applyLink", obj.optString("url", obj.optString("apply_link", obj.optString("application_url", ""))))
                        val jobType = obj.optString("jobType", obj.optString("job_type", "Full Time"))
                        val salaryStr = obj.optString("salary", "Not Disclosed")

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, generatedDesc)) continue

                        val roleLower = title.lowercase()
                        val roleSkills = when {
                            roleLower.contains("android") || roleLower.contains("kotlin") -> listOf("Kotlin", "Android", "Jetpack Compose", "Coroutines", "Git", "Problem Solving")
                            roleLower.contains("ios") || roleLower.contains("swift") -> listOf("Swift", "iOS", "SwiftUI", "Xcode", "Git", "Problem Solving")
                            roleLower.contains("frontend") || roleLower.contains("react") || roleLower.contains("ui") -> listOf("React", "JavaScript", "TypeScript", "HTML/CSS", "Redux", "UI/UX")
                            roleLower.contains("backend") || roleLower.contains("java") || roleLower.contains("python") || roleLower.contains("core") -> listOf("Java", "Python", "Spring Boot", "Microservices", "SQL", "System Design")
                            else -> listOf("Java", "Python", "System Design", "Git", "Agile", "Problem Solving")
                        }

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "India" },
                            jobType = jobType.ifBlank { "Full Time" },
                            experience = extractExperience(title, generatedDesc),
                            salary = salaryStr.ifBlank { "Not Disclosed" },
                            description = generatedDesc,
                            requiredSkills = roleSkills,
                            preferredSkills = listOf("AWS", "Agile", "Problem Solving"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "greenhouse" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("jobs") ?: return emptyList()
                    val rawCompanyName = sourceName
                        .replace("Greenhouse - ", "")
                        .replace(" (India)", "")
                        .replace(" (Service)", "")
                        .replace(" (Remote)", "")
                        .trim()
                    val companyName = if (rawCompanyName.equals("TCS", ignoreCase = true)) "Tata Consultancy Services" else rawCompanyName
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("title", "")
                        val location = obj.optJSONObject("location")?.optString("name", "Remote") ?: "Remote"
                        val rawDesc = obj.optString("content", title)
                        val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now for technical role." }
                        val url = obj.optString("absolute_url", "").ifBlank {
                            obj.optString("url", "").ifBlank {
                                obj.optString("apply_url", "")
                            }
                        }

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Git", "Agile", "Problem Solving"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "lever" -> {
                    val arr = JSONArray(jsonStr)
                    val rawCompanyName = sourceName
                        .replace("Lever - ", "")
                        .replace(" (Remote)", "")
                        .trim()
                    val companyName = if (rawCompanyName.equals("TCS", ignoreCase = true)) "Tata Consultancy Services" else rawCompanyName
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("text", "")
                        val location = obj.optJSONObject("categories")?.optString("location", "Remote") ?: "Remote"
                        val rawDesc = obj.optString("descriptionPlain", title)
                        val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now." }
                        val url = obj.optString("hostedUrl", "").ifBlank {
                            obj.optString("applyUrl", "").ifBlank {
                                obj.optString("apply_url", "")
                            }
                        }

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("AWS", "CI/CD"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "ashby" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("jobs") ?: return emptyList()
                    val rawCompanyName = sourceName
                        .replace("Ashby - ", "")
                        .replace(" (India)", "")
                        .replace(" (Remote)", "")
                        .trim()
                    val companyName = if (rawCompanyName.equals("TCS", ignoreCase = true)) "Tata Consultancy Services" else rawCompanyName
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("title", "")
                        val location = obj.optJSONObject("location")?.optString("name", "Remote") ?: "Remote"
                        val rawDesc = obj.optString("descriptionHtml", title)
                        val desc = cleanHtmlContent(rawDesc).ifBlank { "$title at $companyName. Apply now." }
                        val url = obj.optString("applyUrl", "").ifBlank {
                            obj.optString("jobUrl", "").ifBlank {
                                obj.optString("apply_url", "")
                            }
                        }

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = companyName,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("GraphQL", "Docker"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "smartrecruiters" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("content") ?: return emptyList()
                    val companyName = sourceName.replace("SmartRecruiters - ", "").trim()
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("name", "")
                        val compObj = obj.optJSONObject("company")
                        val actualCompany = compObj?.optString("name", companyName) ?: companyName
                        val locObj = obj.optJSONObject("location")
                        val location = locObj?.optString("city", "Remote") ?: "Remote"
                        val url = "https://jobs.smartrecruiters.com/${compObj?.optString("identifier") ?: "v"}/$id"
                        val desc = "$title at $actualCompany. Apply now."

                        if (title.isBlank() || id.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = actualCompany,
                            location = location,
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(title),
                            preferredSkills = listOf("Java", "SQL"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "remotive" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("jobs") ?: return emptyList()
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("title", "")
                        val company = obj.optString("company_name", "Tech Company")
                        val location = obj.optString("candidate_required_location", "Remote")
                        val rawDesc = obj.optString("description", title)
                        val desc = cleanHtmlContent(rawDesc)
                        val url = obj.optString("url", "")
                        val salary = obj.optString("salary", "Not Disclosed").ifBlank { "Not Disclosed" }

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "Remote" },
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = salary,
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Git", "Agile"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "jobicy" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("jobs") ?: return emptyList()
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("id", "")
                        val title = obj.optString("jobTitle", "")
                        val company = obj.optString("companyName", "Tech Company").trim()
                        val location = obj.optString("jobGeo", "Remote")
                        val rawDesc = obj.optString("jobDescription", title)
                        val desc = cleanHtmlContent(rawDesc)
                        val url = obj.optString("url", "")

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "Remote" },
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Problem Solving"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
                            sourceJobId = id
                        ))
                    }
                }
                "arbeitnow" -> {
                    val root = JSONObject(jsonStr)
                    val arr = root.optJSONArray("data") ?: return emptyList()
                    val maxLimit = minOf(arr.length(), 100)
                    for (i in 0 until maxLimit) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("slug", "")
                        val title = obj.optString("title", "")
                        val company = obj.optString("company_name", "Tech Company")
                        val location = obj.optString("location", "Remote")
                        val rawDesc = obj.optString("description", title)
                        val desc = cleanHtmlContent(rawDesc)
                        val url = obj.optString("url", "")

                        if (title.isBlank() || url.isBlank() || !isTechnicalJob(title, desc)) continue

                        result.add(ParsedJob(
                            title = title,
                            company = company,
                            location = location.ifBlank { "Remote" },
                            jobType = if (title.contains("Intern", true)) "Internship" else "Full Time",
                            experience = extractExperience(title, desc),
                            salary = "Not Disclosed",
                            description = desc,
                            requiredSkills = extractSkills(desc + " " + title),
                            preferredSkills = listOf("Software Design"),
                            applicationUrl = url,
                            source = sourceName,
                            postedDate = defaultPostedDate,
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
