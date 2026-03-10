package com.eimemes.chat.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.App;
import com.eimemes.chat.AppDialog;
import com.eimemes.chat.R;
import com.eimemes.chat.adapters.ChatAdapter;
import com.eimemes.chat.adapters.ConversationAdapter;
import com.eimemes.chat.models.Conversation;
import com.eimemes.chat.models.Message;
import com.eimemes.chat.network.StreamingClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private DrawerLayout         drawerLayout;
    private RecyclerView         rvChat, rvConversations;
    private EditText             etInput;
    private ImageButton          btnMenu, btnTopbarNew, btnSend, btnStop;
    private TextView             tvTopbarTitle;
    private LinearLayout         chatView, settingsView, welcomeLayout, chipsLayout;
    private TextView             tvNoConversations;
    private View                 toggleDarkMode, toggleBg, toggleKnob;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Message>      messages      = new ArrayList<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private ChatAdapter         chatAdapter;
    private ConversationAdapter convAdapter;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth            mAuth;
    private FirebaseFirestore       db;
    private FirebaseUser            user;
    private ListenerRegistration    msgListener, convListener;

    // ── State ─────────────────────────────────────────────────────────────────
    private String          currentConvId = null;
    private boolean         isSending     = false;
    private StreamingClient stream;
    private Message         streamMsg     = null;
    private int             streamPos     = -1;
    private boolean         isDark        = true;

    private static final int    MSG_LIMIT  = 150;
    private static final String P_COUNT    = "ec_msg_count";
    private static final String P_DATE     = "ec_msg_date";
    private static final String P_CHIPS    = "ec_chips_used";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth  = FirebaseAuth.getInstance();
        db     = FirebaseFirestore.getInstance();
        user   = mAuth.getCurrentUser();
        stream = new StreamingClient();

        if (user == null) { goLogin(); return; }

        bindViews();
        applyGradientTitle();
        applyGradientWelcome();
        setupChat();
        setupSidebar();
        setupSettings();
        setupInput();
        setupSuggestionChips();
        subscribeConversations();
    }

    // ── View binding ──────────────────────────────────────────────────────────
    private void bindViews() {
        drawerLayout       = findViewById(R.id.drawerLayout);
        rvChat             = findViewById(R.id.rvChat);
        rvConversations    = findViewById(R.id.rvConversations);
        etInput            = findViewById(R.id.etInput);
        btnMenu            = findViewById(R.id.btnMenu);
        btnTopbarNew       = findViewById(R.id.btnTopbarNew);
        btnSend            = findViewById(R.id.btnSend);
        btnStop            = findViewById(R.id.btnStop);
        tvTopbarTitle      = findViewById(R.id.tvTopbarTitle);
        chatView           = findViewById(R.id.chatView);
        settingsView       = findViewById(R.id.settingsView);
        welcomeLayout      = findViewById(R.id.welcomeLayout);
        chipsLayout        = findViewById(R.id.chipsLayout);
        tvNoConversations  = findViewById(R.id.tvNoConversations);
        toggleDarkMode     = findViewById(R.id.toggleDarkMode);
        toggleBg           = findViewById(R.id.toggleBg);
        toggleKnob         = findViewById(R.id.toggleKnob);
    }

    // ── Gradient title ────────────────────────────────────────────────────────
    private void applyGradientTitle() {
        tvTopbarTitle.post(() -> gradient(tvTopbarTitle));
    }

    private void applyGradientWelcome() {
        TextView tw = findViewById(R.id.tvWelcomeTitle);
        if (tw != null) tw.post(() -> gradient(tw));
    }

    private void gradient(TextView tv) {
        float w = tv.getPaint().measureText(tv.getText().toString());
        if (w <= 0) return;
        tv.getPaint().setShader(new LinearGradient(0, 0, w, 0,
            new int[]{0xFF5e9cff, 0xFFc96eff}, null, Shader.TileMode.CLAMP));
        tv.invalidate();
    }

    // ── Chat setup ────────────────────────────────────────────────────────────
    private void setupChat() {
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);

        chatAdapter = new ChatAdapter(messages, this);
        chatAdapter.setRegenCallback(userMsg -> {
            if (!isSending && userMsg != null && !userMsg.isEmpty()) regenerate(userMsg);
        });
        rvChat.setAdapter(chatAdapter);

        btnMenu.setOnClickListener(v   -> drawerLayout.openDrawer(Gravity.START));
        btnTopbarNew.setOnClickListener(v -> startNewChat());
        btnSend.setOnClickListener(v   -> sendMessage());
        btnStop.setOnClickListener(v   -> stopStream());
    }

    // ── Suggestion chips ──────────────────────────────────────────────────────
    private void setupSuggestionChips() {
        // Show chips only on first ever session — same as web's ec_chips_used
        SharedPreferences prefs    = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE);
        boolean           chipsUsed = prefs.getBoolean(P_CHIPS, false);

        if (chipsUsed) {
            chipsLayout.setVisibility(View.GONE);
            return;
        }

        String[] prompts = { getString(R.string.prompt1), getString(R.string.prompt2),
                             getString(R.string.prompt3), getString(R.string.prompt4) };
        int[] ids = { R.id.chip1, R.id.chip2, R.id.chip3, R.id.chip4 };

        for (int i = 0; i < ids.length; i++) {
            final String prompt = prompts[i];
            TextView chip = findViewById(ids[i]);
            chip.setOnClickListener(v -> {
                etInput.setText(prompt);
                prefs.edit().putBoolean(P_CHIPS, true).apply();
                chipsLayout.setVisibility(View.GONE);
                sendMessage();
            });
        }
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private void setupSidebar() {
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        convAdapter = new ConversationAdapter(conversations, conv -> {
            drawerLayout.closeDrawers();
            loadConversation(conv.getId(), conv.getTitle());
        });
        rvConversations.setAdapter(convAdapter);

        findViewById(R.id.btnCloseSidebar).setOnClickListener(v -> drawerLayout.closeDrawers());
        findViewById(R.id.btnSidebarNewChat).setOnClickListener(v -> { drawerLayout.closeDrawers(); startNewChat(); });
        findViewById(R.id.btnSettings).setOnClickListener(v -> { drawerLayout.closeDrawers(); showSettings(); });
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    private void setupSettings() {
        // Profile
        String email = user.getEmail() != null ? user.getEmail() : "";
        String name  = user.getDisplayName() != null ? user.getDisplayName() : email;
        String initial = (name.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0))));
        ((TextView) findViewById(R.id.tvAvatar)).setText(initial);
        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvProfileEmail)).setText(email);

        // Dark mode toggle — read current preference
        SharedPreferences prefs = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE);
        String theme = prefs.getString(App.KEY_THEME, "system");
        isDark = theme.equals("dark") || (theme.equals("system") && isDarkSystem());
        updateToggleUI(isDark);

        toggleDarkMode.setOnClickListener(v -> {
            isDark = !isDark;
            updateToggleUI(isDark);
            String newTheme = isDark ? "dark" : "light";
            prefs.edit().putString(App.KEY_THEME, newTheme).apply();
            App.applyTheme(newTheme);
            recreate(); // applies immediately
        });

        // Back
        findViewById(R.id.btnSettingsBack).setOnClickListener(v -> showChat());

        // Sign out
        findViewById(R.id.rowSignOut).setOnClickListener(v ->
            AppDialog.showSignOut(this, () -> {
                mAuth.signOut();
                goLogin();
            })
        );

        // Clear chats
        findViewById(R.id.rowClearChats).setOnClickListener(v ->
            AppDialog.showConfirm(
                this,
                "Clear all chats",
                "Permanently delete all conversations?\nThis cannot be undone.",
                "Delete all",
                true,
                () -> clearAllChats()
            )
        );

        // Links
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> openUrl("https://app-eimemeschat.vercel.app/privacy.html"));
        findViewById(R.id.rowHelp).setOnClickListener(v    -> openUrl("https://app-eimemeschat.vercel.app/support.html"));
        findViewById(R.id.rowAbout).setOnClickListener(v   -> openUrl("https://app-eimemeschat.vercel.app/about.html"));
    }

    private boolean isDarkSystem() {
        int night = getResources().getConfiguration().uiMode &
            android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateToggleUI(boolean on) {
        toggleBg.setBackgroundResource(on ? R.drawable.toggle_bg_on : R.drawable.toggle_bg);
        ObjectAnimator.ofFloat(toggleKnob, "translationX", on ? 20f : 0f)
            .setDuration(200).start();
    }

    // ── Input handling ────────────────────────────────────────────────────────
    private void setupInput() {
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.3f);

        etInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                boolean has = s.toString().trim().length() > 0 && !isSending;
                btnSend.setEnabled(has);
                btnSend.setAlpha(has ? 1f : 0.3f);
            }
        });
    }

    // ── View switching ────────────────────────────────────────────────────────
    private void showSettings() { chatView.setVisibility(View.GONE); settingsView.setVisibility(View.VISIBLE); }
    private void showChat()     { settingsView.setVisibility(View.GONE); chatView.setVisibility(View.VISIBLE); }
    private void showWelcome()  { welcomeLayout.setVisibility(View.VISIBLE); rvChat.setVisibility(View.GONE); }
    private void showMessages() { welcomeLayout.setVisibility(View.GONE); rvChat.setVisibility(View.VISIBLE); }

    // ── Firestore helpers ─────────────────────────────────────────────────────
    private CollectionReference convCol() {
        return db.collection("users").document(user.getUid()).collection("conversations");
    }

    private void subscribeConversations() {
        convListener = convCol()
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                conversations.clear();
                for (DocumentSnapshot d : snap.getDocuments())
                    conversations.add(new Conversation(d.getId(), d.getString("title")));
                convAdapter.notifyDataSetChanged();
                tvNoConversations.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    private void loadConversation(String id, String title) {
        currentConvId = id;
        messages.clear();
        chatAdapter.notifyDataSetChanged();
        convAdapter.setActiveId(id);
        tvTopbarTitle.setText(title != null ? title : "EimemesChat AI");
        applyGradientTitle();
        showMessages();
        showChat();

        if (msgListener != null) msgListener.remove();
        msgListener = convCol().document(id).addSnapshotListener((snap, e) -> {
            if (e != null || snap == null || !snap.exists()) return;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> raw = (List<Map<String, Object>>) snap.get("messages");
            if (raw == null) return;
            messages.clear();
            for (Map<String, Object> m : raw) {
                Message msg = new Message(
                    (String) m.get("role"), (String) m.get("content"), (String) m.get("time"));
                msg.setModel((String) m.get("model"));
                Boolean disc = (Boolean) m.get("disclaimer");
                if (disc != null) msg.setDisclaimer(disc);
                messages.add(msg);
            }
            refreshLastAiPos();
            chatAdapter.notifyDataSetChanged();
            scrollBottom();
        });
    }

    private void startNewChat() {
        currentConvId = null;
        messages.clear();
        chatAdapter.notifyDataSetChanged();
        tvTopbarTitle.setText("✦ EimemesChat");
        applyGradientTitle();
        convAdapter.setActiveId(null);
        if (msgListener != null) { msgListener.remove(); msgListener = null; }
        showWelcome();
        showChat();
    }

    // ── Messaging ─────────────────────────────────────────────────────────────
    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || isSending) return;

        if (!checkLimit()) {
            Toast.makeText(this, "Daily limit of " + MSG_LIMIT + " messages reached. Try again tomorrow!", Toast.LENGTH_LONG).show();
            return;
        }

        // Mark chips as used on first message
        getSharedPreferences(App.PREF_NAME, MODE_PRIVATE).edit().putBoolean(P_CHIPS, true).apply();
        chipsLayout.setVisibility(View.GONE);

        setSending(true);
        etInput.setText("");

        messages.add(new Message(Message.ROLE_USER, text, time()));
        showMessages();
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollBottom();

        if (currentConvId == null) {
            createConversation(text, () -> doSend(text));
        } else {
            saveUserMsg(text, () -> doSend(text));
        }
    }

    private void regenerate(String userMsg) {
        if (isSending) return;
        // Remove last AI message from local list and resend
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (Message.ROLE_ASSISTANT.equals(messages.get(i).getRole())) {
                messages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
        setSending(true);
        doSend(userMsg);
    }

    private void createConversation(String firstMsg, Runnable next) {
        String title = firstMsg.length() > 50 ? firstMsg.substring(0, 50) + "…" : firstMsg;
        Map<String, Object> data = new HashMap<>();
        data.put("title",     title);
        data.put("messages",  new ArrayList<>());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        convCol().add(data)
            .addOnSuccessListener(ref -> {
                currentConvId = ref.getId();
                convAdapter.setActiveId(currentConvId);
                tvTopbarTitle.setText(title);
                applyGradientTitle();
                saveUserMsg(firstMsg, next);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to create chat.", Toast.LENGTH_SHORT).show();
                resetSend();
            });
    }

    private void saveUserMsg(String text, Runnable next) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user"); msg.put("content", text); msg.put("time", time());

        convCol().document(currentConvId)
            .update("messages", FieldValue.arrayUnion(msg), "updatedAt", FieldValue.serverTimestamp())
            .addOnSuccessListener(v -> next.run())
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send.", Toast.LENGTH_SHORT).show();
                resetSend();
            });
    }

    private void doSend(String text) {
        // Add typing indicator
        streamMsg = new Message(Message.ROLE_TYPING, "", time());
        messages.add(streamMsg);
        streamPos = messages.size() - 1;
        chatAdapter.notifyItemInserted(streamPos);
        scrollBottom();

        JSONArray history = buildHistory();

        user.getIdToken(false)
            .addOnFailureListener(e -> {
                if (streamMsg != null) {
                    streamMsg.setRole(Message.ROLE_ASSISTANT);
                    streamMsg.setContent("⚠ Auth error. Please sign out and back in.");
                    chatAdapter.notifyItemChanged(streamPos);
                }
                resetSend();
            })
            .addOnSuccessListener(result ->
            stream.send(text, history, result.getToken(), new StreamingClient.Callback() {
                @Override public void onToken(String t) {
                    if (streamMsg == null) return;
                    if (Message.ROLE_TYPING.equals(streamMsg.getRole()))
                        streamMsg.setRole(Message.ROLE_ASSISTANT);
                    streamMsg.setContent(streamMsg.getContent() + t);
                    chatAdapter.notifyItemChanged(streamPos);
                    scrollBottom();
                }
                @Override public void onDone(String full, String model, boolean disc) {
                    if (streamMsg != null) {
                        streamMsg.setRole(Message.ROLE_ASSISTANT);
                        streamMsg.setContent(full);
                        streamMsg.setModel(model);
                        streamMsg.setDisclaimer(disc);
                        chatAdapter.notifyItemChanged(streamPos);
                    }
                    saveAiMsg(full, model, disc);
                    refreshLastAiPos();
                    chatAdapter.notifyDataSetChanged();
                    resetSend();
                }
                @Override public void onError(String err) {
                    if (streamMsg != null) {
                        streamMsg.setRole(Message.ROLE_ASSISTANT);
                        streamMsg.setContent("⚠ " + err);
                        chatAdapter.notifyItemChanged(streamPos);
                    }
                    resetSend();
                }
            })
        );
    }

    private void saveAiMsg(String text, String model, boolean disc) {
        if (currentConvId == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant"); msg.put("content", text);
        msg.put("time", time()); msg.put("disclaimer", disc);
        if (model != null && !model.isEmpty()) msg.put("model", model);
        convCol().document(currentConvId)
            .update("messages", FieldValue.arrayUnion(msg), "updatedAt", FieldValue.serverTimestamp());
    }

    private void stopStream() { stream.cancel(); resetSend(); }

    private void resetSend() {
        isSending = false;
        streamMsg = null;
        streamPos = -1;
        setSending(false);
    }

    private void setSending(boolean on) {
        isSending = on;
        btnStop.setVisibility(on ? View.VISIBLE : View.GONE);
        btnSend.setVisibility(on ? View.GONE    : View.VISIBLE);
        if (!on) {
            String t = etInput.getText().toString().trim();
            btnSend.setEnabled(t.length() > 0);
            btnSend.setAlpha(t.length() > 0 ? 1f : 0.3f);
        }
    }

    private void refreshLastAiPos() {
        int last = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (Message.ROLE_ASSISTANT.equals(messages.get(i).getRole())) { last = i; break; }
        }
        chatAdapter.setLastAiPos(last);
    }

    // ── Clear chats ───────────────────────────────────────────────────────────
    private void clearAllChats() {
        convCol().get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) doc.getReference().delete();
            startNewChat();
            Toast.makeText(this, "All chats cleared", Toast.LENGTH_SHORT).show();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JSONArray buildHistory() {
        JSONArray arr   = new JSONArray();
        int       start = Math.max(0, messages.size() - 12);
        for (int i = start; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (Message.ROLE_TYPING.equals(m.getRole())) continue;
            try {
                JSONObject o = new JSONObject();
                o.put("role", m.getRole());
                o.put("content", m.getContent());
                arr.put(o);
            } catch (Exception ignored) {}
        }
        return arr;
    }

    private boolean checkLimit() {
        SharedPreferences p    = getSharedPreferences(App.PREF_NAME, MODE_PRIVATE);
        String            today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        int               count = today.equals(p.getString(P_DATE, "")) ? p.getInt(P_COUNT, 0) : 0;
        if (count >= MSG_LIMIT) return false;
        p.edit().putInt(P_COUNT, count + 1).putString(P_DATE, today).apply();
        return true;
    }

    private String time()      { return new SimpleDateFormat("h:mm a", Locale.US).format(new Date()); }
    private void scrollBottom(){ if (!messages.isEmpty()) rvChat.smoothScrollToPosition(messages.size() - 1); }
    private void goLogin()     { startActivity(new Intent(this, LoginActivity.class)); finish(); }
    private void openUrl(String u) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }

    // ── Back navigation ───────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (settingsView.getVisibility() == View.VISIBLE) { showChat(); return; }
        if (drawerLayout.isDrawerOpen(Gravity.START))     { drawerLayout.closeDrawers(); return; }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener  != null) msgListener.remove();
        if (convListener != null) convListener.remove();
        stream.cancel();
    }
}
