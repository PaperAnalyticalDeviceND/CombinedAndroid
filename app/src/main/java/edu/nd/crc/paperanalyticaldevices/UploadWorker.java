package edu.nd.crc.paperanalyticaldevices;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import edu.nd.crc.paperanalyticaldevices.api.WebService;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressCallback;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Response;

public class UploadWorker extends Worker implements ProgressCallback {
    private static final int serialVersionUID = 841333473;

    private final NotificationManager notificationManager;
    private final FirebaseAnalytics mFirebaseAnalytics;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(MainActivity.PROJECT, "Upload", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(int current, int max) {
        PendingIntent intent = WorkManager.getInstance(getApplicationContext()).createCancelPendingIntent(getId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), MainActivity.PROJECT)
                .setOngoing(true)
                .setContentTitle("Uploading Result")
                .setProgress(max, current, false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(android.R.drawable.ic_delete, "Cancel", intent)
                .build();

        return new ForegroundInfo(serialVersionUID, notification);
    }

    private void LogEvent(final Map<String, String> data) {
        Bundle bundle = new Bundle();
        data.forEach((key, value) -> bundle.putString(key, value));
        mFirebaseAnalytics.logEvent("image_upload", bundle);
    }

    @NotNull
    @Override
    public Result doWork() {
        UploadData data = UploadData.from(getInputData(), getApplicationContext());
        if (!data.isValid()) {
            return Result.failure();
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new ProgressInterceptor())
                .build();

        final WebService service = WebService.instantiate(client);
        try {
            final Map<String, String> parameters = UploadData.asMap(getInputData(), getApplicationContext());
            if (parameters.containsValue(null)) {
                return Result.failure();
            }
            LogEvent(parameters);

            Response<JsonObject> resp = service.UploadResult(parameters, this).execute();
            if (!resp.isSuccessful() || resp.body().has("status") && resp.body().get("status").getAsString().equals("ko")) {
                return Result.failure();
            }

            data.CleanupImages();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.failure();
        }

        return Result.success();
    }

    @Override
    public void onProgress(long bytes, long contentLength) {
        if (contentLength > 0 && bytes < contentLength) {
            final int progress = (int) Math.round((((double) bytes / contentLength) * 100));
            setForegroundAsync(createForegroundInfo(progress, 100));
        }
    }
}
