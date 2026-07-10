package in.mohammad.ramiz.travel.ui.settings;

import androidx.datastore.preferences.core.Preferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.data.repository.SettingsRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final SettingsRepository settingsRepository;

    @Inject
    public SettingsViewModel(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public LiveData<Preferences> observe() {
        return settingsRepository.observe();
    }

    public SettingsRepository repo() {
        return settingsRepository;
    }
}

