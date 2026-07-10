package in.mohammad.ramiz.travel.ui.timeline;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.mohammad.ramiz.travel.databinding.FragmentTimelineBinding;
import in.mohammad.ramiz.travel.ui.settings.SettingsDialog;
import in.mohammad.ramiz.travel.util.FormatUtil;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TimelineFragment extends Fragment {

    private FragmentTimelineBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimelineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TimelineViewModel viewModel = new ViewModelProvider(this).get(TimelineViewModel.class);

        JourneyAdapter adapter = new JourneyAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        viewModel.getHistory().observe(getViewLifecycleOwner(), journeys -> {
            adapter.submit(journeys);
            boolean empty = journeys == null || journeys.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        viewModel.getTotalDistance().observe(getViewLifecycleOwner(), total ->
                binding.textTotalDistance.setText(total != null
                        ? FormatUtil.distance(total) : "0 m"));
        viewModel.getTotalJourneys().observe(getViewLifecycleOwner(), total ->
                binding.textTotalJourneys.setText(String.valueOf(total != null ? total : 0)));

        binding.buttonSettings.setOnClickListener(v ->
                new SettingsDialog().show(getChildFragmentManager(), "settings"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

