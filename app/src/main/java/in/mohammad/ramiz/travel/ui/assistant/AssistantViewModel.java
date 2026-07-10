package in.mohammad.ramiz.travel.ui.assistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.data.local.dao.JourneyDao;
import in.mohammad.ramiz.travel.data.local.dao.NotificationDao;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.local.entity.NotificationEntity;
import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.data.repository.ReminderRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AssistantViewModel extends ViewModel {

    private final NotificationDao notificationDao;
    private final ReminderRepository reminderRepository;
    private final JourneyDao journeyDao;

    @Inject
    public AssistantViewModel(NotificationDao notificationDao,
                              ReminderRepository reminderRepository,
                              JourneyDao journeyDao) {
        this.notificationDao = notificationDao;
        this.reminderRepository = reminderRepository;
        this.journeyDao = journeyDao;
    }

    /** Non-null only while a ride is in progress; the assistant is gated on this. */
    public LiveData<JourneyEntity> getActiveJourney() {
        return journeyDao.observeActive();
    }

    public LiveData<List<NotificationEntity>> getFeed() {
        return notificationDao.observeRecent();
    }

    public LiveData<List<ReminderEntity>> getReminders() {
        return reminderRepository.observeActive();
    }

    public void addReminder(ReminderEntity reminder) {
        reminderRepository.add(reminder);
    }

    public void completeReminder(long id) {
        reminderRepository.markDone(id);
    }
}

