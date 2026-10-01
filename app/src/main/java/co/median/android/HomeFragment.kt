package co.median.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var scrollView: ScrollView
    private lateinit var greetingText: TextView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var profileProgress: ProgressBar
    private lateinit var profileProgressText: TextView
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var latestRecyclerView: RecyclerView
    private lateinit var viewAllRecommendedBtn: TextView
    private lateinit var viewAllLatestBtn: TextView
    private lateinit var exploreAllBtn: Button
    private lateinit var completeProfileBtn: TextView

    private val jobViewModel = JobViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefresh = view.findViewById(R.id.homeSwipeRefresh)
        scrollView = view.findViewById(R.id.homeScrollView)
        greetingText = view.findViewById(R.id.greetingText)
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        profileProgress = view.findViewById(R.id.profileProgress)
        profileProgressText = view.findViewById(R.id.profileProgressText)
        recommendedRecyclerView = view.findViewById(R.id.recommendedRecyclerView)
        latestRecyclerView = view.findViewById(R.id.latestRecyclerView)
        viewAllRecommendedBtn = view.findViewById(R.id.viewAllRecommendedBtn)
        viewAllLatestBtn = view.findViewById(R.id.viewAllLatestBtn)
        exploreAllBtn = view.findViewById(R.id.exploreAllBtn)
        completeProfileBtn = view.findViewById(R.id.completeProfileBtn)

        updateProfileUI()

        recommendedRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        latestRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Handle WindowInsets for bottom navigation and system navigation bar safe area
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
            val bottomNavHeight = if (bottomNav != null && bottomNav.visibility == View.VISIBLE) bottomNav.height else 0
            val effectiveBottomNav = if (bottomNavHeight > 0) bottomNavHeight else (56 * resources.displayMetrics.density).toInt()
            val spacing = (16 * resources.displayMetrics.density).toInt()
            val totalBottomPadding = effectiveBottomNav + navBarInsets.bottom + spacing
            scrollView.setPadding(scrollView.paddingLeft, scrollView.paddingTop, scrollView.paddingRight, totalBottomPadding)
            insets
        }

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            navigateToJobs(query)
        }

        swipeRefresh.setOnRefreshListener {
            if (swipeRefresh.isRefreshing) {
                return@setOnRefreshListener
            }
            updateProfileUI()
            jobViewModel.fetchJobs()
        }

        val chipClickListener = View.OnClickListener { v ->
            val chipText = (v as TextView).text.toString()
            navigateToJobs(chipText)
        }

        view.findViewById<TextView>(R.id.chipSoftware).setOnClickListener(chipClickListener)
        view.findViewById<TextView>(R.id.chipFrontend).setOnClickListener(chipClickListener)
        view.findViewById<TextView>(R.id.chipBackend).setOnClickListener(chipClickListener)
        view.findViewById<TextView>(R.id.chipReact).setOnClickListener(chipClickListener)
        view.findViewById<TextView>(R.id.chipJava).setOnClickListener(chipClickListener)
        view.findViewById<TextView>(R.id.chipRemote).setOnClickListener(chipClickListener)

        viewAllRecommendedBtn.setOnClickListener { navigateToJobs("") }
        viewAllLatestBtn.setOnClickListener { navigateToJobs("") }
        exploreAllBtn.setOnClickListener { navigateToJobs("") }

        completeProfileBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.native_fragment_container, ProfileFragment())
                .commit()
        }

        jobViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (!loading) {
                swipeRefresh.isRefreshing = false
            }
        }

        jobViewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            swipeRefresh.isRefreshing = false
            if (jobs.isNotEmpty()) {
                val profile = FirebaseProfileRepository.loadLocalProfile(requireContext())
                val sortedByMatch = jobs.sortedByDescending { job ->
                    ProfileMatchingHelper.calculateMatch(profile, job).matchPercentage
                }

                recommendedRecyclerView.adapter = JobAdapter(sortedByMatch.take(5)) { job ->
                    openJobDetails(job)
                }
                latestRecyclerView.adapter = JobAdapter(jobs.take(6)) { job ->
                    openJobDetails(job)
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
        jobViewModel.fetchJobs()
    }

    private fun updateProfileUI() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profile = FirebaseProfileRepository.loadLocalProfile(requireContext())
        val name = currentUser?.displayName ?: profile.name.ifBlank { "Job Seeker" }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }

        greetingText.text = "$greeting, $name"

        val completion = profile.calculateCompletionPercentage()
        profileProgress.progress = completion
        profileProgressText.text = "$completion% Complete • Improve job matches"
    }

    private fun navigateToJobs(query: String) {
        val jobsFrag = JobsFragment().apply {
            arguments = Bundle().apply { putString("search_query", query) }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.native_fragment_container, jobsFrag)
            .addToBackStack(null)
            .commit()
    }

    private fun openJobDetails(job: Job) {
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
}
