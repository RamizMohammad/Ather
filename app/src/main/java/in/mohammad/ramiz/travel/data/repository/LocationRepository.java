package in.mohammad.ramiz.travel.data.repository;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Wraps FusedLocationProvider behind LiveData.
 * Callers are responsible for holding ACCESS_FINE_LOCATION before subscribing;
 * SecurityException is caught and surfaced as null location.
 */
@Singleton
public class LocationRepository {

    private final FusedLocationProviderClient fusedClient;
    private final MutableLiveData<Location> lastLocation = new MutableLiveData<>();

    private final LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            Location loc = result.getLastLocation();
            if (loc != null) lastLocation.postValue(loc);
        }
    };

    private boolean updating;

    @Inject
    public LocationRepository(FusedLocationProviderClient fusedClient) {
        this.fusedClient = fusedClient;
    }

    public LiveData<Location> observeLocation() {
        return lastLocation;
    }

    @Nullable
    public Location getCachedLocation() {
        return lastLocation.getValue();
    }

    @SuppressLint("MissingPermission")
    public void startUpdates(long intervalMillis) {
        if (updating) return;
        try {
            LocationRequest request = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                    .setMinUpdateIntervalMillis(intervalMillis / 2)
                    .build();
            fusedClient.requestLocationUpdates(request, callback, null);
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) lastLocation.postValue(loc);
            });
            updating = true;
        } catch (SecurityException ignored) {
            // Permission not yet granted; UI layer will request it.
        }
    }

    public void stopUpdates() {
        if (!updating) return;
        fusedClient.removeLocationUpdates(callback);
        updating = false;
    }
}

