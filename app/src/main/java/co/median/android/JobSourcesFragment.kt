package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class JobSourcesFragment : Fragment() {

    private lateinit var btnGlobalSync: MaterialButton
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

        btnGlobalSync = view.findViewById(R.id.btnTriggerGlobalSync)
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

        btnGlobalSync.setOnClickListener {
            triggerGlobalSync()
        }

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
                // Fetch existing sources immediately for lightning-fast UI loading
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

                // Upsert defaults in the background so new sources are added without blocking loading
                CoroutineScope(Dispatchers.IO).launch {
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
        Toast.makeText(ctx, "Testing source: ${source.name}...", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = JobFetchEngine.fetchSingleSource(source)
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "${source.name}: Discovered: ${stats["discovered"]}, Added: ${stats["added"]}, Duplicates: ${stats["duplicates"]}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadSources()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerGlobalSync() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Running global synchronization...", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stats = JobFetchEngine.fetchAndStoreJobs()
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Global sync complete! Discovered: ${stats["discovered"]}, Added: ${stats["added"]}, Duplicates: ${stats["duplicates"]}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadSources()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Sync failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerFetchAllCompanies() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Enabling all companies and starting full batch sync...", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
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
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "All companies synced! Discovered: ${stats["discovered"]}, Added: ${stats["added"]}, Duplicates: ${stats["duplicates"]}",
                        Toast.LENGTH_LONG
                    ).show()
                    loadSources()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || context == null) return@withContext
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Fetch All failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
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

            btnTest.setOnClickListener { onTestFetch(source) }
        }
    }
}
