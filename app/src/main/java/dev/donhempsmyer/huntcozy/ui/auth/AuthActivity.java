package dev.donhempsmyer.huntcozy.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import dev.donhempsmyer.huntcozy.R;
import dev.donhempsmyer.huntcozy.data.seed.FirebaseSeeder;
import dev.donhempsmyer.huntcozy.ui.main.MainActivity;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";

    private EditText inputEmail;
    private EditText inputPassword;
    private Button buttonSignIn;
    private Button buttonSignUp;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();

        // If already signed in, skip auth screen
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            Log.d(TAG, "onCreate: user already signed in -> " + current.getUid());
            goToMain(current);
            return;
        }

        buttonSignIn.setOnClickListener(v -> attemptSignIn());
        buttonSignUp.setOnClickListener(v -> attemptSignUp());
    }

    private void bindViews() {
        inputEmail = findViewById(R.id.edit_auth_email);
        inputPassword = findViewById(R.id.edit_auth_password);
        buttonSignIn = findViewById(R.id.button_auth_sign_in);
        buttonSignUp = findViewById(R.id.button_auth_sign_up);
        progressBar = findViewById(R.id.progress_auth);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------------------
    // Sign In flow
    // ------------------------------------------------------------------------

    private void attemptSignIn() {
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString() : "";

        if (!validateEmailPassword(email, password)) {
            return;
        }

        setLoading(true);
        Log.d(TAG, "attemptSignIn: " + email);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    Log.d(TAG, "signIn: success uid=" + (user != null ? user.getUid() : "null"));
                    onAuthSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "signIn: failed", e);
                    setLoading(false);

                    if (isUserNotFoundError(e)) {
                        showError("No account found for that email. Please create an account.");
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        showError("Incorrect password. Please try again.");
                    } else {
                        showError("Sign-in failed: " + friendlyMessage(e));
                    }
                });
    }

    // ------------------------------------------------------------------------
    // Sign Up flow
    // ------------------------------------------------------------------------

    private void attemptSignUp() {
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString() : "";

        if (!validateEmailPassword(email, password)) {
            return;
        }

        setLoading(true);
        Log.d(TAG, "attemptSignUp: " + email);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    Log.d(TAG, "createUser: success uid=" + (user != null ? user.getUid() : "null"));
                    onAuthSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createUser: failed", e);
                    setLoading(false);
                    showError("Sign-up failed: " + friendlyMessage(e));
                });
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Email required");
            inputEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Password required");
            inputPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            inputPassword.requestFocus();
            return false;
        }
        return true;
    }

    private boolean isUserNotFoundError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            return true;
        }
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            Log.d(TAG, "FirebaseAuthException code=" + code);
            return "ERROR_USER_NOT_FOUND".equals(code);
        }
        return false;
    }

    private void onAuthSuccess(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "onAuthSuccess: user was null");
            setLoading(false);
            showError("Authentication failed: no user");
            return;
        }

        Log.d(TAG, "onAuthSuccess: uid=" + user.getUid());

        // Seed Firestore structure for this user (idempotent)
        FirebaseSeeder.seedStructureForUser(firestore, user.getUid());

        goToMain(user);
    }

    private void goToMain(@NonNull FirebaseUser user) {
        Log.d(TAG, "goToMain: launching MainActivity for uid=" + user.getUid());
        setLoading(false);

        Intent intent = new Intent(this, MainActivity.class);
        // Clear auth screen from back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (buttonSignIn != null) buttonSignIn.setEnabled(!loading);
        if (buttonSignUp != null) buttonSignUp.setEnabled(!loading);
        if (inputEmail != null) inputEmail.setEnabled(!loading);
        if (inputPassword != null) inputPassword.setEnabled(!loading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String friendlyMessage(Exception e) {
        String raw = e != null && e.getMessage() != null ? e.getMessage() : "Unknown error";
        // You can later map specific Firebase error codes to nicer user messages here.
        return raw;
    }
}
