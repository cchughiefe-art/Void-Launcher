package com.voidlauncher.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WorkspaceAdapter extends RecyclerView.Adapter<WorkspaceAdapter.Holder> {
    interface Listener {
        void openWorkspaceItem(WorkspaceEntry item);
        void menuWorkspaceItem(WorkspaceEntry item, View anchor);
        void workspaceOrderChanged(List<WorkspaceEntry> items);
    }

    private final List<WorkspaceEntry> items = new ArrayList<>();
    private final Listener listener;
    private boolean showLabels = true;
    private int iconSizeDp = 56;

    WorkspaceAdapter(Listener listener) { this.listener = listener; }
    void setAppearance(boolean labels, int iconSize) { showLabels = labels; iconSizeDp = iconSize; notifyDataSetChanged(); }
    void submit(List<WorkspaceEntry> value) { items.clear(); items.addAll(value); notifyDataSetChanged(); }
    List<WorkspaceEntry> items() { return new ArrayList<>(items); }
    void move(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= items.size() || to >= items.size()) return;
        Collections.swap(items, from, to); notifyItemMoved(from, to); listener.workspaceOrderChanged(items());
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        WorkspaceEntry item = items.get(position);
        holder.label.setText(item.label); holder.label.setVisibility(showLabels ? View.VISIBLE : View.GONE);
        int px = (int)(iconSizeDp * holder.itemView.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams params = holder.icon.getLayoutParams(); params.width = px; params.height = px; holder.icon.setLayoutParams(params);
        holder.icon.setImageDrawable(item.icon); holder.itemView.setContentDescription(item.label + (item.type == WorkspaceEntry.Type.FOLDER ? " folder" : ""));
        holder.itemView.setOnClickListener(v -> listener.openWorkspaceItem(item));
        holder.itemView.setOnLongClickListener(v -> { listener.menuWorkspaceItem(item, v); return true; });
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon; final TextView label;
        Holder(View view) { super(view); icon = view.findViewById(R.id.icon); label = view.findViewById(R.id.label); }
    }
}
