package in.mohammad.ramiz.travel.ui.assistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import in.mohammad.ramiz.travel.databinding.FragmentAssistantBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Assistant timeline: active smart-todos on top, then the proactive insight feed
 * (everything Aether has told the user, silent or not). FAB adds a reminder.
 */
@AndroidEntryPoint
public class AssistantFragment extends Fragment {

    private FragmentAssistantBinding binding;
    private AssistantViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AssistantViewModel.class);

        ReminderAdapter reminderAdapter = new ReminderAdapter(id -> viewModel.completeReminder(id));
        FeedAdapter feedAdapter = new FeedAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(new ConcatAdapter(reminderAdapter, feedAdapter));

        viewModel.getReminders().observe(getViewLifecycleOwner(), reminderAdapter::submit);
        viewModel.getFeed().observe(getViewLifecycleOwner(), feedAdapter::submit);

        binding.fabAddReminder.setOnClickListener(v ->
                new AddReminderDialog(reminder -> viewModel.addReminder(reminder))
                        .show(getChildFragmentManager(), "add_reminder"));

        // The assistant only runs during a ride: without an ACTIVE journey the
        // screen stays on standby and reminders can't be added.
        viewModel.getActiveJourney().observe(getViewLifecycleOwner(), journey -> {
            boolean riding = journey != null;
            binding.textHeader.setText(riding ? "AETHER ACTIVE" : "AETHER STANDBY");
            binding.recycler.setVisibility(riding ? View.VISIBLE : View.GONE);
            binding.groupStandby.setVisibility(riding ? View.GONE : View.VISIBLE);
            if (riding) {
                binding.fabAddReminder.show();
            } else {
                binding.fabAddReminder.hide();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

