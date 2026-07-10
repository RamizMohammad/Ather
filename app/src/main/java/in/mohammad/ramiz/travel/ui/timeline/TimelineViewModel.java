package in.mohammad.ramiz.travel.ui.timeline;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.data.local.dao.StatisticsDao;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.repository.JourneyRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TimelineViewModel extends ViewModel {

    private final JourneyRepository journeyRepository;
    private final StatisticsDao statisticsDao;

    @Inject
    public TimelineViewModel(JourneyRepository journeyRepository, StatisticsDao statisticsDao) {
        this.journeyRepository = journeyRepository;
        this.statisticsDao = statisticsDao;
    }

    public LiveData<List<JourneyEntity>> getHistory() {
        return journeyRepository.observeHistory();
    }

    public LiveData<Double> getTotalDistance() {
        return statisticsDao.observeTotalDistance();
    }

    public LiveData<Integer> getTotalJourneys() {
        return statisticsDao.observeTotalJourneys();
    }
}

