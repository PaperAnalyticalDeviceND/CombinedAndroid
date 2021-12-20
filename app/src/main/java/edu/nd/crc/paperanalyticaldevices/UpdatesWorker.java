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
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class UpdatesWorker extends Worker {

    private static final String TAG_WEIGHTS = "weights_url";
    private static final String TAG_NAME = "name";
    private static final String TAG_VERSION = "version";

    private static final String PROGRESS = "PROGRESS";

    private NotificationManager notificationManager;


    public UpdatesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        setProgressAsync(new Data.Builder().putInt(PROGRESS, 0).build());
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(MainActivity.PROJECT, "Upload", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
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
                .setContentTitle("Data Download")
                .setProgress(max, current, false)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(android.R.drawable.ic_delete, "Cancel", intent)
                .build();

        return new ForegroundInfo(hashCode(), notification);
    }

    @NonNull
    @Override
    public Result doWork() {

        int count;
        BufferedReader reader = null;
        HttpURLConnection conn = null;

        Uri.Builder builder = new Uri.Builder();
        //@TODO make this a setting and fetch a unique one from an API on first start
        String api_key = "5NWT4K7IS60WMLR3J2LV";
        builder.scheme("https")
                .authority("pad.crc.nd.edu")
                .appendPath("index.php")
                .appendQueryParameter("option", "com_jbackend")
                .appendQueryParameter("view", "request")
                .appendQueryParameter("module", "querytojson")
                .appendQueryParameter("resource", "list")
                .appendQueryParameter("action", "get")
                .appendQueryParameter("api_key", api_key)
                .appendQueryParameter("queryname", "network_info");

        try {
            URL urlObj = new URL(builder.build().toString());

            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.connect();

            InputStream stream = conn.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();

            String[] projectFolders = getInputData().getStringArray("projectkeys");

            for (String projectSet : projectFolders) {
                String projectVersionString = prefs.getString(projectSet + "version", "0.0");
                double projectVersion = Double.parseDouble(projectVersionString);

                JSONObject jsonObject = new JSONObject(buffer.toString());
                JSONArray listArray = jsonObject.getJSONArray("list");

                for (int i = 0; i < listArray.length(); i++) {
                    JSONObject item = listArray.getJSONObject(i);

                    String projectName = item.getString(TAG_NAME);
                    if (projectName.equals(projectSet)) {

                        String weightsUrl = item.getString(TAG_WEIGHTS);
                        Log.d("PADS_URL", weightsUrl);
                        String versionString = item.getString(TAG_VERSION);
                        double version = Double.parseDouble(versionString);

                        if (version > projectVersion) {
                            // then get updated files and update the shared preferences with new data

                            URL url = new URL(weightsUrl);
                            URLConnection connection = url.openConnection();
                            connection.connect();

                            InputStream input = new BufferedInputStream(url.openStream(), 8192);

                            Context context = getApplicationContext();
                            File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);
                            if (!newDir.exists()) {
                                newDir.mkdirs();
                            }

                            String newFileName = URLUtil.guessFileName(String.valueOf(url), null, null);
                            //store the values in shared preferences
                            editor.putString(projectSet + "filename", newFileName);
                            editor.putString(projectSet + "version", versionString);
                            editor.apply();

                            Log.d("UPDATES_WORKER", "Updating: " + projectSet + "filename to " + newFileName);
                            Log.d("UPDATES_WORKER", "Updating: " + projectSet + "version to " + versionString);

                            File newFile = new File(newDir, newFileName);

                            OutputStream output = new FileOutputStream(newFile);

                            int lengthOfFile = connection.getContentLength();
                            long total = 0;

                            //setForegroundAsync(createForegroundInfo(0, lengthOfFile));
                            setForegroundAsync(createForegroundInfo(0, 100));

                            byte[] data = new byte[1024];
                            while ((count = input.read(data)) != -1) {
                                output.write(data, 0, count);
                                total += count;
                                int prog = Integer.parseInt(String.valueOf( (total * 100) / lengthOfFile) );
                                setProgressAsync(new Data.Builder().putInt(PROGRESS, prog).build());

                                if(prog % 10 == 0) {
                                    setForegroundAsync(createForegroundInfo(prog, 100));
                                }
                            }

                            output.flush();

                            output.close();
                            input.close();

                        }

                    }
                }
            } //for each project name

        } catch (IOException | JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        } finally {

            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }

        }

        setProgressAsync(new Data.Builder().putInt(PROGRESS, 100).build());

        return Result.success();
    }


}
