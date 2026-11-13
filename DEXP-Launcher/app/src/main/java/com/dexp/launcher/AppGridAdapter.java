package com.dexp.launcher;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Простой и быстрый адаптер сетки приложений.
 * Никаких Glide/Picasso — иконки грузятся напрямую из PackageManager (кэшируются в модели).
 */
public class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.VH> {

    public interface Listener {
        void onClick(AppInfoItem item);
        boolean onLongClick(AppInfoItem item);
    }

    private final List<AppInfoItem> data;
    private final PackageManager pm;
    private final Listener listener;

    public AppGridAdapter(List<AppInfoItem> data, PackageManager pm, Listener listener) {
        this.data = data;
        this.pm = pm;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        final android.widget.ImageView icon;
        final TextView label;

        VH(View v) {
            super(v);
            icon = (android.widget.ImageView) v.findViewById(R.id.appIcon);
            label = (TextView) v.findViewById(R.id.appLabel);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, final int position) {
        final AppInfoItem item = data.get(position);
        holder.label.setText(item.label);

        Drawable d = item.getIcon(pm);
        if (d != null) {
            holder.icon.setImageDrawable(d);
        } else {
            holder.icon.setImageResource(R.drawable.ic_default_app);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onClick(item);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return listener != null && listener.onLongClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
