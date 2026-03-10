package com.eimemes.chat.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.eimemes.chat.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE = 100;

    private FirebaseAuth     mAuth;
    private GoogleSignInClient googleClient;
    private EditText         emailInput, passwordInput;
    private Button           btnLogin, btnSignup, btnGoogle;
    private TextView         tvToggle;
    private ProgressBar      progressBar;
    private boolean          isLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth         = FirebaseAuth.getInstance();
        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        btnLogin      = findViewById(R.id.btnLogin);
        btnSignup     = findViewById(R.id.btnSignup);
        btnGoogle     = findViewById(R.id.btnGoogle);
        tvToggle      = findViewById(R.id.tvToggle);
        progressBar   = findViewById(R.id.progressBar);

        applyGradient();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("230417181657-7v30t8ogq03broga9p676p3f9lltng1a.apps.googleusercontent.com")
            .requestEmail()
            .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v  -> handleAuth(true));
        btnSignup.setOnClickListener(v -> handleAuth(false));
        btnGoogle.setOnClickListener(v -> { setLoading(true); startActivityForResult(googleClient.getSignInIntent(), RC_GOOGLE); });
        tvToggle.setOnClickListener(v  -> toggleMode());
    }

    private void toggleMode() {
        isLogin = !isLogin;
        btnLogin.setVisibility(isLogin  ? View.VISIBLE : View.GONE);
        btnSignup.setVisibility(isLogin ? View.GONE    : View.VISIBLE);
        tvToggle.setText(isLogin
            ? "Don't have an account? Sign up"
            : "Already have an account? Sign in");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RC_GOOGLE) {
            try {
                GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                mAuth.signInWithCredential(GoogleAuthProvider.getCredential(acct.getIdToken(), null))
                    .addOnSuccessListener(r -> goMain())
                    .addOnFailureListener(e -> { setLoading(false); toast(e.getMessage()); });
            } catch (ApiException e) {
                setLoading(false); toast("Google sign-in failed: " + e.getMessage());
            }
        }
    }

    private void applyGradient() {
        TextView tv = findViewById(R.id.tvTitle);
        tv.post(() -> {
            float w = tv.getPaint().measureText(tv.getText().toString());
            tv.getPaint().setShader(new LinearGradient(0, 0, w, 0,
                new int[]{0xFF5e9cff, 0xFFc96eff}, null, Shader.TileMode.CLAMP));
            tv.invalidate();
        });
    }

    private void handleAuth(boolean login) {
        String email = emailInput.getText().toString().trim();
        String pass  = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) { toast("Enter email and password"); return; }
        if (pass.length() < 6) { toast("Password must be at least 6 characters"); return; }
        setLoading(true);
        (login
            ? mAuth.signInWithEmailAndPassword(email, pass)
            : mAuth.createUserWithEmailAndPassword(email, pass))
            .addOnSuccessListener(r -> goMain())
            .addOnFailureListener(e -> { setLoading(false); toast(e.getMessage()); });
    }

    private void goMain()         { startActivity(new Intent(this, MainActivity.class)); finish(); }
    private void setLoading(boolean on) { progressBar.setVisibility(on ? View.VISIBLE : View.GONE); btnLogin.setEnabled(!on); btnSignup.setEnabled(!on); btnGoogle.setEnabled(!on); }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
}
