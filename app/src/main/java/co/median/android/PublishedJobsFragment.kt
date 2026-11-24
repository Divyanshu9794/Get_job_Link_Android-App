package co.median.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PublishedJobsFragment : Fragment() {

    private lateinit var btnAddJob: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var adminAdapter: AdminJobAdapter

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_published_jobs, container, false)

        btnAddJob = view.findViewById(R.id.btnAddJob)
        swipeRefreshLayout = view.findViewById(R.id.adminSwipeRefresh)
        recyclerView = view.findViewById(R.id.adminJobsRecyclerView)
        progressBar = view.findViewById(R.id.adminProgressBar)
        emptyView = view.findViewById(R.id.adminEmptyView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adminAdapter = AdminJobAdapter(mutableListOf(),
            onEdit = { job ->
                val intent = Intent(requireContext(), AdminJobFormActivity::class.java).apply {
                    putExtra("job_id", job.id)
                }
                startActivity(intent)
            },
            onDelete = { job ->
                showDeleteConfirmationDialog(job)
            }
        )
        recyclerView.adapter = adminAdapter

        btnAddJob.setOnClickListener {
            val intent = Intent(requireContext(), AdminJobFormActivity::class.java)
            startActivity(intent)
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadAdminJobs()
        }

        loadAdminJobs()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadAdminJobs()
    }

    private fun loadAdminJobs() {
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
                val snapshot = firestore.collection("jobs")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val jobsList = snapshot.documents.map { Job.fromDocument(it) }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    if (jobsList.isNotEmpty()) {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adminAdapter.updateJobs(jobsList)
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = "No job listings found."
                    }
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
                "Permission Denied: You do not have administrator privileges to manage published jobs."
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication Error: Please log in again."
            msg.contains("PERMISSION_DENIED", true) ->
                "Permission Denied: Admin privileges required."
            msg.contains("unavailable", true) || msg.contains("network", true) ->
                "Network Unavailable: Please check your connection."
            else -> "Failed to load jobs: $msg"
        }
    }

    private fun showDeleteConfirmationDialog(job: Job) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Job?")
            .setMessage("Are you sure you want to delete '${job.title}' at ${job.company}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteJob(job.id)
            }
            .show()
    }

    private fun deleteJob(jobId: String) {
        if (jobId.isBlank()) return
        progressBar.visibility = View.VISIBLE
        firestore.collection("jobs").document(jobId)
            .delete()
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Job deleted successfully", Toast.LENGTH_SHORT).show()
                loadAdminJobs()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), parseFirestoreError(e), Toast.LENGTH_LONG).show()
            }
    }

    private class AdminJobAdapter(
        private val jobs: MutableList<Job>,
        private val onEdit: (Job) -> Unit,
        private val onDelete: (Job) -> Unit
    ) : RecyclerView.Adapter<AdminJobViewHolder>() {

        fun updateJobs(newJobs: List<Job>) {
            jobs.clear()
            jobs.addAll(newJobs)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminJobViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_job, parent, false)
            return AdminJobViewHolder(view)
        }

        override fun onBindViewHolder(holder: AdminJobViewHolder, position: Int) {
            val job = jobs[position]
            holder.bind(job, onEdit, onDelete)
        }

        override fun getItemCount(): Int = jobs.size
    }

    private class AdminJobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.adminJobTitle)
        private val txtCompany: TextView = itemView.findViewById(R.id.adminJobCompany)
        private val txtType: TextView = itemView.findViewById(R.id.adminJobType)
        private val txtDate: TextView = itemView.findViewById(R.id.adminJobDate)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEditJob)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteJob)

        fun bind(job: Job, onEdit: (Job) -> Unit, onDelete: (Job) -> Unit) {
            txtTitle.text = job.title
            txtCompany.text = "${job.company} • ${job.location.ifBlank { "Remote" }}"
            txtType.text = job.jobType.ifBlank { "Full Time" }
            txtDate.text = job.postedDate

            btnEdit.setOnClickListener { onEdit(job) }
            btnDelete.setOnClickListener { onDelete(job) }
        }
    }
}
