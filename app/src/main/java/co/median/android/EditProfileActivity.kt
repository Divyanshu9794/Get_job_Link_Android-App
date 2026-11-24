package co.median.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sheet = ProfileBottomSheetDialog(
            onProfileSaved = {
                finish()
            },
            onDismissAction = {
                finish()
            }
        )
        sheet.show(supportFragmentManager, "edit_profile_bottom_sheet")
    }
}
