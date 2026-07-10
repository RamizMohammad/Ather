package in.mohammad.ramiz.travel.data.repository;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * User preferences via Jetpack DataStore (RxJava3 flavor for Java).
 * Exposed as LiveData for easy consumption from Java ViewModels.
 */
@Singleton
public class SettingsRepository {

    public static final Preferences.Key<String> KEY_TRANSPORT_MODE =
            PreferencesKeys.stringKey("transport_mode");
    public static final Preferences.Key<Boolean> KEY_VOICE_GUIDANCE =
            PreferencesKeys.booleanKey("voice_guidance");
    public static final Preferences.Key<Boolean> KEY_TRAFFIC_LAYER =
            PreferencesKeys.booleanKey("traffic_layer");
    public static final Preferences.Key<Boolean> KEY_INSIGHT_NOTIFICATIONS =
            PreferencesKeys.booleanKey("insight_notifications");

    private final RxDataStore<Preferences> dataStore;
    private final MutableLiveData<Preferences> live = new MutableLiveData<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final Disposable subscription;

    @Inject
    public SettingsRepository(RxDataStore<Preferences> dataStore) {
        this.dataStore = dataStore;
        this.subscription = dataStore.data().subscribe(live::postValue, throwable -> { });
    }

    public LiveData<Preferences> observe() {
        return live;
    }

    public String getTransportMode() {
        Preferences p = live.getValue();
        String v = p != null ? p.get(KEY_TRANSPORT_MODE) : null;
        return v != null ? v : "CAR";
    }

    public boolean isVoiceGuidanceEnabled() {
        Preferences p = live.getValue();
        Boolean v = p != null ? p.get(KEY_VOICE_GUIDANCE) : null;
        return v == null || v;
    }

    public boolean isTrafficLayerEnabled() {
        Preferences p = live.getValue();
        Boolean v = p != null ? p.get(KEY_TRAFFIC_LAYER) : null;
        return v == null || v;
    }

    public boolean areInsightNotificationsEnabled() {
        Preferences p = live.getValue();
        Boolean v = p != null ? p.get(KEY_INSIGHT_NOTIFICATIONS) : null;
        return v == null || v;
    }

    public void setString(Preferences.Key<String> key, String value) {
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(key, value);
            return Single.just(mutable);
        });
    }

    public void setBoolean(Preferences.Key<Boolean> key, boolean value) {
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(key, value);
            return Single.just(mutable);
        });
    }
}

