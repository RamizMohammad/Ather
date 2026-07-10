package in.mohammad.ramiz.travel.ui.assistant;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.mohammad.ramiz.travel.data.local.entity.NotificationEntity;
import in.mohammad.ramiz.travel.databinding.ItemFeedBinding;
import in.mohammad.ramiz.travel.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.Holder> {

    private final List<NotificationEntity> items = new ArrayList<>();

    public void submit(List<NotificationEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemFeedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        NotificationEntity n = items.get(position);
        holder.binding.textTitle.setText(n.title);
        holder.binding.textMessage.setText(n.message);
        holder.binding.textTime.setText(FormatUtil.dateShort(n.shownAt)
                + " Â· " + FormatUtil.clockTime(n.shownAt));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemFeedBinding binding;

        Holder(ItemFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

