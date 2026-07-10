package in.mohammad.ramiz.travel.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import in.mohammad.ramiz.travel.data.local.dao.FavoritePlaceDao;
import in.mohammad.ramiz.travel.data.local.dao.JourneyDao;
import in.mohammad.ramiz.travel.data.local.dao.NotificationDao;
import in.mohammad.ramiz.travel.data.local.dao.PackingDao;
import in.mohammad.ramiz.travel.data.local.dao.ReminderDao;
import in.mohammad.ramiz.travel.data.local.dao.SearchHistoryDao;
import in.mohammad.ramiz.travel.data.local.dao.StatisticsDao;
import in.mohammad.ramiz.travel.data.local.dao.WeatherCacheDao;
import in.mohammad.ramiz.travel.data.local.entity.FavoritePlaceEntity;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.local.entity.NotificationEntity;
import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;
import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.data.local.entity.SearchHistoryEntity;
import in.mohammad.ramiz.travel.data.local.entity.StatisticsEntity;
import in.mohammad.ramiz.travel.data.local.entity.WeatherCacheEntity;

@Database(
        entities = {
                JourneyEntity.class,
                ReminderEntity.class,
                PackingEntity.class,
                WeatherCacheEntity.class,
                FavoritePlaceEntity.class,
                SearchHistoryEntity.class,
                NotificationEntity.class,
                StatisticsEntity.class
        },
        version = 1,
        exportSchema = false)
public abstract class AetherDatabase extends RoomDatabase {

    public abstract JourneyDao journeyDao();

    public abstract ReminderDao reminderDao();

    public abstract PackingDao packingDao();

    public abstract WeatherCacheDao weatherCacheDao();

    public abstract FavoritePlaceDao favoritePlaceDao();

    public abstract SearchHistoryDao searchHistoryDao();

    public abstract NotificationDao notificationDao();

    public abstract StatisticsDao statisticsDao();
}

