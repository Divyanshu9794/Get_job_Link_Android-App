package co.median.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        Intent intent;

        if (auth.getCurrentUser() != null) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, AuthActivity.class);
        }

        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }

        startActivity(intent);
        finish();
    }
}
