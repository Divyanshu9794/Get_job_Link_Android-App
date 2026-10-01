package co.median.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    private val _jobs = MutableLiveData<List<Job>>()
    val jobs: LiveData<List<Job>> get() = _jobs

    private val _filteredJobs = MutableLiveData<List<Job>>()
    val filteredJobs: LiveData<List<Job>> get() = _filteredJobs

    private val _filterState = MutableLiveData<JobFilterState>(JobFilterState())
    val filterState: LiveData<JobFilterState> get() = _filterState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var currentSortIsLatest = true
    private var currentSearchQuery = ""

    init {
        JobRepository.startRealtimeJobsListener()
        viewModelScope.launch {
            JobRepository.jobsState.collectLatest { jobsList ->
                _isLoading.value = false
                _jobs.value = jobsList
                applyQueryAndSort()
            }
        }
    }

    fun fetchJobs() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val list = JobRepository.getAllJobs()
            _isLoading.value = false
            _jobs.value = list
            applyQueryAndSort()
        }
    }

    fun setSortOrder(isLatest: Boolean) {
        currentSortIsLatest = isLatest
        applyQueryAndSort()
    }

    fun setPostedDateFilter(postedDate: String) {
        val current = _filterState.value ?: JobFilterState()
        _filterState.value = current.copy(postedDateFilter = postedDate)
        applyQueryAndSort()
    }

    fun updateFilterState(newState: JobFilterState) {
        _filterState.value = newState
        applyQueryAndSort()
    }

    fun removeSingleFilter(filterType: String, value: String) {
        val current = _filterState.value ?: JobFilterState()
        val updated = when (filterType) {
            "jobType" -> current.copy(selectedJobTypes = current.selectedJobTypes - value)
            "experience" -> current.copy(selectedExperience = current.selectedExperience - value)
            "workMode" -> current.copy(selectedWorkModes = current.selectedWorkModes - value)
            "skill" -> current.copy(selectedSkills = current.selectedSkills - value)
            "location" -> current.copy(locationQuery = "")
            "company" -> current.copy(companyQuery = "")
            "postedDate" -> current.copy(postedDateFilter = "Anytime")
            else -> current
        }
        _filterState.value = updated
        applyQueryAndSort()
    }

    fun clearAllFilters() {
        _filterState.value = JobFilterState()
        applyQueryAndSort()
    }

    fun searchJobs(query: String) {
        currentSearchQuery = query
        applyQueryAndSort()
    }

    private fun applyQueryAndSort() {
        var list = _jobs.value ?: emptyList()
        val filter = _filterState.value ?: JobFilterState()

        // 1. Posted Date Filter
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        list = when (filter.postedDateFilter) {
            "Last 24 hours" -> list.filter { (now - (if (it.createdAt > 0) it.createdAt else now)) <= dayInMillis }
            "Last 3 days" -> list.filter { (now - (if (it.createdAt > 0) it.createdAt else now)) <= 3 * dayInMillis }
            "Last 7 days" -> list.filter { (now - (if (it.createdAt > 0) it.createdAt else now)) <= 7 * dayInMillis }
            "Last 30 days" -> list.filter { (now - (if (it.createdAt > 0) it.createdAt else now)) <= 30 * dayInMillis }
            else -> list
        }

        // 2. Work Mode Filter
        if (filter.selectedWorkModes.isNotEmpty()) {
            list = list.filter { job ->
                filter.selectedWorkModes.any { mode ->
                    job.location.contains(mode, ignoreCase = true) ||
                            job.jobType.contains(mode, ignoreCase = true) ||
                            job.description.contains(mode, ignoreCase = true)
                }
            }
        }

        // 3. Job Type Filter
        if (filter.selectedJobTypes.isNotEmpty()) {
            list = list.filter { job ->
                filter.selectedJobTypes.any { type ->
                    job.jobType.contains(type, ignoreCase = true) ||
                            job.title.contains(type, ignoreCase = true) ||
                            job.description.contains(type, ignoreCase = true)
                }
            }
        }

        // 4. Experience Filter
        if (filter.selectedExperience.isNotEmpty()) {
            list = list.filter { job ->
                filter.selectedExperience.any { exp ->
                    job.experience.contains(exp, ignoreCase = true) ||
                            job.title.contains(exp, ignoreCase = true) ||
                            job.description.contains(exp, ignoreCase = true)
                }
            }
        }

        // 5. Location Query Filter
        if (filter.locationQuery.isNotBlank()) {
            val locLower = filter.locationQuery.lowercase()
            list = list.filter { it.location.lowercase().contains(locLower) }
        }

        // 6. Company Query Filter
        if (filter.companyQuery.isNotBlank()) {
            val compLower = filter.companyQuery.lowercase()
            list = list.filter { it.company.lowercase().contains(compLower) }
        }

        // 7. Skills Filter
        if (filter.selectedSkills.isNotEmpty()) {
            list = list.filter { job ->
                filter.selectedSkills.any { skill ->
                    val lowerSkill = skill.lowercase()
                    job.skills.any { s -> s.lowercase().contains(lowerSkill) } ||
                            job.requiredSkills.any { s -> s.lowercase().contains(lowerSkill) } ||
                            job.title.lowercase().contains(lowerSkill)
                }
            }
        }

        // 8. Search Input Query
        if (currentSearchQuery.isNotBlank()) {
            val lower = currentSearchQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(lower) ||
                        it.company.lowercase().contains(lower) ||
                        it.location.lowercase().contains(lower) ||
                        it.skills.any { skill -> skill.lowercase().contains(lower) } ||
                        it.requiredSkills.any { skill -> skill.lowercase().contains(lower) }
            }
        }

        // 9. Sorting (Always preserve newest-first by canonical timestamp when requested)
        list = if (currentSortIsLatest) {
            list.sortedByDescending { it.createdAt }
        } else {
            list.sortedBy { if (it.createdAt > 0) it.createdAt else Long.MAX_VALUE }
        }

        _filteredJobs.value = list
    }
}
