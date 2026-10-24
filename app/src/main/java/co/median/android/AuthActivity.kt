package co.median.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AuthActivity"
    }

    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { taskAuth ->
                        if (taskAuth.isSuccessful) {
                            FirebaseProfileRepository.clearLocalProfile(this)
                            FirebaseProfileRepository.ensureUserProfileExists(this) {
                                navigateToMain()
                            }
                        } else {
                            val msg = taskAuth.exception?.localizedMessage ?: "Firebase credential authentication failed"
                            Log.e(TAG, "Firebase Auth credential sign-in failed: $msg", taskAuth.exception)
                            Toast.makeText(this, "Sign in failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Log.e(TAG, "Google Sign-In completed but idToken is null. Check google-services.json and OAuth client ID.")
                Toast.makeText(this, "Sign-In failed: Web client ID / OAuth token unavailable. Verify google-services.json.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
            val errorMsg = when (e.statusCode) {
                10 -> "Developer error (Code 10): Package name / SHA-1 fingerprint mismatch or invalid API key in google-services.json."
                12500 -> "Sign-in failed (Code 12500): Google Play Services error."
                12501 -> "Sign-in canceled."
                7 -> "Network error. Please check your internet connection."
                else -> "Sign-In failed (Code ${e.statusCode}): ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_auth)

        val googleSignInBtn = findViewById<Button>(R.id.googleSignInBtn)
        val guestBtn = findViewById<Button>(R.id.guestBtn)

        googleSignInBtn.setOnClickListener {
            val webClientId = try {
                getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load default_web_client_id resource", e)
                null
            }

            val appPackage = BuildConfig.APPLICATION_ID
            val firebaseProjectId = try { FirebaseApp.getInstance().options.projectId } catch (_: Exception) { "unknown" }
            Log.i(TAG, "Google Sign-In Diagnostic - ApplicationId: $appPackage, FirebaseProjectId: $firebaseProjectId, ClientID: $webClientId")

            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()

            if (!webClientId.isNullOrBlank() && !webClientId.contains("placeholder")) {
                gsoBuilder.requestIdToken(webClientId)
            } else {
                Log.w(TAG, "webClientId is null/blank/placeholder! requestIdToken was not set.")
            }

            val googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        guestBtn.setOnClickListener {
            auth.signInAnonymously().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    FirebaseProfileRepository.clearLocalProfile(this)
                    FirebaseProfileRepository.ensureUserProfileExists(this) {
                        navigateToMain()
                    }
                } else {
                    val msg = task.exception?.localizedMessage ?: "Unknown error"
                    Log.e(TAG, "Guest Sign-In failed: $msg", task.exception)
                    Toast.makeText(this, "Guest Sign-In failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveBasicProfile(email: String?, displayName: String?) {
        val user = auth.currentUser ?: return
        if (!email.isNullOrBlank() || !displayName.isNullOrBlank()) {
            val profile = UserProfile(
                id = user.uid,
                name = displayName ?: "Job Seeker",
                email = email ?: ""
            )
            FirebaseProfileRepository.saveProfileLocally(this, profile)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (getIntent() != null && getIntent().extras != null) {
                putExtras(getIntent().extras!!)
            }
        }
        startActivity(intent)
        finish()
    }
}
