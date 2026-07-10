package in.mohammad.ramiz.travel.ui.assistant;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.databinding.ItemReminderBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.Holder> {

    private final List<ReminderEntity> items = new ArrayList<>();
    private final LongConsumer onComplete;

    public ReminderAdapter(LongConsumer onComplete) {
        this.onComplete = onComplete;
    }

    public void submit(List<ReminderEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemReminderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ReminderEntity r = items.get(position);
        holder.binding.textTitle.setText(r.title);
        String trigger;
        switch (r.triggerType) {
            case ReminderEntity.TRIGGER_ON_ARRIVAL:
                trigger = "On arrival";
                break;
            case ReminderEntity.TRIGGER_AFTER_LEAVING:
                trigger = "After leaving";
                break;
            case ReminderEntity.TRIGGER_RADIUS:
                trigger = "When nearby";
                break;
            default:
                trigger = r.minutesBefore + " min before arrival";
        }
        holder.binding.textSubtitle.setText(r.placeName + " Â· " + trigger);
        holder.binding.checkDone.setOnCheckedChangeListener(null);
        holder.binding.checkDone.setChecked(false);
        holder.binding.checkDone.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) onComplete.accept(r.id);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemReminderBinding binding;

        Holder(ItemReminderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

