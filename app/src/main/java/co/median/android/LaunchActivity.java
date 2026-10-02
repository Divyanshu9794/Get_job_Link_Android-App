package co.median.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = getCurrentFirebaseUser();

            Intent intent;
            if (currentUser != null) {
                intent = new Intent(LaunchActivity.this, MainActivity.class);
            } else {
                intent = new Intent(LaunchActivity.this, AuthActivity.class);
            }

            if (getIntent() != null && getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }

            startActivity(intent);
            finish();
        }, 400);
    }

    private FirebaseUser getCurrentFirebaseUser() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }
}
