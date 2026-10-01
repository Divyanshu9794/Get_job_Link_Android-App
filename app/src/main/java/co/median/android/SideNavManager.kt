package co.median.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import co.median.android.widget.GoNativeDrawerLayout
import co.median.median_core.AppConfig
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SideNavManager(val main: MainActivity) {

    private val drawerLayout: GoNativeDrawerLayout = main.findViewById(R.id.drawer_layout)
    private var drawerToggle: ActionBarDrawerToggle? = null
    val navMenu: NavigationView = main.findViewById(R.id.nav_menu)
    private var isRoot = true

    fun setupNavigationMenu(isRoot: Boolean) {
        this.isRoot = isRoot
        val appConfig = AppConfig.getInstance(main)

        if (isRoot) {
            drawerToggle = object :
                ActionBarDrawerToggle(
                    main,
                    drawerLayout,
                    R.string.drawer_open,
                    R.string.drawer_close
                ) {
                override fun onDrawerClosed(view: View) {
                    drawerLayout.setDisableTouch(appConfig.swipeGestures && main.canGoBack())
                }

                override fun onDrawerOpened(drawerView: View) {
                    drawerLayout.setDisableTouch(false)
                    setupHeader()
                    updateNotificationBadge()
                }
            }.apply {
                isDrawerIndicatorEnabled = true
                drawerArrowDrawable.color = ContextCompat.getColor(main, R.color.titleTextColor)
                syncState()
            }
            drawerToggle?.let { drawerLayout.addDrawerListener(it) }
        }

        setupHeader()
        updateNotificationBadge()
    }

    fun setupHeader() {
        if (navMenu.headerCount == 0) return
        val headerView = navMenu.getHeaderView(0) ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser

        val txtName = headerView.findViewById<TextView>(R.id.txt_account_name)
        val txtEmail = headerView.findViewById<TextView>(R.id.txt_account_email)
        val imgAvatar = headerView.findViewById<ImageView>(R.id.img_account_avatar)
        val layoutAccount = headerView.findViewById<View>(R.id.layout_account_header)

        if (currentUser != null) {
            val localProfile = FirebaseProfileRepository.loadLocalProfile(main)
            val displayName = currentUser.displayName.orEmpty()
                .ifEmpty { localProfile.name }
                .ifEmpty { "Job Seeker" }
            val email = currentUser.email.orEmpty()
                .ifEmpty { localProfile.email }
                .ifEmpty { "user@email.com" }

            txtName?.text = displayName
            txtEmail?.text = email

            val initialsDrawable = createInitialsAvatar(main, displayName)
            imgAvatar?.setImageDrawable(initialsDrawable)

            layoutAccount?.setOnClickListener {
                closeDrawer()
                main.openProfileFromDrawer()
            }
        } else {
            txtName?.setText(R.string.guest_user)
            txtEmail?.setText(R.string.sign_in_hint)
            imgAvatar?.setImageResource(R.drawable.ic_profile)

            layoutAccount?.setOnClickListener {
                closeDrawer()
                val intent = Intent(main, AuthActivity::class.java)
                main.startActivity(intent)
            }
        }
    }

    fun updateNotificationBadge() {
        val menuItem = navMenu.menu.findItem(R.id.drawer_notifications) ?: return
        val actionView = menuItem.actionView ?: return
        val txtBadge = actionView.findViewById<TextView>(R.id.txt_notification_badge) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = JobRepository.getAllJobs()
            val unreadMatches = NotificationMatchingHelper.getUnreadMatchingJobs(main, jobs)
            val count = unreadMatches.size
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    txtBadge.text = count.toString()
                    txtBadge.visibility = View.VISIBLE
                } else {
                    txtBadge.visibility = View.GONE
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

    fun showNavigationMenu(show: Boolean) {
        if (show) {
            drawerLayout.apply {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                drawerToggle?.let { addDrawerListener(it) }
            }
        } else {
            drawerLayout.apply {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                drawerToggle?.let { removeDrawerListener(it) }
            }
        }
    }

    fun updateMenu(status: String = "default") {
        setupHeader()
        updateNotificationBadge()
    }

    fun autoSelectItem(url: String) {
        // no-op for native drawer
    }

    fun checkUrl(url: String) {
        val appConfig = AppConfig.getInstance(main)
        setDrawerEnabled(appConfig.shouldShowSidebarForUrl(url))
        if (this.drawerLayout.getDrawerLockMode(GravityCompat.START) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
            drawerLayout.setDisableTouch(appConfig.swipeGestures && main.canGoBack())
        }
    }

    fun setSideBarNavigationEnabled(enabled: Boolean) {
        setDrawerEnabled(enabled)
    }

    private fun setDrawerEnabled(enabled: Boolean) {
        if (!isRoot) return

        val appConfig = AppConfig.getInstance(main)
        drawerLayout.setDrawerLockMode(if (enabled) GoNativeDrawerLayout.LOCK_MODE_UNLOCKED else GoNativeDrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val toolbar: Toolbar? = main.findViewById(R.id.toolbar)
        if (toolbar != null && enabled) {
            toolbar.visibility = View.VISIBLE
        }

        main.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(enabled)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    fun isDrawerOpen(): Boolean = drawerLayout.isDrawerOpen(navMenu)
    fun closeDrawer() = drawerLayout.closeDrawers()

    fun isToggleMenuSelected(menuItem: MenuItem): Boolean {
        if (drawerToggle?.onOptionsItemSelected(menuItem) == true) {
            return true
        }
        if (menuItem.itemId == android.R.id.home && isRoot) {
            if (isDrawerOpen()) {
                closeDrawer()
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            return true
        }
        return false
    }

    fun toggleSyncState() = drawerToggle?.syncState()
    fun toggleConfigurationChanged(newConfig: Configuration) =
        drawerToggle?.onConfigurationChanged(newConfig)

    fun onThemeChanged() {
        setupHeader()
        updateNotificationBadge()
    }
}
