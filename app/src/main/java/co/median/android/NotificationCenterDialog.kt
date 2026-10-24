package co.median.android

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationCenterDialog(
    private val onUrlSelected: ((String) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_notification_center, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_notifications)
        emptyStateView = view.findViewById(R.id.layout_empty_state)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(mutableListOf()) { item ->
            if (item.targetUrl.isNotEmpty()) {
                onUrlSelected?.invoke(item.targetUrl)
                dismiss()
            }
        }
        recyclerView.adapter = adapter

        val btnClearAll = view.findViewById<TextView>(R.id.btn_clear_all)
        btnClearAll.setOnClickListener {
            NotificationHelper.clearNotificationHistory(requireContext())
            loadNotifications()
            Toast.makeText(requireContext(), "Notifications cleared", Toast.LENGTH_SHORT).show()
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        val context = context ?: return
        val items = NotificationHelper.getNotificationHistory(context)
        if (items.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
            adapter.updateItems(items)
        }
    }

    private class NotificationAdapter(
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
