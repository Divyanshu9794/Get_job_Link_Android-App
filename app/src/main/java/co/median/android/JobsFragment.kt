package co.median.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class JobsFragment : Fragment() {

    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var filterBtn: Button
    private lateinit var sortBtn: Button
    private lateinit var activeFiltersCount: TextView

    private val jobViewModel = JobViewModel()
    private lateinit var jobAdapter: JobAdapter

    private var selectedSortIndex = 0 // 0: Latest, 1: Oldest
    private var currentFilterState = JobFilterState()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jobs, container, false)

        searchInput = view.findViewById(R.id.jobsSearchInput)
        recyclerView = view.findViewById(R.id.jobsRecyclerView)
        progressBar = view.findViewById(R.id.jobsProgressBar)
        emptyView = view.findViewById(R.id.jobsEmptyView)
        swipeRefreshLayout = view.findViewById(R.id.jobsSwipeRefresh)
        filterBtn = view.findViewById(R.id.filterBtn)
        sortBtn = view.findViewById(R.id.sortBtn)
        activeFiltersCount = view.findViewById(R.id.activeFiltersCount)

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
            jobViewModel.fetchJobs()
        }

        sortBtn.setOnClickListener {
            val options = arrayOf("Latest", "Oldest")
            AlertDialog.Builder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(options, selectedSortIndex) { dialog, which ->
                    selectedSortIndex = which
                    jobViewModel.setSortOrder(which == 0)
                    sortBtn.text = "↕ Sort: ${options[which]}"
                    dialog.dismiss()
                }
                .show()
        }

        filterBtn.setOnClickListener {
            val bottomSheet = JobFilterBottomSheetFragment(currentFilterState) { newState ->
                jobViewModel.updateFilterState(newState)
            }
            bottomSheet.show(parentFragmentManager, "JobFilterBottomSheet")
        }

        val initialQuery = arguments?.getString("search_query")
        if (!initialQuery.isNullOrBlank()) {
            searchInput.setText(initialQuery)
            jobViewModel.searchJobs(initialQuery)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                jobViewModel.searchJobs(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        jobViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
            if (!loading) {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        jobViewModel.filterState.observe(viewLifecycleOwner) { state ->
            currentFilterState = state
            val count = state.activeFilterCount()
            if (count > 0) {
                activeFiltersCount.text = "Filter • $count"
            } else {
                activeFiltersCount.text = ""
            }
        }

        jobViewModel.filteredJobs.observe(viewLifecycleOwner) { jobs ->
            swipeRefreshLayout.isRefreshing = false
            jobAdapter.updateJobs(jobs)
            emptyView.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        jobViewModel.fetchJobs()
    }
}
