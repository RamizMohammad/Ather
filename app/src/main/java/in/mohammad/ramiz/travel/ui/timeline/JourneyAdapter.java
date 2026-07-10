package in.mohammad.ramiz.travel.ui.timeline;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.databinding.ItemJourneyBinding;
import in.mohammad.ramiz.travel.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;

public class JourneyAdapter extends RecyclerView.Adapter<JourneyAdapter.Holder> {

    private final List<JourneyEntity> items = new ArrayList<>();

    public void submit(List<JourneyEntity> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemJourneyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        JourneyEntity j = items.get(position);
        holder.binding.textTitle.setText(
                (j.originName != null ? shorten(j.originName) + " â†’ " : "") + j.destName);
        long durationSec = Math.max(0, (j.completedAt - j.startedAt) / 1000);
        holder.binding.textStats.setText(FormatUtil.dateShort(j.completedAt)
                + " Â· " + FormatUtil.distance(j.travelledMeters)
                + " Â· " + FormatUtil.duration(durationSec)
                + " Â· avg " + Math.round(j.avgSpeedKmh) + " km/h");
        holder.binding.textWeather.setText(j.weatherSummary != null ? j.weatherSummary : "");
        holder.binding.textMode.setText(
                JourneyEntity.MODE_MOTORCYCLE.equals(j.transportMode) ? "MOTORCYCLE" : "CAR");
    }

    private String shorten(String s) {
        int comma = s.indexOf(',');
        return comma > 0 ? s.substring(0, comma) : s;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemJourneyBinding binding;

        Holder(ItemJourneyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

