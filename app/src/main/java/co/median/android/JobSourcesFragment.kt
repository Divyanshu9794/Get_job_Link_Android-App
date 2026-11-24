package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class JobSourcesFragment : Fragment() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var adapter: JobSourcesAdapter

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val sourcesList = mutableListOf<JobSource>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_job_sources, container, false)

        val btnFetchIndia = view.findViewById<MaterialButton>(R.id.btnFetchIndiaJobs)
        val btnFetchRemote = view.findViewById<MaterialButton>(R.id.btnFetchRemoteJobs)
        val btnFetchWorld = view.findViewById<MaterialButton>(R.id.btnFetchWorldJobs)
        swipeRefreshLayout = view.findViewById(R.id.sourcesSwipeRefresh)
        recyclerView = view.findViewById(R.id.sourcesRecyclerView)
        progressBar = view.findViewById(R.id.sourcesProgressBar)
        emptyView = view.findViewById(R.id.sourcesEmptyView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = JobSourcesAdapter(
            sources = sourcesList,
            onToggleEnabled = { source, enabled ->
                updateSourceEnabled(source.id, enabled, source.name)
            },
            onTestFetch = { source ->
                triggerSingleSourceSync(source)
            }
        )
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener { loadSources() }

        btnFetchIndia.setOnClickListener {
            showIndiaApiSelectionDialog()
        }
        btnFetchRemote.setOnClickListener {
            showRemoteApiSelectionDialog()
        }
        btnFetchWorld.setOnClickListener {
            triggerSync("🌍 Syncing World Tech Jobs", "Fetching engineering openings across USA & global tech leaders...") {
                JobFetchEngine.fetchWorldJobs()
            }
        }

        loadSources()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadSources()
    }

    private fun loadSources() {
        if (!isAdded || context == null) return
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "Authentication Error: Please log in to your admin account.", Toast.LENGTH_LONG).show()
            emptyView.text = "User not authenticated. Please log in."
            emptyView.visibility = View.VISIBLE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseProfileRepository.ensureAdminProfileSyncAsync()
                val snapshot = firestore.collection("job_sources").get().await()
                val list = snapshot.documents.map { JobSource.fromDocument(it) }

                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    sourcesList.clear()
                    sourcesList.addAll(list)
                    if (sourcesList.isNotEmpty()) {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "No job sources configured."
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    for (src in JobFetchEngine.DEFAULT_SOURCES) {
                        val id = src["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_")
                        try {
                            firestore.collection("job_sources").document(id).set(src, com.google.firebase.firestore.SetOptions.merge()).await()
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                val sampleSources = JobFetchEngine.DEFAULT_SOURCES.map { src ->
                    JobSource(
                        id = src["name"].toString().lowercase().replace(Regex("[^a-z0-9]"), "_"),
                        name = src["name"].toString(),
                        type = src["type"]?.toString() ?: "greenhouse",
                        boardName = src["boardName"]?.toString() ?: "",
                        companyName = src["companyName"]?.toString() ?: "",
                        organization = src["organization"]?.toString() ?: "",
                        company = src["company"]?.toString() ?: "",
                        enabled = true,
                        lastFetchCount = 5,
                        lastFetchStatus = "SUCCESS"
                    )
                }
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    sourcesList.clear()
                    sourcesList.addAll(sampleSources)
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Loaded default job sources (Offline/Fallback mode)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseFirestoreError(e: Exception): String {
        val msg = e.localizedMessage ?: ""
        return when {
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission Denied: You do not have administrator privileges to manage job sources."
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication Error: Please log in again."
            msg.contains("PERMISSION_DENIED", true) ->
                "Permission Denied: Admin privileges required."
            msg.contains("unavailable", true) || msg.contains("network", true) ->
                "Network Unavailable: Please check your connection."
            else -> "Failed to load job sources: $msg"
        }
    }

    private fun updateSourceEnabled(sourceId: String, enabled: Boolean, sourceName: String) {
        firestore.collection("job_sources").document(sourceId)
            .update("enabled", enabled)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Source status updated", Toast.LENGTH_SHORT).show()
                    val ctx = requireContext()
                    if (sourceName.contains("fetchjobs", true)) {
                        if (enabled) {
                            val constraints = Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                            val workRequest = PeriodicWorkRequestBuilder<FetchJobsSyncWorker>(6, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .build()
                            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                                "fetchjobs_sync",
                                ExistingPeriodicWorkPolicy.UPDATE,
                                workRequest
                            )
                        } else {
                            WorkManager.getInstance(ctx).cancelUniqueWork("fetchjobs_sync")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), parseFirestoreError(e), Toast.LENGTH_LONG).show()
                    loadSources()
                }
            }
    }

    private fun triggerSingleSourceSync(source: JobSource) {
        val ctx = context ?: return
        val companyName = getCleanCompanyName(source)

        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_sync_progress, null)
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtSyncDialogTitle)
        val txtSubtitle = dialogView.findViewById<TextView>(R.id.txtSyncDialogSubtitle)
        val btnCloseX = dialogView.findViewById<ImageButton>(R.id.btnCloseSyncDialogX)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.syncProgressIndicator)
        val cardStats = dialogView.findViewById<MaterialCardView>(R.id.cardSyncStats)
        val txtStatAdded = dialogView.findViewById<TextView>(R.id.txtStatAdded)
        val txtStatDiscovered = dialogView.findViewById<TextView>(R.id.txtStatDiscovered)
        val txtStatDuplicates = dialogView.findViewById<TextView>(R.id.txtStatDuplicates)
        val layoutActions = dialogView.findViewById<LinearLayout>(R.id.layoutSyncDialogActions)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnSyncDialogClose)
        val btnGoToPending = dialogView.findViewById<MaterialButton>(R.id.btnSyncDialogGoToPending)

        txtTitle.text = "⚡ Fetching $companyName Jobs"
        txtSubtitle.text = "Connecting to source API and filtering latest technical openings..."

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCloseX.setOnClickListener { dialog.dismiss() }
        btnClose.setOnClickListener { dialog.dismiss() }
        btnGoToPending.setOnClickListener {
            dialog.dismiss()
            (parentFragment as? AdminDashboardFragment)?.selectPendingTab()
        }

        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = JobFetchEngine.fetchSingleSource(source)
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) {
                        dialog.dismiss()
                        return@withContext
                    }
                    progressIndicator.visibility = View.GONE
                    txtTitle.text = "Sync Complete for $companyName!"
                    txtSubtitle.text = "Technical jobs processed into Pending Approval:"

                    val added = stats["added"] as? Int ?: 0
                    val discovered = stats["discovered"] as? Int ?: 0
                    val within24h = stats["within24h"] as? Int ?: 0
                    val duplicates = stats["duplicates"] as? Int ?: 0
                    val rejected = stats["rejected"] as? Int ?: 0

                    txtStatAdded.text = "📥 Added to Pending Approval: +$added new jobs"
                    txtStatDiscovered.text = "🔍 Total Discovered: $discovered jobs (Fresh <24h: $within24h)"
                    txtStatDuplicates.text = "♻️ Duplicates: $duplicates | Rejected (Age/Limit/URL): $rejected"
                    cardStats.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE

                    loadSources()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) {
                        dialog.dismiss()
                        return@withContext
                    }
                    progressIndicator.visibility = View.GONE
                    txtTitle.text = "Sync Failed"
                    txtSubtitle.text = "Error: ${e.localizedMessage}"
                    layoutActions.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun triggerSync(dialogTitle: String, dialogSubtitle: String, fetchAction: suspend () -> Map<String, Any>) {
        val ctx = context ?: return

        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_sync_progress, null)
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtSyncDialogTitle)
        val txtSubtitle = dialogView.findViewById<TextView>(R.id.txtSyncDialogSubtitle)
        val btnCloseX = dialogView.findViewById<ImageButton>(R.id.btnCloseSyncDialogX)
        val progressIndicator = dialogView.findViewById<LinearProgressIndicator>(R.id.syncProgressIndicator)
        val cardStats = dialogView.findViewById<MaterialCardView>(R.id.cardSyncStats)
        val txtStatAdded = dialogView.findViewById<TextView>(R.id.txtStatAdded)
        val txtStatDiscovered = dialogView.findViewById<TextView>(R.id.txtStatDiscovered)
        val txtStatDuplicates = dialogView.findViewById<TextView>(R.id.txtStatDuplicates)
        val layoutActions = dialogView.findViewById<LinearLayout>(R.id.layoutSyncDialogActions)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnSyncDialogClose)
        val btnGoToPending = dialogView.findViewById<MaterialButton>(R.id.btnSyncDialogGoToPending)

        txtTitle.text = dialogTitle
        txtSubtitle.text = dialogSubtitle

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCloseX.setOnClickListener { dialog.dismiss() }
        btnClose.setOnClickListener { dialog.dismiss() }
        btnGoToPending.setOnClickListener {
            dialog.dismiss()
            (parentFragment as? AdminDashboardFragment)?.selectPendingTab()
        }

        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = fetchAction()
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) {
                        dialog.dismiss()
                        return@withContext
                    }
                    progressIndicator.visibility = View.GONE
                    txtTitle.text = "Sync Complete Successfully!"
                    txtSubtitle.text = "Summary of technical jobs added to Pending Approval:"

                    val added = stats["added"] as? Int ?: 0
                    val discovered = stats["discovered"] as? Int ?: 0
                    val within24h = stats["within24h"] as? Int ?: 0
                    val duplicates = stats["duplicates"] as? Int ?: 0
                    val rejected = stats["rejected"] as? Int ?: 0

                    txtStatAdded.text = "📥 Added to Pending Approval: +$added new jobs"
                    txtStatDiscovered.text = "🔍 Total Discovered: $discovered jobs (Fresh <24h: $within24h)"
                    txtStatDuplicates.text = "♻️ Duplicates: $duplicates | Rejected (Age/Limit/URL): $rejected"
                    cardStats.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE

                    loadSources()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) {
                        dialog.dismiss()
                        return@withContext
                    }
                    progressIndicator.visibility = View.GONE
                    txtTitle.text = "Sync Failed"
                    txtSubtitle.text = "Error: ${e.localizedMessage}"
                    layoutActions.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        fun getCleanCompanyName(source: JobSource): String {
            val raw = source.name
                .replace(Regex("^(Greenhouse|Lever|Ashby|SmartRecruiters|Remotive|Jobicy|Arbeitnow|Remote OK|RemoteFirstJobs|FetchJobs)\\s*-\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\(India\\)$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\(Service\\)$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\(Remote\\)$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\(Global/India\\)$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\(Global\\)$", RegexOption.IGNORE_CASE), "")
                .trim()
            return if (raw.isBlank()) source.name else raw
        }
    }

    private class JobSourcesAdapter(
        private val sources: List<JobSource>,
        private val onToggleEnabled: (JobSource, Boolean) -> Unit,
        private val onTestFetch: (JobSource) -> Unit
    ) : RecyclerView.Adapter<JobSourceViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobSourceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job_source, parent, false)
            return JobSourceViewHolder(view)
        }

        override fun onBindViewHolder(holder: JobSourceViewHolder, position: Int) {
            val source = sources[position]
            holder.bind(source, onToggleEnabled, onTestFetch)
        }

        override fun getItemCount(): Int = sources.size
    }

    private class JobSourceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.sourceName)
        private val switchEnabled: SwitchCompat = itemView.findViewById(R.id.switchSourceEnabled)
        private val txtType: TextView = itemView.findViewById(R.id.sourceType)
        private val txtLastFetch: TextView = itemView.findViewById(R.id.sourceLastFetch)
        private val txtStatus: TextView = itemView.findViewById(R.id.sourceStatus)
        private val btnTest: MaterialButton = itemView.findViewById(R.id.btnTestFetch)

        fun bind(
            source: JobSource,
            onToggleEnabled: (JobSource, Boolean) -> Unit,
            onTestFetch: (JobSource) -> Unit
        ) {
            txtName.text = source.name
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = source.enabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked -> onToggleEnabled(source, isChecked) }

            val target = when (source.type) {
                "greenhouse" -> source.boardName
                "lever" -> source.companyName
                "ashby" -> source.organization
                else -> source.company
            }
            txtType.text = "Type: ${source.type} • Target: $target"
            txtLastFetch.text = "Last Fetch: ${source.lastFetchCount} added"
            txtStatus.text = source.lastFetchStatus

            val companyCleanName = getCleanCompanyName(source)
            btnTest.text = "Fetch $companyCleanName Jobs"

            btnTest.setOnClickListener { onTestFetch(source) }
        }
    }

    private fun showIndiaApiSelectionDialog() {
        val ctx = context ?: return
        val options = arrayOf(
            "1. Fantastic Jobs API (data.fantastic.jobs - Limit 150)",
            "2. Indian API (jobs.indianapi.in - Limit 50)",
            "3. JSearch API (openwebninja.com - JSearch)",
            "4. FetchJobs API (api.fetchjobs.in/mcp)",
            "5. All India Sources Combined"
        )

        MaterialAlertDialogBuilder(ctx)
            .setTitle("🇮🇳 Select India Job API Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> triggerSync("⚡ Fetching via Fantastic Jobs", "Fetching up to 150 active ATS developer jobs via Fantastic Jobs API...") {
                        JobFetchEngine.fetchFantasticJobs()
                    }
                    1 -> triggerSync("⚡ Fetching via Indian API", "Fetching up to 50 tech jobs via Indian API...") {
                        JobFetchEngine.fetchIndianApiJobs()
                    }
                    2 -> triggerSync("⚡ Fetching via JSearch API", "Fetching developer jobs in India via JSearch OpenWeb Ninja...") {
                        JobFetchEngine.fetchJSearchJobs()
                    }
                    3 -> triggerSync("⚡ Fetching via FetchJobs API", "Fetching Indian tech jobs via api.fetchjobs.in/mcp...") {
                        JobFetchEngine.fetchFetchJobs()
                    }
                    4 -> triggerSync("🇮🇳 Syncing All India Tech Jobs", "Fetching from Fantastic Jobs, Indian API, JSearch, FetchJobs, and top Indian product companies...") {
                        JobFetchEngine.fetchIndiaJobs()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showRemoteApiSelectionDialog() {
        val ctx = context ?: return
        val options = arrayOf(
            "1. Remotive API",
            "2. Remote OK API",
            "3. Arbeitnow API",
            "4. RemoteFirstJobs API",
            "5. Jobicy API",
            "6. Greenhouse Atlassian & Adobe",
            "7. All Remote Sources Combined"
        )

        MaterialAlertDialogBuilder(ctx)
            .setTitle("🌐 Select Remote Job API Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> triggerSync("⚡ Fetching via Remotive", "Fetching remote tech jobs via Remotive API...") {
                        JobFetchEngine.fetchRemotiveJobs()
                    }
                    1 -> triggerSync("⚡ Fetching via Remote OK", "Fetching remote openings via Remote OK API...") {
                        JobFetchEngine.fetchRemoteOkJobs()
                    }
                    2 -> triggerSync("⚡ Fetching via Arbeitnow", "Fetching tech jobs via Arbeitnow API...") {
                        JobFetchEngine.fetchArbeitnowJobs()
                    }
                    3 -> triggerSync("⚡ Fetching via RemoteFirstJobs", "Fetching remote engineering via RemoteFirstJobs API...") {
                        JobFetchEngine.fetchRemoteFirstJobs()
                    }
                    4 -> triggerSync("⚡ Fetching via Jobicy", "Fetching engineering jobs via Jobicy API...") {
                        JobFetchEngine.fetchJobicyJobs()
                    }
                    5 -> triggerSync("⚡ Fetching via Greenhouse (Atlassian/Adobe)", "Fetching remote technical jobs directly from Greenhouse ATS portals...") {
                        JobFetchEngine.fetchRemoteJobs()
                    }
                    6 -> triggerSync("🌐 Syncing All Remote Sources", "Fetching from Remotive, Remote OK, Arbeitnow, RemoteFirstJobs, Jobicy, and direct company ATS portals...") {
                        JobFetchEngine.fetchRemoteJobs()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
