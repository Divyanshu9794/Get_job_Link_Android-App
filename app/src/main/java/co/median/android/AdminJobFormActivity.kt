package co.median.android

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AdminJobFormActivity : AppCompatActivity() {

    private lateinit var editJobTitle: EditText
    private lateinit var editCompany: EditText
    private lateinit var editApplicationUrl: EditText
    private lateinit var spinnerJobType: Spinner
    private lateinit var spinnerDomain: Spinner
    private lateinit var spinnerExperience: Spinner
    private lateinit var editSalary: EditText
    private lateinit var switchRemote: SwitchCompat
    private lateinit var editLocation: EditText
    private lateinit var editDescription: EditText
    private lateinit var editRequiredSkills: EditText
    private lateinit var editPreferredSkills: EditText
    private lateinit var btnPreviewJob: MaterialButton
    private lateinit var btnPublishJob: MaterialButton

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var editingJobId: String? = null

    private val jobTypes = arrayOf("Full Time", "Internship", "Contract", "Part Time")
    private val domains = arrayOf("Engineering", "Design", "Marketing", "Management", "Data Entry", "Other")
    private val experiences = arrayOf("More than 0 year", "More than 1 year", "More than 2 years", "More than 3 years", "More than 4 years")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_job_form)

        val toolbar = findViewById<MaterialToolbar>(R.id.adminFormToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        editJobTitle = findViewById(R.id.editJobTitle)
        editCompany = findViewById(R.id.editCompany)
        editApplicationUrl = findViewById(R.id.editApplicationUrl)
        spinnerJobType = findViewById(R.id.spinnerJobType)
        spinnerDomain = findViewById(R.id.spinnerDomain)
        spinnerExperience = findViewById(R.id.spinnerExperience)
        editSalary = findViewById(R.id.editSalary)
        switchRemote = findViewById(R.id.switchRemote)
        editLocation = findViewById(R.id.editLocation)
        editDescription = findViewById(R.id.editDescription)
        editRequiredSkills = findViewById(R.id.editRequiredSkills)
        editPreferredSkills = findViewById(R.id.editPreferredSkills)
        btnPreviewJob = findViewById(R.id.btnPreviewJob)
        btnPublishJob = findViewById(R.id.btnPublishJob)

        setupSpinners()

        editingJobId = intent.getStringExtra("job_id")
        if (!editingJobId.isNullOrBlank()) {
            toolbar.title = "Edit Job Listing"
            btnPublishJob.text = "Save Changes"
            loadJobForEditing(editingJobId!!)
        } else {
            toolbar.title = "Add Job Listing"
            btnPublishJob.text = "Publish Job"
        }

        btnPreviewJob.setOnClickListener {
            previewJob()
        }

        btnPublishJob.setOnClickListener {
            publishOrSaveJob()
        }
    }

    private fun setupSpinners() {
        spinnerJobType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jobTypes)
        spinnerDomain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, domains)
        spinnerExperience.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, experiences)
    }

    private fun loadJobForEditing(jobId: String) {
        firestore.collection("jobs").document(jobId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val job = Job.fromDocument(doc)
                    editJobTitle.setText(job.title)
                    editCompany.setText(job.company)
                    editApplicationUrl.setText(job.applicationUrl)
                    editSalary.setText(job.salary)
                    editLocation.setText(job.location)
                    editDescription.setText(job.description)
                    switchRemote.isChecked = job.location.contains("Remote", true) || job.location.isBlank()

                    editRequiredSkills.setText(job.requiredSkills.joinToString(", "))
                    editPreferredSkills.setText(job.preferredSkills.joinToString(", "))

                    // Select spinners
                    jobTypes.indexOf(job.jobType).takeIf { it >= 0 }?.let { spinnerJobType.setSelection(it) }
                    // Domain or fallback
                    val domainVal = doc.getString("domain") ?: "Engineering"
                    domains.indexOf(domainVal).takeIf { it >= 0 }?.let { spinnerDomain.setSelection(it) }

                    val expVal = doc.getString("experience") ?: doc.getString("experienceLabel") ?: ""
                    experiences.indexOf(expVal).takeIf { it >= 0 }?.let { spinnerExperience.setSelection(it) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load job details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun previewJob() {
        val title = editJobTitle.text.toString().trim()
        val company = editCompany.text.toString().trim()
        val url = editApplicationUrl.text.toString().trim()
        val description = editDescription.text.toString().trim()

        if (title.isBlank() || company.isBlank() || description.isBlank()) {
            Toast.makeText(this, "Please enter at least Title, Company, and Description for preview.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("job_title", title)
            putExtra("job_company", company)
            putExtra("job_location", editLocation.text.toString().trim().ifBlank { if (switchRemote.isChecked) "Remote" else "On-site" })
            putExtra("job_type", spinnerJobType.selectedItem.toString())
            putExtra("job_description", description)
            putExtra("job_url", url)
        }
        startActivity(intent)
    }

    private fun publishOrSaveJob() {
        val title = editJobTitle.text.toString().trim()
        val company = editCompany.text.toString().trim()
        val url = editApplicationUrl.text.toString().trim()
        val description = editDescription.text.toString().trim()
        val salary = editSalary.text.toString().trim()
        val location = editLocation.text.toString().trim()
        val isRemote = switchRemote.isChecked
        val jobType = spinnerJobType.selectedItem.toString()
        val domain = spinnerDomain.selectedItem.toString()
        val experienceLabel = spinnerExperience.selectedItem.toString()
        val experienceYears = experienceLabel.filter { it.isDigit() }.toIntOrNull() ?: 0

        val requiredSkillsStr = editRequiredSkills.text.toString().trim()
        val requiredSkillsList = if (requiredSkillsStr.isNotBlank()) {
            requiredSkillsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val preferredSkillsStr = editPreferredSkills.text.toString().trim()
        val preferredSkillsList = if (preferredSkillsStr.isNotBlank()) {
            preferredSkillsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        // Validation
        if (title.isBlank()) {
            editJobTitle.error = "Job title is required."
            editJobTitle.requestFocus()
            return
        }
        if (company.isBlank()) {
            editCompany.error = "Company name is required."
            editCompany.requestFocus()
            return
        }
        if (url.isBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
            editApplicationUrl.error = "Please enter a valid application URL."
            editApplicationUrl.requestFocus()
            return
        }
        if (description.isBlank()) {
            editDescription.error = "Job description is required."
            editDescription.requestFocus()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val finalLocation = location.ifBlank { if (isRemote) "Remote" else "On-site" }

        val jobMap = mutableMapOf<String, Any>(
            "title" to title,
            "company" to company,
            "applicationUrl" to url,
            "url" to url,
            "applyUrl" to url,
            "link" to url,
            "jobType" to jobType,
            "domain" to domain,
            "experience" to experienceLabel,
            "experienceYears" to experienceYears,
            "salary" to salary,
            "remote" to isRemote,
            "location" to finalLocation,
            "description" to description,
            "requiredSkills" to requiredSkillsList,
            "preferredSkills" to preferredSkillsList,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        btnPublishJob.isEnabled = false

        val docRef = if (!editingJobId.isNullOrBlank()) {
            firestore.collection("jobs").document(editingJobId!!)
        } else {
            jobMap["createdAt"] = FieldValue.serverTimestamp()
            jobMap["createdBy"] = currentUser?.uid ?: ""
            jobMap["createdByEmail"] = currentUser?.email ?: ""
            firestore.collection("jobs").document()
        }

        docRef.set(jobMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, if (editingJobId != null) "Job updated successfully!" else "Job published successfully! Push notification triggered.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnPublishJob.isEnabled = true
                Toast.makeText(this, "Failed to save job: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}
