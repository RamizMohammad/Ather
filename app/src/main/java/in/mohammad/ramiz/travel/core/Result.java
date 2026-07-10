package in.mohammad.ramiz.travel.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Result<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    @NonNull
    private final Status status;
    @Nullable
    private final T data;
    @Nullable
    private final Throwable error;

    private Result(@NonNull Status status, @Nullable T data, @Nullable Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(Status.SUCCESS, data, null);
    }

    public static <T> Result<T> error(@NonNull Throwable error, @Nullable T data) {
        return new Result<>(Status.ERROR, data, error);
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null);
    }

    public static <T> Result<T> stale(@NonNull T data) {
        // Aether weather logic: serving expired cache as 'stale' success
        return new Result<>(Status.SUCCESS, data, null);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public boolean hasData() {
        return data != null;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }
}
