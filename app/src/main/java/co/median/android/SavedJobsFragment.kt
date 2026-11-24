package co.median.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedJobsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var jobAdapter: JobAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jobs, container, false)
        view.findViewById<View>(R.id.jobsSearchInput)?.visibility = View.GONE
        view.findViewById<View>(R.id.filterBtn)?.visibility = View.GONE
        view.findViewById<View>(R.id.sortBtn)?.visibility = View.GONE
        view.findViewById<View>(R.id.activeFiltersCount)?.visibility = View.GONE

        recyclerView = view.findViewById(R.id.jobsRecyclerView)
        progressBar = view.findViewById(R.id.jobsProgressBar)
        emptyView = view.findViewById(R.id.jobsEmptyView)
        swipeRefreshLayout = view.findViewById(R.id.jobsSwipeRefresh)
        emptyView.text = "No saved jobs yet."

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        jobAdapter = JobAdapter(emptyList()) { job ->
            val intent = Intent(requireContext(), JobDetailsActivity::class.java).apply {
                putExtra("job_id", job.id)
                putExtra("job_title", job.title)
                putExtra("job_company", job.company)
                putExtra("job_location", job.location)
                putExtra("job_type", job.jobType)
                putExtra("job_experience", job.experience)
                putExtra("job_salary", job.salary)
                putExtra("job_description", job.description)
                putExtra("job_url", job.applicationUrl)
                putExtra("job_posted", job.postedDate)
            }
            startActivity(intent)
        }
        recyclerView.adapter = jobAdapter

        swipeRefreshLayout.setOnRefreshListener {
            loadSavedJobs()
        }

        loadSavedJobs()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadSavedJobs()
    }

    private fun loadSavedJobs() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            swipeRefreshLayout.isRefreshing = false
            emptyView.text = "Please sign in to view saved jobs."
            emptyView.visibility = View.VISIBLE
            return
        }

        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        CoroutineScope(Dispatchers.IO).launch {
            val saved = JobRepository.getSavedJobs(user.uid)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                jobAdapter.updateJobs(saved)
                emptyView.visibility = if (saved.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
