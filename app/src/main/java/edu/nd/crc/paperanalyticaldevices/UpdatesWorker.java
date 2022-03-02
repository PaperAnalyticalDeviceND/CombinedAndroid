package edu.nd.crc.paperanalyticaldevices;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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

import edu.nd.crc.paperanalyticaldevices.api.NetworkEntry;
import edu.nd.crc.paperanalyticaldevices.api.ResponseList;
import edu.nd.crc.paperanalyticaldevices.api.WebService;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressCallback;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

public class UpdatesWorker extends Worker implements ProgressCallback {
    private static final int serialVersionUID = 841333472;

    private NotificationManager notificationManager;

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
                .addNetworkInterceptor(new ProgressInterceptor())
                .build();

        final WebService service = WebService.instantiate(client);
        try {

            // First get the project names and neural net names
            Response<ResponseList<String[]>> projectsResp = service.GetProjects("5NWT4K7IS60WMLR3J2LV").execute();
            //load projects into database

            ResponseList<String[]> projectsResult = projectsResp.body();

            ProjectsDbHelper dbHelper = new ProjectsDbHelper(getApplicationContext());

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // delete any previous entries
            dbHelper.clearProjects(db);
            dbHelper.clearNetworks(db);
            dbHelper.clearDrugs(db);

            for(String[] p : projectsResult.Entries){
                //Log.d("API Result", p[0]);

                ContentValues dbValues = new ContentValues();

                dbValues.put(ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTID, p[0]);
                dbValues.put(ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME, p[0]);
                db.insert(ProjectContract.ProjectEntry.TABLE_NAME, null, dbValues);

            }


            //load networks and details into database
            //Response<ResponseList<String[]>> networksResp = service.GetNetworkNames("5NWT4K7IS60WMLR3J2LV").execute();
            //ResponseList<String[]> networksResult = networksResp.body();

            Response<ResponseList<NetworkEntry>> resp = service.GetNetworkInfo("5NWT4K7IS60WMLR3J2LV").execute();
            if (!resp.isSuccessful() || !resp.body().Status.equals("ok")) {
                return Result.failure();
            }

            ResponseList<NetworkEntry> result = resp.body();

            for (NetworkEntry network : result.Entries) {

                //write to the DB first
                ContentValues dbValues = new ContentValues();
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKID, network.Name);
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME, network.Name);
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL, network.Weights);
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_VERSIONSTRING, String.valueOf(network.Version));
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_DESCRIPTION, network.Description);
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_DRUGS, network.Drugs.toString());
                dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_TYPE, network.Type);
                Log.d("UPDATESWORKER", network.Drugs.toString());
                if(network.Weights != "" ){
                    String fileName = URLUtil.guessFileName(String.valueOf(network.Weights), null, null);
                    dbValues.put(NetworksContract.NetworksEntry.COLUMN_NAME_FILENAME, fileName);
                }
                db.insert(NetworksContract.NetworksEntry.TABLE_NAME, null, dbValues);

                for(String drugName : network.Drugs){
                    ContentValues drugValues = new ContentValues();
                    drugValues.put(DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK, network.Name);
                    drugValues.put(DrugsContract.DrugsEntry.COLUMN_NAME_DRUGID, drugName);
                    drugValues.put(DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME, drugName);

                    db.insert(DrugsContract.DrugsEntry.TABLE_NAME, null, drugValues);
                }

            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            for (String projectSet : getInputData().getStringArray("projectkeys")) {
              if(projectSet != ""){

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
                            .tag(ProgressCallback.class, this)
                            .build();

                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            return Result.failure();
                        }

                        String newFileName = URLUtil.guessFileName(String.valueOf(network.Weights), null, null);
                        Log.d("UPDATES_WORKER", "Updating: " + projectSet + "filename to " + newFileName);
                        Log.d("UPDATES_WORKER", "Updating: " + projectSet + "version to " + network.Version.toString());

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
              }// end if project != ""
            }

        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }

    @Override
    public void onProgress(long bytes, long totalBytes) {
        if (totalBytes > 0 && bytes < totalBytes) {
            final int progress = (int) Math.round((((double) bytes / totalBytes) * 100));
            setForegroundAsync(createForegroundInfo(progress, 100));
        }
    }
}
