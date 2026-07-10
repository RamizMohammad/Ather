package in.mohammad.ramiz.travel.data.repository;

import androidx.lifecycle.LiveData;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.data.local.dao.FavoritePlaceDao;
import in.mohammad.ramiz.travel.data.local.dao.SearchHistoryDao;
import in.mohammad.ramiz.travel.data.local.entity.FavoritePlaceEntity;
import in.mohammad.ramiz.travel.data.local.entity.SearchHistoryEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Saved places + recent searches (fully offline). */
@Singleton
public class FavoritesRepository {

    private final FavoritePlaceDao favoriteDao;
    private final SearchHistoryDao searchDao;
    private final AppExecutors executors;

    @Inject
    public FavoritesRepository(FavoritePlaceDao favoriteDao, SearchHistoryDao searchDao,
                               AppExecutors executors) {
        this.favoriteDao = favoriteDao;
        this.searchDao = searchDao;
        this.executors = executors;
    }

    public LiveData<List<FavoritePlaceEntity>> observeFavorites() {
        return favoriteDao.observeAll();
    }

    public LiveData<List<SearchHistoryEntity>> observeRecentSearches() {
        return searchDao.observeRecent();
    }

    public void save(FavoritePlaceEntity place) {
        executors.diskIO().execute(() -> {
            place.createdAt = System.currentTimeMillis();
            favoriteDao.insert(place);
        });
    }

    public void remove(FavoritePlaceEntity place) {
        executors.diskIO().execute(() -> favoriteDao.delete(place));
    }

    public void clearSearchHistory() {
        executors.diskIO().execute(searchDao::clearAll);
    }
}

