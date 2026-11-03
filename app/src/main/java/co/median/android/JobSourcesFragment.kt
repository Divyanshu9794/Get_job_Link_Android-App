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

        val btnFetchAll = view.findViewById<MaterialButton>(R.id.btnFetchAllCompanies)
        swipeRefreshLayout = view.findViewById(R.id.sourcesSwipeRefresh)
        recyclerView = view.findViewById(R.id.sourcesRecyclerView)
        progressBar = view.findViewById(R.id.sourcesProgressBar)
        emptyView = view.findViewById(R.id.sourcesEmptyView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = JobSourcesAdapter(
            sources = sourcesList,
            onToggleEnabled = { source, enabled ->
                updateSourceEnabled(source.id, enabled)
            },
            onTestFetch = { source ->
                triggerSingleSourceSync(source)
            }
        )
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener { loadSources() }

        btnFetchAll.setOnClickListener {
            triggerFetchAllCompanies()
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
                val errorMsg = parseFirestoreError(e)
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = errorMsg
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
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

    private fun updateSourceEnabled(sourceId: String, enabled: Boolean) {
        firestore.collection("job_sources").document(sourceId)
            .update("enabled", enabled)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Source status updated", Toast.LENGTH_SHORT).show()
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

                    val added = stats["added"] as Int
                    val discovered = stats["discovered"] as Int
                    val duplicates = stats["duplicates"] as Int

                    txtStatAdded.text = "📥 Added to Pending Approval: +$added new jobs"
                    txtStatDiscovered.text = "🔍 Total Discovered: $discovered jobs"
                    txtStatDuplicates.text = "♻️ Duplicates Skipped: $duplicates jobs"
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

    private fun triggerFetchAllCompanies() {
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

        txtTitle.text = "⚡ Syncing All Technical Jobs"
        txtSubtitle.text = "Fetching technical openings across all active service & product companies..."

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
                val snapshot = firestore.collection("job_sources").get().await()
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "enabled", true)
                }
                batch.commit().await()

                val stats = JobFetchEngine.fetchAndStoreJobs()
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) {
                        dialog.dismiss()
                        return@withContext
                    }
                    progressIndicator.visibility = View.GONE
                    txtTitle.text = "All Sources Synced Successfully!"
                    txtSubtitle.text = "Summary of technical jobs added to Pending Approval:"

                    val added = stats["added"] as Int
                    val discovered = stats["discovered"] as Int
                    val duplicates = stats["duplicates"] as Int

                    txtStatAdded.text = "📥 Added to Pending Approval: +$added new jobs"
                    txtStatDiscovered.text = "🔍 Total Discovered: $discovered jobs"
                    txtStatDuplicates.text = "♻️ Duplicates Skipped: $duplicates jobs"
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
                    txtTitle.text = "Batch Sync Failed"
                    txtSubtitle.text = "Error: ${e.localizedMessage}"
                    layoutActions.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        fun getCleanCompanyName(source: JobSource): String {
            val raw = source.name
                .replace(Regex("^(Greenhouse|Lever|Ashby|SmartRecruiters|Remotive|Jobicy|Arbeitnow)\\s*-\\s*", RegexOption.IGNORE_CASE), "")
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
}
