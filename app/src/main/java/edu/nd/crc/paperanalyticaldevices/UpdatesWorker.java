package edu.nd.crc.paperanalyticaldevices;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.io.ByteStreams;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.vdurmont.semver4j.Semver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.nd.crc.paperanalyticaldevices.api.ProgressResponseBody;
import edu.nd.crc.paperanalyticaldevices.api.NetworkEntry;
import edu.nd.crc.paperanalyticaldevices.api.ResponseList;
import edu.nd.crc.paperanalyticaldevices.api.WebService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

public class UpdatesWorker extends Worker implements ProgressResponseBody.Listener {
    private static final int serialVersionUID = 841333472;

    private NotificationManager notificationManager;
    private String filename;

    public UpdatesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(MainActivity.PROJECT, "Update", NotificationManager.IMPORTANCE_LOW);
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
                .setContentTitle("Downloading neural network")
                .setProgress(max, current, false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(android.R.drawable.ic_delete, "Cancel", intent)
                .build();

        return new ForegroundInfo(serialVersionUID, notification);
    }

    @NonNull
    @Override
    public Result doWork() {
        final OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    okhttp3.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), this))
                            .build();
                })
                .build();

        final WebService service = WebService.instantiate(client);
        try {
            Response<ResponseList<NetworkEntry>> resp = service.GetNetworkInfo("5NWT4K7IS60WMLR3J2LV").execute();
            if (!resp.isSuccessful() || !resp.body().Status.equals("ok")) {
                return Result.failure();
            }

            ResponseList<NetworkEntry> result = resp.body();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            for (String projectSet : getInputData().getStringArray("projectkeys")) {
                Semver currentVersion = new Semver(prefs.getString(projectSet + "version", "0.0"), Semver.SemverType.LOOSE);

                for (NetworkEntry network : result.Entries) {
                    if (!projectSet.equals(network.Name)) {
                        continue;
                    }

                    // Ignore older versions
                    if (!network.Version.isGreaterThan(currentVersion)) {
                        continue;
                    }

                    Log.d("PADS_URL", network.Weights);

                    Request request = new Request.Builder()
                            .url(network.Weights)
                            .build();

                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            return Result.failure();
                        }

                        String newFileName = URLUtil.guessFileName(String.valueOf(network.Weights), null, null);
                        Log.d("UPDATES_WORKER", "Updating: " + projectSet + "filename to " + newFileName);
                        Log.d("UPDATES_WORKER", "Updating: " + projectSet + "version to " + network.Version.toString());
                        filename = newFileName;


                        File newDir = getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE);
                        newDir.mkdirs();

                        try (InputStream input = response.body().byteStream(); OutputStream output = new FileOutputStream(new File(newDir, newFileName))) {
                            ByteStreams.copy(input, output);
                        }

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(projectSet + "filename", newFileName);
                        editor.putString(projectSet + "version", network.Version.toString());
                        editor.apply();
                    }
                }
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }

    @Override
    public void onUpdate(long bytes, long contentLength, boolean done) {
        if (contentLength > 0 && bytes < contentLength) {
            final int progress = (int) Math.round((((double) bytes / contentLength) * 100));
            setForegroundAsync(createForegroundInfo(progress, 100));
        }
    }
}
