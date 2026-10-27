package co.median.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class AdminDashboardFragment : Fragment() {

    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure admin profile is synced in Firestore
        FirebaseProfileRepository.ensureAdminProfileSync()

        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        tabLayout = view.findViewById(R.id.adminTabLayout)

        tabLayout.addTab(tabLayout.newTab().setText("Published Jobs"))
        tabLayout.addTab(tabLayout.newTab().setText("Pending Approval"))
        tabLayout.addTab(tabLayout.newTab().setText("Job Sources"))

        // Load default tab (Published Jobs)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.adminContainer, PublishedJobsFragment())
                .commit()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val fragment = when (tab?.position) {
                    0 -> PublishedJobsFragment()
                    1 -> PendingJobsFragment()
                    2 -> JobSourcesFragment()
                    else -> PublishedJobsFragment()
                }
                childFragmentManager.beginTransaction()
                    .replace(R.id.adminContainer, fragment)
                    .commit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }
}
