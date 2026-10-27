package co.median.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JobAdapter(
    private var jobs: List<Job>,
    private val onItemClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.jobTitle)
        val company: TextView = view.findViewById(R.id.companyName)
        val location: TextView = view.findViewById(R.id.jobLocation)
        val type: TextView = view.findViewById(R.id.jobType)
        val experience: TextView = view.findViewById(R.id.jobExperience)
        val salary: TextView = view.findViewById(R.id.jobSalary)
        val skills: TextView = view.findViewById(R.id.jobSkills)
        val postedDate: TextView = view.findViewById(R.id.jobPostedDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.title.text = job.title
        holder.company.text = job.company
        holder.location.text = job.location.ifBlank { "Remote / On-site" }
        holder.type.text = job.jobType.ifBlank { "Full Time" }
        holder.experience.text = job.experience.ifBlank { "All Levels" }
        holder.salary.text = job.salary.ifBlank { "Competitive" }
        holder.skills.text = if (job.skills.isNotEmpty()) job.skills.joinToString(" • ") else "General"
        holder.postedDate.text = if (job.postedDate.isNotBlank()) {
            if (job.postedDate.startsWith("Posted", ignoreCase = true)) {
                job.postedDate
            } else {
                "Posted ${job.postedDate}"
            }
        } else {
            "Posted recently"
        }

        holder.itemView.setOnClickListener { onItemClick(job) }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
}
