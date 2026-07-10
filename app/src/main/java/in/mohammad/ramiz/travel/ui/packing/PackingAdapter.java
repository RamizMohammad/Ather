package in.mohammad.ramiz.travel.ui.packing;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.mohammad.ramiz.travel.R;
import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;
import in.mohammad.ramiz.travel.databinding.ItemPackingBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class PackingAdapter extends RecyclerView.Adapter<PackingAdapter.Holder> {

    public interface PackedListener {
        void onPacked(long id, boolean packed);
    }

    private final List<PackingEntity> items = new ArrayList<>();
    private final PackedListener packedListener;
    private final LongConsumer dismissListener;

    public PackingAdapter(PackedListener packedListener, LongConsumer dismissListener) {
        this.packedListener = packedListener;
        this.dismissListener = dismissListener;
    }

    public void submit(List<PackingEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPackingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PackingEntity item = items.get(position);
        holder.binding.textName.setText(item.itemName);
        holder.binding.textReason.setText(item.reason);
        holder.binding.textSource.setText(sourceLabel(item.source));

        String priority;
        int color;
        if (item.confidence >= 75) {
            priority = "HIGH PRIORITY";
            color = R.color.priority_high;
        } else if (item.confidence >= 45) {
            priority = "MEDIUM PRIORITY";
            color = R.color.aether_accent;
        } else {
            priority = "LOW PRIORITY";
            color = R.color.outline;
        }
        holder.binding.textPriority.setText(priority);
        holder.binding.textPriority.setTextColor(
                holder.binding.getRoot().getContext().getColor(color));

        holder.binding.checkPacked.setOnCheckedChangeListener(null);
        holder.binding.checkPacked.setChecked(item.isPacked);
        holder.binding.checkPacked.setOnCheckedChangeListener(
                (btn, checked) -> packedListener.onPacked(item.id, checked));
        holder.binding.buttonDismiss.setOnClickListener(v -> dismissListener.accept(item.id));
    }

    private String sourceLabel(String source) {
        switch (source) {
            case PackingEntity.SOURCE_WEATHER: return "WEATHER DRIVEN";
            case PackingEntity.SOURCE_TEMPERATURE: return "TEMPERATURE DRIVEN";
            case PackingEntity.SOURCE_MODE: return "RIDE GEAR";
            case PackingEntity.SOURCE_DURATION: return "TRIP LENGTH";
            default: return "PRECAUTIONARY";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPackingBinding binding;

        Holder(ItemPackingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

