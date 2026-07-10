package in.mohammad.ramiz.travel.ui.packing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;
import in.mohammad.ramiz.travel.data.repository.JourneyRepository;
import in.mohammad.ramiz.travel.data.repository.PackingRepository;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Shows the packing list of the active (or most recently planned) journey. */
@HiltViewModel
public class PackingViewModel extends ViewModel {

    private final PackingRepository packingRepository;
    private final MediatorLiveData<List<PackingEntity>> items = new MediatorLiveData<>();
    private LiveData<List<PackingEntity>> currentSource;

    @Inject
    public PackingViewModel(JourneyRepository journeyRepository,
                            PackingRepository packingRepository) {
        this.packingRepository = packingRepository;
        items.addSource(journeyRepository.observeActive(), journey -> {
            if (journey != null) {
                switchTo(journey);
            } else {
                items.setValue(Collections.emptyList());
            }
        });
    }

    private void switchTo(JourneyEntity journey) {
        if (currentSource != null) items.removeSource(currentSource);
        currentSource = packingRepository.observeForJourney(journey.id);
        items.addSource(currentSource, items::setValue);
    }

    public LiveData<List<PackingEntity>> getItems() {
        return items;
    }

    public void setPacked(long id, boolean packed) {
        packingRepository.setPacked(id, packed);
    }

    public void dismiss(long id) {
        packingRepository.dismiss(id);
    }
}

