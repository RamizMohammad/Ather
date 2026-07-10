package in.mohammad.ramiz.travel.ui.assistant;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.data.repository.PlacesRepository;
import in.mohammad.ramiz.travel.databinding.DialogAddReminderBinding;
import in.mohammad.ramiz.travel.domain.model.PlaceResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Create a smart todo: title + place (resolved through Places text search)
 * + trigger (10/5/2 min before arrival, on arrival, after leaving).
 */
@AndroidEntryPoint
public class AddReminderDialog extends DialogFragment {

    @Inject
    PlacesRepository placesRepository;

    @Nullable
    private final Consumer<ReminderEntity> onCreate;

    /** Required for fragment recreation; the dialog is then inert and simply dismissible. */
    public AddReminderDialog() {
        this(null);
    }

    public AddReminderDialog(@Nullable Consumer<ReminderEntity> onCreate) {
        this.onCreate = onCreate;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogAddReminderBinding binding =
                DialogAddReminderBinding.inflate(getLayoutInflater());

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("New reminder")
                        .setView(binding.getRoot())
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", null) // set below to control dismissal
                        .create();

        // Validate before dismissing: empty fields show inline errors instead.
        dialog.setOnShowListener(d -> dialog
                .getButton(Dialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = text(binding.inputTitle.getText());
                    String placeQuery = text(binding.inputPlace.getText());
                    binding.layoutTitle.setError(title.isEmpty() ? "Required" : null);
                    binding.layoutPlace.setError(placeQuery.isEmpty() ? "Required" : null);
                    if (title.isEmpty() || placeQuery.isEmpty()) return;
                    resolveAndSave(title, placeQuery,
                            triggerPosition(binding.chipsTrigger.getCheckedChipId()));
                    dialog.dismiss();
                }));
        return dialog;
    }

    /** Maps the checked chip to the legacy trigger index used by resolveAndSave. */
    private int triggerPosition(int checkedChipId) {
        if (checkedChipId == in.mohammad.ramiz.travel.R.id.chip_10_min) return 0;
        if (checkedChipId == in.mohammad.ramiz.travel.R.id.chip_5_min) return 1;
        if (checkedChipId == in.mohammad.ramiz.travel.R.id.chip_2_min) return 2;
        if (checkedChipId == in.mohammad.ramiz.travel.R.id.chip_after_leaving) return 4;
        return 3; // on arrival (default)
    }

    private String text(CharSequence cs) {
        return cs != null ? cs.toString().trim() : "";
    }

    private void resolveAndSave(String title, String placeQuery, int triggerPos) {
        placesRepository.searchDestination(placeQuery, (Result<List<PlaceResult>> result) -> {
            if (!result.hasData() || result.getData().isEmpty() || onCreate == null) return;
            PlaceResult place = result.getData().get(0);

            ReminderEntity r = new ReminderEntity();
            r.title = title;
            r.placeName = place.name;
            r.placeLat = place.lat;
            r.placeLng = place.lng;
            switch (triggerPos) {
                case 0: r.triggerType = ReminderEntity.TRIGGER_MINUTES_BEFORE; r.minutesBefore = 10; break;
                case 1: r.triggerType = ReminderEntity.TRIGGER_MINUTES_BEFORE; r.minutesBefore = 5; break;
                case 2: r.triggerType = ReminderEntity.TRIGGER_MINUTES_BEFORE; r.minutesBefore = 2; break;
                case 3: r.triggerType = ReminderEntity.TRIGGER_ON_ARRIVAL; break;
                default: r.triggerType = ReminderEntity.TRIGGER_AFTER_LEAVING; r.radiusMeters = 200; break;
            }
            onCreate.accept(r);
        });
    }
}

