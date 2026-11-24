package co.median.android

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
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
    private lateinit var profileAvatarContainer: View
    private lateinit var profileAvatar: ImageView

    private val jobViewModel = JobViewModel()
    private var lastProfileClickTime = 0L

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
        profileAvatarContainer = view.findViewById(R.id.profileAvatarContainer)
        profileAvatar = view.findViewById(R.id.profileAvatar)

        updateProfileUI()

        val profileClickListener = View.OnClickListener {
            openProfileScreen()
        }
        profileAvatarContainer.setOnClickListener(profileClickListener)
        profileAvatar.setOnClickListener(profileClickListener)

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
            openProfileScreen()
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

    private fun openProfileScreen() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProfileClickTime < 800) return
        lastProfileClickTime = currentTime

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.nav_profile

            parentFragmentManager.beginTransaction()
                .replace(R.id.native_fragment_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        } else {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
        }
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

        if (currentUser != null) {
            profileAvatar.imageTintList = null
            if (profile.photoUrl.isNotBlank()) {
                loadAvatarImageIntoView(requireContext(), profile.photoUrl, name, profileAvatar)
            } else {
                profileAvatar.setImageDrawable(createInitialsAvatar(requireContext(), name))
            }
        } else {
            profileAvatar.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
            profileAvatar.setImageResource(R.drawable.ic_profile)
        }

        val completion = profile.calculateCompletionPercentage()
        profileProgress.progress = completion
        profileProgressText.text = "$completion% Complete • Improve job matches"
    }

    private fun loadAvatarImageIntoView(context: Context, url: String, name: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlObj = URL(url)
                val connection = urlObj.openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.connect()
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                withContext(Dispatchers.Main) {
                    imageView.imageTintList = null
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageDrawable(createInitialsAvatar(context, name))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error loading avatar from URL: $url", e)
                withContext(Dispatchers.Main) {
                    imageView.imageTintList = null
                    imageView.setImageDrawable(createInitialsAvatar(context, name))
                }
            }
        }
    }

    private fun createInitialsAvatar(context: Context, name: String): Drawable {
        val initials = name.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "JS" }

        val size = (40 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.colorPrimary)
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paintBackground)

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.4f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val textY = radius - ((paintText.descent() + paintText.ascent()) / 2f)
        canvas.drawText(initials, radius, textY, paintText)

        return BitmapDrawable(context.resources, bitmap)
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
