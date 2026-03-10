package com.eimemes.chat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.eimemes.chat.R;
import com.eimemes.chat.R;
import com.eimemes.chat.models.Conversation;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {

    public interface OnClick { void onClick(Conversation c); }

    private final List<Conversation> list;
    private final OnClick            listener;
    private       String             activeId = null;

    public ConversationAdapter(List<Conversation> list, OnClick l) {
        this.list = list; this.listener = l;
    }

    public void setActiveId(String id) { this.activeId = id; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_conversation, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Conversation c      = list.get(i);
        boolean      active = c.getId().equals(activeId);
        h.tvTitle.setText(c.getTitle());
        h.tvTitle.setTextColor(active
            ? h.itemView.getContext().getResources().getColor(R.color.accent)
            : h.itemView.getContext().getResources().getColor(R.color.text2));
        h.tvTitle.setAlpha(active ? 1f : 0.85f);
        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        VH(View v) { super(v); tvTitle = v.findViewById(R.id.tvTitle); }
    }
}
