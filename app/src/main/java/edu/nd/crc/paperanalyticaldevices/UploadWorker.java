package edu.nd.crc.paperanalyticaldevices;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UploadWorker extends Worker {
    private NotificationManager notificationManager;
    private FirebaseAnalytics mFirebaseAnalytics;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public Result doWork() {
        UploadData data = UploadData.from(getInputData(), this.getApplicationContext());
        if(!data.isValid()) {
            return Result.failure();
        }

        mFirebaseAnalytics.logEvent("image_upload", data.toBundle());

        String formData;
        try {
            formData = data.toUrlEncoded();
        }catch(IOException|NoSuchAlgorithmException e){
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.failure();
        }

        // Update notification
        setForegroundAsync(createForegroundInfo(0, formData.length()));

        // Build Request
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("pad.crc.nd.edu")
                .appendPath("index.php")
                .appendQueryParameter("option", "com_jbackend")
                .appendQueryParameter("view", "request")
                .appendQueryParameter("module", "querytojson")
                .appendQueryParameter("resource", "upload")
                .appendQueryParameter("action", "post");

        // Send Data
        try{
            URL urlObj = new URL(builder.build().toString());

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.connect();

            BufferedOutputStream bof = new BufferedOutputStream(conn.getOutputStream());
            InputStream sData = new ByteArrayInputStream(formData.getBytes());

            byte[] buffer = new byte[8096];

            int totalRead = 0;

            int bytesRead;
            while ((bytesRead = sData.read(buffer)) != -1) {
                bof.write(buffer, 0, bytesRead);
                bof.flush();
                totalRead += bytesRead;
                setForegroundAsync(createForegroundInfo(totalRead, formData.length()));
            }
            bof.close();
            sData.close();

            InputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.failure();
        }

        // Cleanup images as they are extracted in the main activity again
        data.CleanupImages();

        // Update the notification
        setForegroundAsync(createForegroundInfo(0, 0));

        // Mark as succeeded
        return Result.success();
    }


    @NonNull
    private ForegroundInfo createForegroundInfo(int current, int max) {
        Context context = getApplicationContext();

        PendingIntent intent = WorkManager.getInstance(context).createCancelPendingIntent(getId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, MainActivity.PROJECT)
                .setOngoing(true)
                .setContentTitle("Data Upload")
                .setProgress(max, current, false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(android.R.drawable.ic_delete, "Cancel", intent)
                .build();

        return new ForegroundInfo(this.hashCode(), notification);
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(MainActivity.PROJECT, "Upload", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }
}
