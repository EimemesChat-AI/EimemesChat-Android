package com.eimemes.chat.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.eimemes.chat.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tv = findViewById(R.id.tvSplash);

        // Gradient "breathe" animation — exact match to web's loader-wordmark
        tv.post(() -> {
            float w = tv.getPaint().measureText(tv.getText().toString());
            LinearGradient g = new LinearGradient(0, 0, w, 0,
                new int[]{0xFF5e9cff, 0xFFc96eff}, null, Shader.TileMode.CLAMP);
            tv.getPaint().setShader(g);
            tv.invalidate();

            // Pulse: 0%,100% opacity:1  50% opacity:0.35 — matches CSS breathe keyframe
            ObjectAnimator pulse = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.35f);
            pulse.setDuration(800);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            pulse.start();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                pulse.cancel();
                Intent intent = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }, 1800);
        });
    }
}
