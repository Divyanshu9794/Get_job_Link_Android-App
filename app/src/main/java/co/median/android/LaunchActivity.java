package co.median.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(LaunchActivity.this, MainActivity.class);

            if (getIntent() != null && getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }

            startActivity(intent);
            finish();
        }, 400);
    }
}
