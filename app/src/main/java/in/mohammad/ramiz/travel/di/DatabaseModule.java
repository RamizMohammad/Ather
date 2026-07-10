package in.mohammad.ramiz.travel.di;

import android.content.Context;

import androidx.room.Room;

import in.mohammad.ramiz.travel.data.local.AetherDatabase;
import in.mohammad.ramiz.travel.data.local.dao.FavoritePlaceDao;
import in.mohammad.ramiz.travel.data.local.dao.JourneyDao;
import in.mohammad.ramiz.travel.data.local.dao.NotificationDao;
import in.mohammad.ramiz.travel.data.local.dao.PackingDao;
import in.mohammad.ramiz.travel.data.local.dao.ReminderDao;
import in.mohammad.ramiz.travel.data.local.dao.SearchHistoryDao;
import in.mohammad.ramiz.travel.data.local.dao.StatisticsDao;
import in.mohammad.ramiz.travel.data.local.dao.WeatherCacheDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AetherDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AetherDatabase.class, "aether.db")
                // v1 ships with destructive fallback; real migrations are added from v2 on.
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    public JourneyDao provideJourneyDao(AetherDatabase db) {
        return db.journeyDao();
    }

    @Provides
    public ReminderDao provideReminderDao(AetherDatabase db) {
        return db.reminderDao();
    }

    @Provides
    public PackingDao providePackingDao(AetherDatabase db) {
        return db.packingDao();
    }

    @Provides
    public WeatherCacheDao provideWeatherCacheDao(AetherDatabase db) {
        return db.weatherCacheDao();
    }

    @Provides
    public FavoritePlaceDao provideFavoritePlaceDao(AetherDatabase db) {
        return db.favoritePlaceDao();
    }

    @Provides
    public SearchHistoryDao provideSearchHistoryDao(AetherDatabase db) {
        return db.searchHistoryDao();
    }

    @Provides
    public NotificationDao provideNotificationDao(AetherDatabase db) {
        return db.notificationDao();
    }

    @Provides
    public StatisticsDao provideStatisticsDao(AetherDatabase db) {
        return db.statisticsDao();
    }
}

