package com.eimemes.chat.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.R;
import com.eimemes.chat.models.Message;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.syntax.Prism4jThemeDarkula;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0, TYPE_AI = 1, TYPE_TYPING = 2;

    public interface RegenCallback { void onRegen(String originalUserMsg); }

    private final List<Message> msgs;
    private final Markwon       markwon;
    private       RegenCallback regenCallback;
    private       int           lastAiPos = -1; // index of last assistant msg — shows regen btn

    public ChatAdapter(List<Message> msgs, Context ctx) {
        this.msgs = msgs;
        Prism4j prism4j = new Prism4j(new com.eimemes.chat.GrammarLocatorDef());
        this.markwon = Markwon.builder(ctx)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(ctx))
            .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDarkula.create()))
            .build();
    }

    public void setRegenCallback(RegenCallback cb) { this.regenCallback = cb; }
    public void setLastAiPos(int pos)              { this.lastAiPos = pos; }

    @Override
    public int getItemViewType(int i) {
        String r = msgs.get(i).getRole();
        if (Message.ROLE_TYPING.equals(r)) return TYPE_TYPING;
        if (Message.ROLE_USER.equals(r))   return TYPE_USER;
        return TYPE_AI;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        LayoutInflater li = LayoutInflater.from(p.getContext());
        if (t == TYPE_USER)   return new UserVH(li.inflate(R.layout.item_message_user, p, false));
        if (t == TYPE_TYPING) return new TypingVH(li.inflate(R.layout.item_typing, p, false));
        return new AiVH(li.inflate(R.layout.item_message_assistant, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        Message m = msgs.get(pos);
        if (h instanceof UserVH) ((UserVH) h).bind(m);
        if (h instanceof AiVH)   ((AiVH) h).bind(m, pos, markwon, regenCallback, lastAiPos, msgs);
    }

    @Override public int getItemCount() { return msgs.size(); }

    // ── User message ──────────────────────────────────────────────
    static class UserVH extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        UserVH(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tvContent);
            tvTime    = v.findViewById(R.id.tvTime);
        }
        void bind(Message m) {
            tvContent.setText(m.getContent());
            tvTime.setText(m.getTime() != null ? m.getTime() : "");
        }
    }

    // ── AI message ────────────────────────────────────────────────
    static class AiVH extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime, tvModel, btnCopyResponse, tvDisclaimer;
        View     btnRegen;

        AiVH(View v) {
            super(v);
            tvContent      = v.findViewById(R.id.tvContent);
            tvTime         = v.findViewById(R.id.tvTime);
            tvModel        = v.findViewById(R.id.tvModel);
            btnCopyResponse= v.findViewById(R.id.btnCopyResponse);
            btnRegen       = v.findViewById(R.id.btnRegen);
            tvDisclaimer   = v.findViewById(R.id.tvDisclaimer);
        }

        void bind(Message m, int pos, Markwon markwon, RegenCallback regenCb,
                  int lastAiPos, List<Message> msgs) {

            // Render markdown (same as marked.js on web)
            markwon.setMarkdown(tvContent, m.getContent());

            tvTime.setText(m.getTime() != null ? m.getTime() : "");

            // Model badge
            if (m.getModel() != null && !m.getModel().isEmpty()) {
                tvModel.setText(m.getModel());
                tvModel.setVisibility(View.VISIBLE);
            } else {
                tvModel.setVisibility(View.GONE);
            }

            // Disclaimer
            tvDisclaimer.setVisibility(m.isDisclaimer() ? View.VISIBLE : View.GONE);

            // Copy response button — use tag to avoid recycled-view bug
            btnCopyResponse.setTag(null); // clear any pending tag
            btnCopyResponse.setText("Copy");
            btnCopyResponse.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("response", m.getContent()));
                    btnCopyResponse.setText("Copied!");
                    // Tag the view with its message content so we only reset OUR copy button
                    btnCopyResponse.setTag(m.getContent());
                    v.postDelayed(() -> {
                        // Only reset if this view is still showing the same message
                        if (m.getContent().equals(btnCopyResponse.getTag())) {
                            btnCopyResponse.setText("Copy");
                            btnCopyResponse.setTag(null);
                        }
                    }, 2000);
                }
            });

            // Regenerate button — only on last AI message, same as web
            if (pos == lastAiPos && regenCb != null) {
                btnRegen.setVisibility(View.VISIBLE);
                // Find the most recent user message before this AI message
                String lastUserMsg = "";
                for (int i = pos - 1; i >= 0; i--) {
                    if (Message.ROLE_USER.equals(msgs.get(i).getRole())) {
                        lastUserMsg = msgs.get(i).getContent();
                        break;
                    }
                }
                final String userMsg = lastUserMsg;
                btnRegen.setOnClickListener(v -> regenCb.onRegen(userMsg));
            } else {
                btnRegen.setVisibility(View.GONE);
            }
        }
    }

    // ── Typing indicator ──────────────────────────────────────────
    static class TypingVH extends RecyclerView.ViewHolder {
        TypingVH(View v) { super(v); }
    }
}

