package co.median.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JobDetailsActivity : AppCompatActivity() {

    private var currentJob: Job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        val rootLayout = findViewById<View>(R.id.detailRootLayout)
        val appBarLayout = findViewById<AppBarLayout>(R.id.detailAppBarLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.detailToolbar)
        val scrollView = findViewById<ScrollView>(R.id.detailScrollView)

        // Handle System Windows Insets (Status bar, Camera Notch / Display Cutout, System Nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            appBarLayout.setPadding(0, statusBarInsets.top, 0, 0)
            scrollView.setPadding(0, 0, 0, navBarInsets.bottom)
            insets
        }

        // Back button navigation
        toolbar.setNavigationOnClickListener {
            handleBackNavigation()
        }

        val jobId = intent.getStringExtra("job_id") ?: intent.getStringExtra("jobId") ?: ""
        val title = intent.getStringExtra("job_title")
        val company = intent.getStringExtra("job_company")
        val location = intent.getStringExtra("job_location")
        val type = intent.getStringExtra("job_type")
        val description = intent.getStringExtra("job_description")
        val url = intent.getStringExtra("job_url")

        currentJob = Job(
            id = jobId,
            title = title ?: "",
            company = company ?: "",
            location = location ?: "",
            jobType = type ?: "",
            description = description ?: "",
            applicationUrl = url ?: ""
        )

        if (!title.isNullOrBlank() && !description.isNullOrBlank() && description != "A new opportunity has been posted. Tap to view the job.") {
            setupJobDetailsUI(title, company ?: "", location ?: "", type ?: "", description, url ?: "")
        } else if (jobId.isNotBlank()) {
            fetchJobFromFirestore(jobId)
        } else if (!title.isNullOrBlank()) {
            setupJobDetailsUI(title, company ?: "", location ?: "", type ?: "", description ?: "", url ?: "")
        } else {
            showJobNotAvailableState()
        }

        setupMatchScoreButton()
    }

    override fun onResume() {
        super.onResume()
        setupMatchScoreButton()
    }

    private fun setupMatchScoreButton() {
        val btnCheckMatch = findViewById<MaterialButton>(R.id.btnCheckMatch) ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profile = FirebaseProfileRepository.loadLocalProfile(this)
        val hasResume = profile.resumeFileName.isNotBlank() || profile.resumeDownloadUrl.isNotBlank() || profile.resumeData.isNotBlank() || profile.resumeStoragePath.isNotBlank()

        when {
            currentUser == null -> {
                btnCheckMatch.text = "Sign in to Check Match"
                btnCheckMatch.setOnClickListener {
                    val intent = Intent(this, AuthActivity::class.java)
                    startActivity(intent)
                }
            }
            !hasResume -> {
                btnCheckMatch.text = "Upload Resume to Check Match"
                btnCheckMatch.setOnClickListener {
                    val intent = Intent(this, EditProfileActivity::class.java)
                    startActivity(intent)
                }
            }
            else -> {
                btnCheckMatch.text = "Check Match Score"
                btnCheckMatch.setOnClickListener {
                    showMatchScoreDialog(profile, currentJob)
                }
            }
        }
    }

    private fun showMatchScoreDialog(profile: UserProfile, job: Job) {
        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            setPadding(40, 40, 40, 40)
        }
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Analyzing your resume...")
            .setMessage("Comparing skills with this job...")
            .setView(progressBar)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            loadingDialog.dismiss()
            val result = ProfileMatchingHelper.calculateResumeMatch(profile, job)

            if (result.isUnavailable) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Match Score Unavailable")
                    .setMessage(result.unavailabilityReason)
                    .setPositiveButton("Update Resume") { _, _ ->
                        startActivity(Intent(this, EditProfileActivity::class.java))
                    }
                    .setNegativeButton("Close", null)
                    .show()
                return@postDelayed
            }

            val messageBuilder = StringBuilder()
            messageBuilder.append("Based on your uploaded resume:\n\n")
            messageBuilder.append("Matched ${result.matchedCriteria.size} of ${result.matchedCriteria.size + result.missingCriteria.size} relevant skills.\n\n")

            if (result.matchedCriteria.isNotEmpty()) {
                messageBuilder.append("✓ Matched Skills:\n")
                for (skill in result.matchedCriteria) {
                    messageBuilder.append(" • $skill\n")
                }
                messageBuilder.append("\n")
            }

            if (result.missingCriteria.isNotEmpty()) {
                messageBuilder.append("• Skills to Improve / Missing:\n")
                for (skill in result.missingCriteria) {
                    messageBuilder.append(" • $skill\n")
                }
            } else {
                messageBuilder.append("All identified job skills are present in your resume.\n")
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Match Score: ${result.matchPercentage}%")
                .setMessage(messageBuilder.toString())
                .setIcon(R.drawable.ic_work)
                .setPositiveButton("Close", null)
                .show()
        }, 500)
    }

    private fun handleBackNavigation() {
        if (isTaskRoot) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBackNavigation()
        super.onBackPressed()
    }

    private fun fetchJobFromFirestore(jobId: String) {
        FirebaseFirestore.getInstance()
            .collection("jobs")
            .document(jobId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val job = Job.fromDocument(doc)
                    currentJob = job
                    setupJobDetailsUI(
                        job.title,
                        job.company,
                        job.location,
                        job.jobType,
                        job.description,
                        job.applicationUrl
                    )
                } else {
                    showJobNotAvailableState()
                }
            }
            .addOnFailureListener {
                showJobNotAvailableState()
            }
    }

    private fun showJobNotAvailableState() {
        findViewById<TextView>(R.id.detailTitle).text = "Job no longer available"
        findViewById<TextView>(R.id.detailCompany).text = ""
        findViewById<TextView>(R.id.detailLocation).text = ""
        findViewById<TextView>(R.id.detailType).text = ""
        findViewById<TextView>(R.id.detailDescription).text = "This job is no longer available."
        val applyBtn = findViewById<Button>(R.id.applyButton)
        applyBtn.text = "BROWSE JOBS"
        applyBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupJobDetailsUI(
        title: String,
        company: String,
        location: String,
        type: String,
        description: String,
        url: String
    ) {
        findViewById<TextView>(R.id.detailTitle).text = title
        findViewById<TextView>(R.id.detailCompany).text = company
        findViewById<TextView>(R.id.detailLocation).text = location.ifBlank { "Remote / On-site" }
        findViewById<TextView>(R.id.detailType).text = type.ifBlank { "Full Time" }
        findViewById<TextView>(R.id.detailDescription).text = description

        val applyBtn = findViewById<Button>(R.id.applyButton)
        applyBtn.text = "APPLY NOW"
        applyBtn.setOnClickListener {
            val targetUrl = if (url.isNotBlank()) url else "https://www.google.com/search?q=${Uri.encode("$title $company")}"
            val intent = Intent(this, InAppWebViewActivity::class.java).apply {
                putExtra("url", targetUrl)
                putExtra("title", title)
            }
            startActivity(intent)
        }
    }
}
