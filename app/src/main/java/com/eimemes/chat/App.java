package com.eimemes.chat;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class App extends Application {

    public static final String PREF_NAME  = "ec_prefs";
    public static final String KEY_THEME  = "theme"; // "dark" | "light" | "system"

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme before any activity starts
        String theme = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_THEME, "system");
        applyTheme(theme);

        // Firebase manual init (keeps API key off disk in CI)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions opts = new FirebaseOptions.Builder()
                    .setApplicationId("1:230417181657:android:577c478074c10436f387c8")
                    .setProjectId("chat-eimeme")
                    .setApiKey("AIzaSyBBAw2643r9q4mKpFU2uUUiCpu7Fzb287w")
                    .setStorageBucket("chat-eimeme.firebasestorage.app")
                    .setGcmSenderId("230417181657")
                    .build();
                FirebaseApp.initializeApp(this, opts);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void applyTheme(String theme) {
        switch (theme) {
            case "dark":   AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);  break;
            case "light":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);   break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }
}
