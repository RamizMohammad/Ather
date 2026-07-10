package in.mohammad.ramiz.travel.core;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppExecutors {

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    @Inject
    public AppExecutors() {
        this(Executors.newSingleThreadExecutor(),
                Executors.newFixedThreadPool(3),
                new MainThreadExecutor());
    }

    public AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor network() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    public void postToMain(Runnable runnable) {
        mainThread.execute(runnable);
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
