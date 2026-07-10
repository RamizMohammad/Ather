package in.mohammad.ramiz.travel.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.dao.SearchHistoryDao;
import in.mohammad.ramiz.travel.data.local.entity.SearchHistoryEntity;
import in.mohammad.ramiz.travel.domain.model.PlaceResult;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.autosuggest.MapplsAutoSuggest;
import com.mappls.sdk.services.api.autosuggest.MapplsAutosuggestManager;
import com.mappls.sdk.services.api.autosuggest.model.AutoSuggestAtlasResponse;
import com.mappls.sdk.services.api.autosuggest.model.ELocation;
import com.mappls.sdk.services.api.nearby.MapplsNearby;
import com.mappls.sdk.services.api.nearby.MapplsNearbyManager;
import com.mappls.sdk.services.api.nearby.model.NearbyAtlasResponse;
import com.mappls.sdk.services.api.nearby.model.NearbyAtlasResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Destination search (Mappls AutoSuggest / Atlas) + nearby POI lookup
 * (Mappls Nearby: "petrol pump", "parking", ...). OAuth is handled internally
 * by the SDK via the Atlas client id/secret set in AetherApplication.
 * Results are mapped to the provider-agnostic PlaceResult; every successful
 * search is recorded to local history for offline suggestions.
 */
@Singleton
public class PlacesRepository {

    private final SearchHistoryDao searchHistoryDao;
    private final AppExecutors executors;

    @Nullable
    private volatile android.location.Location bias;

    public interface Callback {
        void onResult(Result<List<PlaceResult>> result);
    }

    @Inject
    public PlacesRepository(SearchHistoryDao searchHistoryDao, AppExecutors executors) {
        this.searchHistoryDao = searchHistoryDao;
        this.executors = executors;
    }

    /** Optional: bias search ranking towards the user's position. */
    public void setLocationBias(@Nullable android.location.Location location) {
        this.bias = location;
    }

    public void searchDestination(@NonNull String query, @NonNull Callback callback) {
        MapplsAutoSuggest.Builder builder = MapplsAutoSuggest.builder().query(query);
        android.location.Location loc = bias;
        if (loc != null) {
            builder.setLocation(loc.getLatitude(), loc.getLongitude());
        }
        MapplsAutosuggestManager.newInstance(builder.build()).call(
                new OnResponseCallback<AutoSuggestAtlasResponse>() {
                    @Override
                    public void onSuccess(AutoSuggestAtlasResponse response) {
                        List<PlaceResult> results = new ArrayList<>();
                        if (response != null && response.getSuggestedLocations() != null) {
                            for (ELocation e : response.getSuggestedLocations()) {
                                PlaceResult p = fromELocation(e);
                                if (p != null) results.add(p);
                            }
                        }
                        if (!results.isEmpty()) {
                            recordSearch(query, results.get(0));
                        }
                        callback.onResult(Result.success(results));
                    }

                    @Override
                    public void onError(int code, String message) {
                        callback.onResult(Result.error(
                                new IOException("Search error " + code + ": " + message),
                                Collections.emptyList()));
                    }
                });
    }

    /** e.g. keyword = "petrol pump" or "parking". */
    public void nearby(double lat, double lng, @NonNull String keyword,
                       @NonNull Callback callback) {
        MapplsNearby nearby = MapplsNearby.builder()
                .setLocation(lat, lng)
                .keyword(keyword)
                .build();
        MapplsNearbyManager.newInstance(nearby).call(
                new OnResponseCallback<NearbyAtlasResponse>() {
                    @Override
                    public void onSuccess(NearbyAtlasResponse response) {
                        List<PlaceResult> results = new ArrayList<>();
                        if (response != null && response.getSuggestedLocations() != null) {
                            for (NearbyAtlasResult r : response.getSuggestedLocations()) {
                                PlaceResult p = fromNearby(r);
                                if (p != null) results.add(p);
                            }
                        }
                        callback.onResult(Result.success(results));
                    }

                    @Override
                    public void onError(int code, String message) {
                        callback.onResult(Result.error(
                                new IOException("Nearby error " + code + ": " + message),
                                Collections.emptyList()));
                    }
                });
    }

    // ------------------------------------------------------------------

    @Nullable
    private PlaceResult fromELocation(ELocation e) {
        if (e == null || e.latitude == null || e.longitude == null) return null;
        return new PlaceResult(e.placeName, e.placeAddress,
                e.latitude, e.longitude, e.mapplsPin);
    }

    @Nullable
    private PlaceResult fromNearby(NearbyAtlasResult r) {
        if (r == null || r.getLatitude() == null || r.getLongitude() == null) return null;
        PlaceResult p = new PlaceResult();
        p.name = r.getPlaceName();
        p.address = r.getPlaceAddress();
        p.lat = r.getLatitude();
        p.lng = r.getLongitude();
        // Note: NearbyAtlasResult's eLoc accessor name varies across SDK versions
        // and the pin isn't used for nearby POIs â€” skip it.
        return p;
    }

    private void recordSearch(String query, PlaceResult top) {
        executors.diskIO().execute(() -> {
            SearchHistoryEntity entry = new SearchHistoryEntity();
            entry.query = query;
            entry.resolvedName = top.name;
            entry.lat = top.lat;
            entry.lng = top.lng;
            entry.searchedAt = System.currentTimeMillis();
            searchHistoryDao.insert(entry);
            searchHistoryDao.pruneToLimit();
        });
    }
}

