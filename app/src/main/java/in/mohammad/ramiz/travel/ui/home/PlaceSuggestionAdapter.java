package in.mohammad.ramiz.travel.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import in.mohammad.ramiz.travel.databinding.ItemPlaceSuggestionBinding;
import in.mohammad.ramiz.travel.domain.model.PlaceResult;
import in.mohammad.ramiz.travel.util.FormatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Live search suggestions under the "Where to?" field, closest place first. */
public class PlaceSuggestionAdapter
        extends RecyclerView.Adapter<PlaceSuggestionAdapter.Holder> {

    private final List<PlaceResult> items = new ArrayList<>();
    private final Consumer<PlaceResult> onClick;

    public PlaceSuggestionAdapter(Consumer<PlaceResult> onClick) {
        this.onClick = onClick;
    }

    public void submit(List<PlaceResult> places) {
        items.clear();
        if (places != null) items.addAll(places);
        notifyDataSetChanged();
    }

    public void clear() {
        submit(null);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPlaceSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlaceResult place = items.get(position);
        holder.binding.textPlaceName.setText(place.name != null ? place.name : "");
        holder.binding.textPlaceAddress.setText(place.address != null ? place.address : "");
        holder.binding.textPlaceDistance.setText(
                place.distanceMeters >= 0 ? FormatUtil.distance(place.distanceMeters) : "");
        holder.binding.getRoot().setOnClickListener(v -> onClick.accept(place));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemPlaceSuggestionBinding binding;

        Holder(ItemPlaceSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
