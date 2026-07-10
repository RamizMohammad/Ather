package in.mohammad.ramiz.travel.di;

import in.mohammad.ramiz.travel.data.remote.api.WeatherApiService;
import com.google.gson.Gson;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit is now only used for WeatherAPI.com.
 * Maps, routing and search all go through the Mappls SDK, which manages
 * its own HTTP stack and authentication.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String WEATHER_BASE_URL = "https://api.weatherapi.com/v1/";

    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    @Provides
    @Singleton
    @Named("weather")
    public Retrofit provideWeatherRetrofit(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(WEATHER_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public WeatherApiService provideWeatherApiService(@Named("weather") Retrofit retrofit) {
        return retrofit.create(WeatherApiService.class);
    }
}

