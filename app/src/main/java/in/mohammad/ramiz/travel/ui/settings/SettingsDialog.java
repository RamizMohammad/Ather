package in.mohammad.ramiz.travel.ui.settings;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.repository.SettingsRepository;
import in.mohammad.ramiz.travel.databinding.DialogSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

/** Transport mode + notification/voice/traffic toggles. */
@AndroidEntryPoint
public class SettingsDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SettingsViewModel viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        SettingsRepository repo = viewModel.repo();
        DialogSettingsBinding binding = DialogSettingsBinding.inflate(getLayoutInflater());

        boolean motorcycle = JourneyEntity.MODE_MOTORCYCLE.equals(repo.getTransportMode());
        binding.toggleMode.check(motorcycle ? binding.buttonMotorcycle.getId()
                : binding.buttonCar.getId());
        binding.switchVoice.setChecked(repo.isVoiceGuidanceEnabled());
        binding.switchTraffic.setChecked(repo.isTrafficLayerEnabled());
        binding.switchInsights.setChecked(repo.areInsightNotificationsEnabled());

        binding.toggleMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            repo.setString(SettingsRepository.KEY_TRANSPORT_MODE,
                    checkedId == binding.buttonMotorcycle.getId()
                            ? JourneyEntity.MODE_MOTORCYCLE : JourneyEntity.MODE_CAR);
        });
        binding.switchVoice.setOnCheckedChangeListener((b, c) ->
                repo.setBoolean(SettingsRepository.KEY_VOICE_GUIDANCE, c));
        binding.switchTraffic.setOnCheckedChangeListener((b, c) ->
                repo.setBoolean(SettingsRepository.KEY_TRAFFIC_LAYER, c));
        binding.switchInsights.setOnCheckedChangeListener((b, c) ->
                repo.setBoolean(SettingsRepository.KEY_INSIGHT_NOTIFICATIONS, c));

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Settings")
                .setView(binding.getRoot())
                .setPositiveButton("Done", null)
                .create();
    }
}

