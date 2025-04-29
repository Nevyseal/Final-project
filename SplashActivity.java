package app.ij.mlwithtensorflowlite;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force sign‐out on every cold start
        FirebaseAuth.getInstance().signOut();

        // Now immediately go to Login screen
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
