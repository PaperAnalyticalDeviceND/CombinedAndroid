package edu.nd.crc.paperanalyticaldevices;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opencv.android.OpenCVLoader;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SAMPLEID = "e.nd.paddatacapture.EXTRA_SAMPLEID";
    public static final String EXTRA_TIMESTAMP = "e.nd.paddatacapture.EXTRA_TIMESTAMP";
    public static final String EXTRA_PREDICTED = "e.nd.paddatacapture.EXTRA_PREDICTED";
    public static final String EXTRA_LABEL_DRUGS = "e.nd.paddatacapture.EXTRA_LABEL_DRUGS";

    static final String PROJECT = "";

    public static boolean HoldCamera = false;

    private static final String subFhiConc = "fhi360_conc_large_lite";
    private static final String subFhi = "fhi360_small_lite";
    private static final String subId = "idPAD_small_lite";
    private static final String subMsh = "msh_tanzania_3k_10_lite";

    //these filenames should get updated from SharedPreferences if new versions are published
    //the SettingsActivity will check for a newer version when the project setting is changed.
    private String idPadName = "idPAD_small_1_6.tflite";
    private String fhiName = "fhi360_small_1_21.tflite";
    private String fhiConcName = "fhi360_conc_large_1_21.tflite";
    private String mshName = "model_small_1_10.tflite";

    String ProjectName;

    private PredictionModel tensorflowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //initialize opencv
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.i("GBT", "Opencv not loaded");
        }

        //check that a project has been selected, otherwise we can't do anything
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        idPadName = prefs.getString(subId + "filename", idPadName);
        fhiName = prefs.getString(subFhi + "filename", fhiName);
        fhiConcName = prefs.getString(subFhiConc + "filename", fhiConcName);
        mshName = prefs.getString(subMsh + "filename", mshName);

        String project = prefs.getString("neuralnet", "");
        ProjectName = project;

        boolean sync = prefs.getBoolean("sync", false);
        if (sync) {
            checkForUpdates(project);
        }

        // setup remainder
        setContentView(R.layout.activity_main);

        //put in a top toolbar with a menu dropdown
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        tensorflowView = new ViewModelProvider(this).get(PredictionModel.class);
        tensorflowView.getResult().observe(this, new Observer<PredictionModel.Result>() {
            @Override
            public void onChanged(@Nullable PredictionModel.Result result) {
                Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                intent.setData(result.RectifiedImage);
                intent.putExtra(EXTRA_PREDICTED, result.Predicted);
                if (result.QRCode.isPresent()) intent.putExtra(EXTRA_SAMPLEID, result.QRCode.get());
                if (result.Timestamp.isPresent())
                    intent.putExtra(EXTRA_TIMESTAMP, result.Timestamp.get());
                if (result.Labels.length > 0) intent.putExtra(EXTRA_LABEL_DRUGS, result.Labels);
                startActivity(intent);

                HoldCamera = true;
            }
        });
    }

    public void checkForUpdates(String project) {
        String[] projectFolders;

        // check the currently selected project for updated NN files on app start
        if (project.length() > 0) {
            switch (project) {
                case "FHI360-App":

                    projectFolders = new String[]{subFhi, subFhiConc};
                    break;
                case "Veripad idPAD":

                    projectFolders = new String[]{subId};
                    break;
                case "MSH Tanzania":

                    projectFolders = new String[]{subMsh};
                    break;
                default:
                    //12-06-21 allow running without neural net so all projects can be captured
                    return;
            }

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build();

            WorkRequest myUploadWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                    .addTag("neuralnet_updates").setInputData(new Data.Builder()
                            .putStringArray("projectkeys", projectFolders)
                            .build()
                    )
                    .build();

            WorkManager.getInstance(this).enqueue(myUploadWork);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //attach the menu for settings and queue to the app bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.maintoolbarmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //main screen app bar overflow menu
        switch (item.getItemId()) {

            case R.id.app_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.upload_queue:
                Intent iq = new Intent(this, UploadQueueActivity.class);
                startActivity(iq);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void startImageCapture(View view) {
        Log.i("GBR", "Image capture starting");
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                | (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 90);
        } else {

            try {
                ListenableFuture<List<WorkInfo>> workInfoListenable = WorkManager.getInstance(this).getWorkInfosByTag("neuralnet_download");

                // get any download workers
                List<WorkInfo> workinfos = workInfoListenable.get();

                boolean Continue = true;

                //check if they are all finished or not
                for(WorkInfo inf : workinfos){
                    if(!inf.getState().isFinished()){
                        Continue = false;
                        Toast.makeText(getBaseContext(), R.string.pleasewaitdownload, Toast.LENGTH_SHORT).show();
                    }
                }

                // only allow proceeding if all downloads are finished
                if(Continue) {
                    Intent intent = new Intent(this, Camera2Activity.class);
                    startActivityForResult(intent, 10);
                }

            }catch(InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 10) {
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            //tensorflowView.onSharedPreferenceChanged(prefs, "neuralnet");
            tensorflowView.predict(data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
