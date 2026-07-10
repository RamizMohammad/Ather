package in.mohammad.ramiz.travel.di;

import android.content.Context;

import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.datastore.preferences.core.Preferences;

import in.mohammad.ramiz.travel.domain.engine.rules.AirQualityRule;
import in.mohammad.ramiz.travel.domain.engine.rules.CrosswindRule;
import in.mohammad.ramiz.travel.domain.engine.rules.FogRule;
import in.mohammad.ramiz.travel.domain.engine.rules.HeatHydrationRule;
import in.mohammad.ramiz.travel.domain.engine.rules.RainRule;
import in.mohammad.ramiz.travel.domain.engine.rules.RecommendationRule;
import in.mohammad.ramiz.travel.domain.engine.rules.SnowRule;
import in.mohammad.ramiz.travel.domain.engine.rules.StormRule;
import in.mohammad.ramiz.travel.domain.engine.rules.TemperatureDropRule;
import in.mohammad.ramiz.travel.domain.engine.rules.UvRule;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public FusedLocationProviderClient provideFusedLocationClient(@ApplicationContext Context context) {
        return LocationServices.getFusedLocationProviderClient(context);
    }

    @Provides
    @Singleton
    public RxDataStore<Preferences> provideDataStore(@ApplicationContext Context context) {
        return new RxPreferenceDataStoreBuilder(context, "aether_settings").build();
    }

    /**
     * The rule registry. Adding a new recommendation = adding one class here.
     * Existing rules are never modified (Open/Closed principle).
     */
    @Provides
    @Singleton
    public List<RecommendationRule> provideRecommendationRules() {
        return Arrays.asList(
                new RainRule(),
                new UvRule(),
                new TemperatureDropRule(),
                new CrosswindRule(),
                new AirQualityRule(),
                new HeatHydrationRule(),
                new StormRule(),
                new FogRule(),
                new SnowRule()
        );
    }
}

