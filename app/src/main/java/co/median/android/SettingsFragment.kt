package co.median.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class SettingsFragment : Fragment() {

    private val PREFS_NAME = "get_job_link_settings"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val backBtn = view.findViewById<View>(R.id.settingsBackBtn)
        backBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.native_fragment_container, HomeFragment())
                .commit()
        }

        val themeGroup = view.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val radioSystem = view.findViewById<RadioButton>(R.id.radioSystem)
        val radioLight = view.findViewById<RadioButton>(R.id.radioLight)
        val radioDark = view.findViewById<RadioButton>(R.id.radioDark)

        val textSizeGroup = view.findViewById<RadioGroup>(R.id.textSizeRadioGroup)
        val radioTextSmall = view.findViewById<RadioButton>(R.id.radioTextSmall)
        val radioTextMedium = view.findViewById<RadioButton>(R.id.radioTextMedium)
        val radioTextLarge = view.findViewById<RadioButton>(R.id.radioTextLarge)
        val radioTextExtraLarge = view.findViewById<RadioButton>(R.id.radioTextExtraLarge)

        val switchJobAlerts = view.findViewById<SwitchCompat>(R.id.switchJobAlerts)
        val switchRecommended = view.findViewById<SwitchCompat>(R.id.switchRecommended)
        val switchProfileReminders = view.findViewById<SwitchCompat>(R.id.switchProfileReminders)
        val switchAppUpdates = view.findViewById<SwitchCompat>(R.id.switchAppUpdates)

        val editWorkType = view.findViewById<EditText>(R.id.editWorkType)
        val editLocation = view.findViewById<EditText>(R.id.editLocation)
        val editExperienceLevel = view.findViewById<EditText>(R.id.editExperienceLevel)
        val editSkills = view.findViewById<EditText>(R.id.editSkills)
        val btnSavePreferences = view.findViewById<Button>(R.id.btnSavePreferences)

        val logoutBtn = view.findViewById<Button>(R.id.settingsLogoutBtn)

        // Load Theme
        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> radioDark.isChecked = true
            else -> radioSystem.isChecked = true
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.edit().putInt("theme_mode", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Load Text Size
        val textSize = prefs.getString("text_size", "medium") ?: "medium"
        when (textSize) {
            "small" -> radioTextSmall.isChecked = true
            "large" -> radioTextLarge.isChecked = true
            "extra_large" -> radioTextExtraLarge.isChecked = true
            else -> radioTextMedium.isChecked = true
        }

        textSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedSize = when (checkedId) {
                R.id.radioTextSmall -> "small"
                R.id.radioTextLarge -> "large"
                R.id.radioTextExtraLarge -> "extra_large"
                else -> "medium"
            }
            prefs.edit().putString("text_size", selectedSize).apply()
            Toast.makeText(requireContext(), "Text size set to $selectedSize", Toast.LENGTH_SHORT).show()
        }

        // Load Notification Preferences
        switchJobAlerts.isChecked = prefs.getBoolean("notif_job_alerts", true)
        switchRecommended.isChecked = prefs.getBoolean("notif_recommended", true)
        switchProfileReminders.isChecked = prefs.getBoolean("notif_profile_reminders", true)
        switchAppUpdates.isChecked = prefs.getBoolean("notif_app_updates", true)

        switchJobAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_job_alerts", isChecked).apply()
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("new_jobs")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("new_jobs")
            }
        }
        switchRecommended.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_recommended", isChecked).apply()
        }
        switchProfileReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_profile_reminders", isChecked).apply()
        }
        switchAppUpdates.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_app_updates", isChecked).apply()
        }

        // Load Job Preferences
        editWorkType.setText(prefs.getString("pref_work_type", "Full Time, Remote"))
        editLocation.setText(prefs.getString("pref_location", "Remote / Any"))
        editExperienceLevel.setText(prefs.getString("pref_experience", "1-3 Years"))
        editSkills.setText(prefs.getString("pref_skills", "Android, Kotlin, Java, React"))

        btnSavePreferences.setOnClickListener {
            prefs.edit()
                .putString("pref_work_type", editWorkType.text.toString().trim())
                .putString("pref_location", editLocation.text.toString().trim())
                .putString("pref_experience", editExperienceLevel.text.toString().trim())
                .putString("pref_skills", editSkills.text.toString().trim())
                .apply()
            Toast.makeText(requireContext(), "Job preferences saved!", Toast.LENGTH_SHORT).show()
        }

    logoutBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("Are you sure you want to log out of GetJobLink?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out") { _, _ ->
                    FirebaseProfileRepository.clearLocalProfile(requireContext())
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                .show()
        }

        return view
    }
}
