package com.voidlauncher.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
    interface Listener { void open(AppEntry app); void menu(AppEntry app, View anchor); }
    private final List<AppEntry> shown = new ArrayList<>();
    private final Listener listener;

    AppAdapter(Listener listener) { this.listener = listener; }

    void submit(List<AppEntry> apps) {
        shown.clear(); shown.addAll(apps); notifyDataSetChanged();
    }

    AppEntry first() { return shown.isEmpty() ? null : shown.get(0); }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p, int type) {
        return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_app, p, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
        AppEntry app = shown.get(pos);
        h.label.setText(app.label); h.icon.setImageDrawable(app.icon);
        h.itemView.setOnClickListener(v -> listener.open(app));
        h.itemView.setOnLongClickListener(v -> { listener.menu(app, v); return true; });
    }
    @Override public int getItemCount() { return shown.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon; final TextView label;
        Holder(View v) { super(v); icon = v.findViewById(R.id.icon); label = v.findViewById(R.id.label); }
    }
}
