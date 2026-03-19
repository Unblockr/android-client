package io.netbird.client.ui.egress;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.netbird.client.R;

public class EgressGroupAdapter extends RecyclerView.Adapter<EgressGroupAdapter.ViewHolder> {

    public interface OnGroupSelectedListener {
        void onGroupSelected(EgressGroup group);
    }

    private List<EgressGroup> groups = new ArrayList<>();
    private String selectedGroupId = null;
    private final OnGroupSelectedListener listener;

    public EgressGroupAdapter(OnGroupSelectedListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<EgressGroup> groups, String selectedGroupId) {
        this.groups = groups != null ? groups : new ArrayList<>();
        this.selectedGroupId = selectedGroupId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_egress_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EgressGroup group = groups.get(position);
        boolean isSelected = group.id.equals(selectedGroupId);

        holder.name.setText(group.name);
        holder.peers.setText(holder.itemView.getContext()
                .getString(R.string.egress_peers, group.peersCount));
        holder.radio.setChecked(isSelected);

        holder.itemView.setOnClickListener(v -> listener.onGroupSelected(group));
        holder.radio.setOnClickListener(v -> listener.onGroupSelected(group));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RadioButton radio;
        final TextView name;
        final TextView peers;

        ViewHolder(View itemView) {
            super(itemView);
            radio = itemView.findViewById(R.id.radio_egress_group);
            name = itemView.findViewById(R.id.text_egress_group_name);
            peers = itemView.findViewById(R.id.text_egress_group_peers);
        }
    }
}
