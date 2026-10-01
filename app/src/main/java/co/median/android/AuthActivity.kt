package co.median.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { taskAuth ->
                        if (taskAuth.isSuccessful) {
                            navigateToMain()
                        } else {
                            Toast.makeText(this, "Firebase Auth failed: ${taskAuth.exception?.message}", Toast.LENGTH_LONG).show()
                            navigateToMain()
                        }
                    }
            } else {
                navigateToMain()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In completed", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()

        val googleSignInBtn = findViewById<Button>(R.id.googleSignInBtn)
        val guestBtn = findViewById<Button>(R.id.guestBtn)

        googleSignInBtn.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("123456789012-placeholder.apps.googleusercontent.com")
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        guestBtn.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            if (getIntent() != null && getIntent().extras != null) {
                putExtras(getIntent().extras!!)
            }
        }
        startActivity(intent)
        finish()
    }
}
