package in.mohammad.ramiz.travel.ui.navigation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.repository.JourneyRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Supplies the active journey (route polyline, destination) to the navigation map. */
@HiltViewModel
public class NavigationViewModel extends ViewModel {

    private final JourneyRepository journeyRepository;

    @Inject
    public NavigationViewModel(JourneyRepository journeyRepository) {
        this.journeyRepository = journeyRepository;
    }

    public LiveData<JourneyEntity> observeJourney(long journeyId) {
        return journeyRepository.observeById(journeyId);
    }
}
