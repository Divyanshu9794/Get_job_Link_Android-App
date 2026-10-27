package co.median.android

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PendingJobsFragment : Fragment() {

    private lateinit var editSearch: TextInputEditText
    private lateinit var checkboxSelectAll: CheckBox
    private lateinit var txtSelectedCount: TextView
    private lateinit var btnBulkApprove: MaterialButton
    private lateinit var btnBulkReject: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private lateinit var adapter: PendingJobsAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val allPendingJobs = mutableListOf<PendingJob>()
    private val selectedJobIds = mutableSetOf<String>()
    private var isUpdatingSelectAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pending_jobs, container, false)

        editSearch = view.findViewById(R.id.editSearchPending)
        checkboxSelectAll = view.findViewById(R.id.checkboxSelectAll)
        txtSelectedCount = view.findViewById(R.id.txtSelectedCount)
        btnBulkApprove = view.findViewById(R.id.btnBulkApprove)
        btnBulkReject = view.findViewById(R.id.btnBulkReject)
        swipeRefreshLayout = view.findViewById(R.id.pendingSwipeRefresh)
        recyclerView = view.findViewById(R.id.pendingJobsRecyclerView)
        progressBar = view.findViewById(R.id.pendingProgressBar)
        emptyView = view.findViewById(R.id.pendingEmptyView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PendingJobsAdapter(
            jobs = mutableListOf(),
            selectedIds = selectedJobIds,
            onSelectChanged = { jobId, isChecked ->
                if (jobId.isNotBlank()) {
                    if (isChecked) selectedJobIds.add(jobId) else selectedJobIds.remove(jobId)
                }
                updateSelectionUI()
            },
            onViewDetails = { job -> showJobDetailsDialog(job) },
            onApprove = { job -> approveJob(job) },
            onReject = { job -> showRejectDialog(job.id) }
        )
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener { loadPendingJobs() }

        checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSelectAll) return@setOnCheckedChangeListener
            isUpdatingSelectAll = true

            val currentJobs = adapter.getCurrentJobs()
            if (isChecked) {
                selectedJobIds.addAll(currentJobs.map { it.id }.filter { it.isNotBlank() })
            } else {
                selectedJobIds.removeAll(currentJobs.map { it.id }.toSet())
            }
            adapter.notifyDataSetChanged()
            isUpdatingSelectAll = false
            updateSelectionUI()
        }

        btnBulkApprove.setOnClickListener {
            if (selectedJobIds.isEmpty()) {
                Toast.makeText(requireContext(), "No jobs selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bulkApproveJobs(selectedJobIds.toList())
        }

        btnBulkReject.setOnClickListener {
            if (selectedJobIds.isEmpty()) {
                Toast.makeText(requireContext(), "No jobs selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bulkRejectJobs(selectedJobIds.toList())
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterJobs(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadPendingJobs()
        updateSelectionUI()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadPendingJobs()
    }

    private fun updateSelectionUI() {
        val count = selectedJobIds.size
        txtSelectedCount.text = "$count selected"

        btnBulkApprove.text = if (count > 0) "Approve Selected ($count)" else "Approve Selected"
        btnBulkReject.text = if (count > 0) "Reject Selected ($count)" else "Reject Selected"

        val currentJobs = adapter.getCurrentJobs()
        val isAllSelected = currentJobs.isNotEmpty() && currentJobs.all { it.id.isNotBlank() && selectedJobIds.contains(it.id) }

        isUpdatingSelectAll = true
        checkboxSelectAll.isChecked = isAllSelected
        isUpdatingSelectAll = false
    }

    private fun loadPendingJobs() {
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
                val snapshot = firestore.collection("pending_jobs")
                    .whereEqualTo("status", "PENDING_APPROVAL")
                    .get()
                    .await()

                val list = snapshot.documents.map { PendingJob.fromDocument(it) }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    allPendingJobs.clear()
                    allPendingJobs.addAll(list)
                    filterJobs(editSearch.text?.toString() ?: "")
                }
            } catch (e: Exception) {
                val errorMsg = parseFirestoreError(e)
                withContext(Dispatchers.Main) {
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
                "Permission Denied: You do not have administrator privileges to access pending jobs."
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication Error: Please log in again."
            msg.contains("PERMISSION_DENIED", true) ->
                "Permission Denied: Admin privileges required."
            msg.contains("unavailable", true) || msg.contains("network", true) ->
                "Network Unavailable: Please check your connection."
            else -> "Failed to load pending jobs: $msg"
        }
    }

    private fun filterJobs(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allPendingJobs
        } else {
            allPendingJobs.filter {
                it.title.lowercase().contains(q) || it.company.lowercase().contains(q) || it.source.lowercase().contains(q)
            }
        }

        if (filtered.isNotEmpty()) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateJobs(filtered)
        } else {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No pending jobs for approval."
            adapter.updateJobs(emptyList())
        }
        updateSelectionUI()
    }

    private fun approveJob(job: PendingJob) {
        progressBar.visibility = View.VISIBLE
        val currentUser = FirebaseAuth.getInstance().currentUser
        val adminUid = currentUser?.uid ?: "unknown_admin"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Check duplicate against published jobs
                val pubSnapshot = firestore.collection("jobs")
                    .whereEqualTo("title", job.title)
                    .whereEqualTo("company", job.company)
                    .get()
                    .await()

                if (!pubSnapshot.isEmpty) {
                    firestore.collection("pending_jobs").document(job.id)
                        .update("status", "DUPLICATE").await()
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Job is a duplicate. Marked as DUPLICATE.", Toast.LENGTH_LONG).show()
                        selectedJobIds.remove(job.id)
                        updateSelectionUI()
                        loadPendingJobs()
                    }
                    return@launch
                }

                val publishedMap = mapOf(
                    "title" to job.title,
                    "company" to job.company.trim(),
                    "location" to job.location,
                    "jobType" to job.jobType,
                    "domain" to "Engineering",
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
                    "isRemote" to job.location.contains("Remote", true),
                    "remote" to job.location.contains("Remote", true),
                    "source" to (job.source.ifBlank { "Auto" }),
                    "postedDate" to job.postedDate,
                    "date" to java.time.LocalDate.now().toString(),
                    "createdAt" to System.currentTimeMillis(),
                    "approvedBy" to adminUid,
                    "approvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                val newPubRef = firestore.collection("jobs").document()
                newPubRef.set(publishedMap).await()

                // 3. Update pending job status
                firestore.collection("pending_jobs").document(job.id).update(
                    mapOf(
                        "status" to "APPROVED",
                        "publishedJobId" to newPubRef.id,
                        "approvedBy" to adminUid,
                        "approvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                // 4. Log approval
                firestore.collection("job_approval_logs").add(
                    mapOf(
                        "jobId" to job.id,
                        "publishedJobId" to newPubRef.id,
                        "action" to "APPROVE",
                        "adminUid" to adminUid,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Job approved & published successfully!", Toast.LENGTH_SHORT).show()
                    selectedJobIds.remove(job.id)
                    updateSelectionUI()
                    loadPendingJobs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), parseFirestoreError(e), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRejectDialog(jobId: String) {
        val input = TextInputEditText(requireContext()).apply {
            hint = "Reason for rejection (optional)"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject Job")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                rejectJob(jobId, reason)
            }
            .show()
    }

    private fun rejectJob(jobId: String, reason: String) {
        progressBar.visibility = View.VISIBLE
        val currentUser = FirebaseAuth.getInstance().currentUser
        val adminUid = currentUser?.uid ?: "unknown_admin"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("pending_jobs").document(jobId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "rejectionReason" to reason.ifBlank { "No reason provided" },
                        "rejectedBy" to adminUid,
                        "rejectedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                firestore.collection("job_approval_logs").add(
                    mapOf(
                        "jobId" to jobId,
                        "action" to "REJECT",
                        "reason" to reason,
                        "adminUid" to adminUid,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Job rejected.", Toast.LENGTH_SHORT).show()
                    selectedJobIds.remove(jobId)
                    updateSelectionUI()
                    loadPendingJobs()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), parseFirestoreError(e), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bulkApproveJobs(jobIds: List<String>) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            for (id in jobIds) {
                try {
                    val doc = firestore.collection("pending_jobs").document(id).get().await()
                    if (doc.exists()) {
                        val job = PendingJob.fromDocument(doc)
                        val publishedMap = mapOf(
                            "title" to job.title,
                            "company" to job.company.trim(),
                            "location" to job.location,
                            "jobType" to job.jobType,
                            "domain" to "Engineering",
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
                            "isRemote" to job.location.contains("Remote", true),
                            "remote" to job.location.contains("Remote", true),
                            "source" to (job.source.ifBlank { "Auto" }),
                            "postedDate" to job.postedDate,
                            "date" to java.time.LocalDate.now().toString(),
                            "createdAt" to System.currentTimeMillis(),
                            "approvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        val newPubRef = firestore.collection("jobs").document()
                        newPubRef.set(publishedMap).await()
                        firestore.collection("pending_jobs").document(id).update("status", "APPROVED", "publishedJobId", newPubRef.id).await()
                        successCount++
                    }
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Successfully approved $successCount / ${jobIds.size} jobs", Toast.LENGTH_LONG).show()
                selectedJobIds.clear()
                updateSelectionUI()
                loadPendingJobs()
            }
        }
    }

    private fun bulkRejectJobs(jobIds: List<String>) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            for (id in jobIds) {
                try {
                    firestore.collection("pending_jobs").document(id).update("status", "REJECTED", "rejectionReason", "Bulk rejected").await()
                    successCount++
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Successfully rejected $successCount / ${jobIds.size} jobs", Toast.LENGTH_LONG).show()
                selectedJobIds.clear()
                updateSelectionUI()
                loadPendingJobs()
            }
        }
    }

    private fun showJobDetailsDialog(job: PendingJob) {
        val details = """
            Company: ${job.company}
            Role: ${job.title}
            Location: ${job.location}
            Job Type: ${job.jobType}
            Experience: ${job.experience}
            Salary: ${job.salary}
            Source: ${job.source}
            Application URL: ${job.applicationUrl}
            
            Required Skills:
            ${job.requiredSkills.joinToString(", ")}
            
            Description:
            ${job.description}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(job.title)
            .setMessage(details)
            .setPositiveButton("Approve") { _, _ -> approveJob(job) }
            .setNeutralButton("Close", null)
            .setNegativeButton("Reject") { _, _ -> showRejectDialog(job.id) }
            .show()
    }

    private class PendingJobsAdapter(
        private val jobs: MutableList<PendingJob>,
        private val selectedIds: Set<String>,
        private val onSelectChanged: (String, Boolean) -> Unit,
        private val onViewDetails: (PendingJob) -> Unit,
        private val onApprove: (PendingJob) -> Unit,
        private val onReject: (PendingJob) -> Unit
    ) : RecyclerView.Adapter<PendingJobViewHolder>() {

        fun updateJobs(newJobs: List<PendingJob>) {
            jobs.clear()
            jobs.addAll(newJobs)
            notifyDataSetChanged()
        }

        fun getCurrentJobs(): List<PendingJob> = jobs

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingJobViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_job, parent, false)
            return PendingJobViewHolder(view)
        }

        override fun onBindViewHolder(holder: PendingJobViewHolder, position: Int) {
            val job = jobs[position]
            holder.bind(job, selectedIds.contains(job.id), onSelectChanged, onViewDetails, onApprove, onReject)
        }

        override fun getItemCount(): Int = jobs.size
    }

    private class PendingJobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkboxSelectJob)
        private val txtTitle: TextView = itemView.findViewById(R.id.pendingJobTitle)
        private val txtCompany: TextView = itemView.findViewById(R.id.pendingJobCompany)
        private val txtSalary: TextView = itemView.findViewById(R.id.pendingJobSalary)
        private val txtType: TextView = itemView.findViewById(R.id.pendingJobType)
        private val txtSkills: TextView = itemView.findViewById(R.id.pendingJobSkills)
        private val chipSource: Chip = itemView.findViewById(R.id.chipSource)
        private val btnDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        private val btnReject: MaterialButton = itemView.findViewById(R.id.btnRejectJob)
        private val btnApprove: MaterialButton = itemView.findViewById(R.id.btnApproveJob)

        fun bind(
            job: PendingJob,
            isSelected: Boolean,
            onSelectChanged: (String, Boolean) -> Unit,
            onViewDetails: (PendingJob) -> Unit,
            onApprove: (PendingJob) -> Unit,
            onReject: (PendingJob) -> Unit
        ) {
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = isSelected
            checkBox.setOnCheckedChangeListener { _, isChecked -> onSelectChanged(job.id, isChecked) }

            txtTitle.text = job.title
            txtCompany.text = "${job.company} • ${job.location}"
            txtSalary.text = job.salary
            txtType.text = job.jobType
            txtSkills.text = "Skills: " + (job.requiredSkills.ifEmpty { listOf("General") }).joinToString(", ")
            chipSource.text = job.source.ifBlank { "Auto" }

            btnDetails.setOnClickListener { onViewDetails(job) }
            btnApprove.setOnClickListener { onApprove(job) }
            btnReject.setOnClickListener { onReject(job) }
        }
    }
}
