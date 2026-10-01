package co.median.android

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class NotificationsFragment : Fragment() {

    private lateinit var emptyStateContainer: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyDesc: TextView
    private lateinit var clearBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jobs, container, false)
        view.findViewById<View>(R.id.jobsSearchInput)?.visibility = View.GONE
        view.findViewById<View>(R.id.filterBtn)?.visibility = View.GONE
        view.findViewById<View>(R.id.sortBtn)?.visibility = View.GONE
        view.findViewById<View>(R.id.activeFiltersCount)?.visibility = View.GONE

        progressBar = view.findViewById(R.id.jobsProgressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        emptyTitle = view.findViewById(R.id.jobsEmptyTitle)
        emptyDesc = view.findViewById(R.id.jobsEmptyView)
        clearBtn = view.findViewById(R.id.clearFiltersEmptyBtn)

        clearBtn.visibility = View.GONE
        emptyTitle.text = "No Notifications"
        emptyDesc.text = "You're all caught up! No new job notifications."

        swipeRefreshLayout = view.findViewById(R.id.jobsSwipeRefresh)

        recyclerView = view.findViewById(R.id.jobsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificationHistoryAdapter(mutableListOf()) { item ->
            if (item.jobId.isNotBlank()) {
                val intent = Intent(requireContext(), JobDetailsActivity::class.java).apply {
                    putExtra("job_id", item.jobId)
                    putExtra("job_title", item.title)
                    putExtra("job_description", item.message)
                    putExtra("job_url", item.targetUrl)
                }
                startActivity(intent)
            } else if (item.targetUrl.isNotBlank()) {
                val intent = Intent(requireContext(), InAppWebViewActivity::class.java).apply {
                    putExtra("url", item.targetUrl)
                    putExtra("title", item.title)
                }
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }

        loadNotifications()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        if (!swipeRefreshLayout.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }

        val context = context ?: return
        val items = NotificationHelper.getNotificationHistory(context)
        NotificationHelper.markAllAsRead(context)

        progressBar.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        if (items.isNotEmpty()) {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.updateItems(items)
        } else {
            recyclerView.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        }
    }

    private class NotificationHistoryAdapter(
        private val items: MutableList<NotificationItem>,
        private val onItemClick: (NotificationItem) -> Unit
    ) : RecyclerView.Adapter<NotificationViewHolder>() {

        fun updateItems(newItems: List<NotificationItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, onItemClick)
        }

        override fun getItemCount(): Int = items.size
    }

    private class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txt_title)
        private val txtMessage: TextView = itemView.findViewById(R.id.txt_message)
        private val txtTime: TextView = itemView.findViewById(R.id.txt_time)

        fun bind(item: NotificationItem, onClick: (NotificationItem) -> Unit) {
            txtTitle.text = item.title
            txtMessage.text = item.message

            val timeAgo = if (item.timestamp > 0) {
                DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            } else ""
            txtTime.text = timeAgo

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
