package edu.nd.crc.paperanalyticaldevices;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
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

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SAMPLEID = "e.nd.paddatacapture.EXTRA_SAMPLEID";
    public static final String EXTRA_TIMESTAMP = "e.nd.paddatacapture.EXTRA_TIMESTAMP";
    public static final String EXTRA_PREDICTED = "e.nd.paddatacapture.EXTRA_PREDICTED";
    public static final String EXTRA_LABEL_DRUGS = "e.nd.paddatacapture.EXTRA_LABEL_DRUGS";
    public static final String EXTRA_STATED_DRUG = "e.nd.paddatacapture.EXTRA_STATED_DRUG";
    public static final String EXTRA_STATED_CONC = "e.nd.paddatacapture.EXTRA_STATED_CONC";
    public static final String EXTRA_PREDICTED_DRUG = "e.nd.paddatacapture.EXTRA_PREDICTED_DRUG";
    public static final String EXTRA_PLS_CONC = "e.nd.paddatacapture.EXTRA_PLS_CONC";
    public static final String EXTRA_NN_CONC = "e.nd.paddatacapture.EXTRA_NN_CONC";
    public static final String EXTRA_PROBABILITY = "e.nd.paddatacapture.EXTRA_PROBABILITY";
    public static final String EXTRA_PLS_USED = "e.nd.paddatacapture.EXTRA_PLS_USED";

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

    String neuralNetName;
    String secondaryNeuralNetName;

    static final String[] concentrations = new String[]{"100", "80", "50", "20"};
    static final String[] concentraionStrings = new String[]{"100%", "80%", "50%", "20%"};

    private PredictionModel tensorflowView;

    public static boolean workerSemaphore;

    public static void setSemaphore(boolean val){
        workerSemaphore = val;
    }

    ProjectsDbHelper dbHelper;
    SQLiteDatabase db;

    TextView networkLabel;
    TextView projectLabel;

    NumberPicker sDrugs;
    //NumberPicker sConc;

    int concIndex;

    private final OnSliderTouchListener touchListener =
            new OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {
                    //showSnackbar(slider, R.string.cat_slider_start_touch_description);
                }

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    //showSnackbar(slider, R.string.cat_slider_stop_touch_description);
                    //concIndex = Integer.valueOf(String.valueOf(slider.getValue()));
                    concIndex = Math.round(slider.getValue());
                }
            };

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

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("DROID ID", androidId);

        //check that a project has been selected, otherwise we can't do anything
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        idPadName = prefs.getString(subId + "filename", idPadName);
        fhiName = prefs.getString(subFhi + "filename", fhiName);
        fhiConcName = prefs.getString(subFhiConc + "filename", fhiConcName);
        mshName = prefs.getString(subMsh + "filename", mshName);

        String project = prefs.getString("neuralnet", "");
        ProjectName = project;

        boolean sync = prefs.getBoolean("sync", true);
        //default to true to make sure this runs on first start
        if (sync) {
            checkForUpdates(project);
        }

        // setup remainder
        setContentView(R.layout.activity_main);

        //put in a top toolbar with a menu dropdown
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        //get drug labels stored for primary neural net

        dbHelper = new ProjectsDbHelper(this);
        db = dbHelper.getReadableDatabase();
        setDrugSpinnerItems();

/*
        Spinner sDrugs = findViewById(R.id.statedDrugSpinner);
        String[] drugsArray = drugEntries.toArray(new String[drugEntries.size()]);
        ArrayAdapter<String> aDrugs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, drugsArray);
        sDrugs.setAdapter(aDrugs);

        // prepare picker for drug %
        ArrayAdapter<String> aConcentrations = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Defaults.Brands);
        Spinner sConc = findViewById(R.id.concDrugSpinner);
        sConc.setAdapter(aConcentrations);
*/

        Slider sConc = findViewById(R.id.concDrugSpinner);
        //NumberPicker sConc = findViewById(R.id.concDrugSpinner);
        //sConc.setMinValue(0);
        //sConc.setMaxValue(Defaults.Brands.size() - 1);
        //sConc.setDisplayedValues(Defaults.Brands.toArray(new String[Defaults.Brands.size()]));
        LabelFormatter formatter = new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                //do what you want with the value in here...
                int index = Math.round(value);
                return concentraionStrings[index];
            }
        };

        sConc.setLabelFormatter(formatter);
        sConc.addOnSliderTouchListener(touchListener);


        networkLabel = findViewById(R.id.neuralnet_name_view);
        networkLabel.setText(project);

        projectLabel = findViewById(R.id.project_name_view);
        String projectName = prefs.getString("project", "");
        projectLabel.setText(projectName);

        workerSemaphore = true;

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

                intent.putExtra(EXTRA_NN_CONC, result.Concentration);
                Log.d("Concentration", String.valueOf(result.Concentration) );
                intent.putExtra(EXTRA_PREDICTED_DRUG, result.PredictedDrug);
                Log.d("PredictedDrug", result.PredictedDrug);
                intent.putExtra(EXTRA_PLS_CONC, result.PLS);
                Log.d("PLS", String.valueOf(result.PLS) );
                intent.putExtra(EXTRA_PROBABILITY, result.Probability);
                Log.d("Probability", String.valueOf(result.Probability) );
                if(tensorflowView.usePls){
                    intent.putExtra(EXTRA_PLS_USED, true);
                }else{
                    intent.putExtra(EXTRA_PLS_USED, false);
                }
                /*
                Spinner spinnerDrugs = findViewById(R.id.statedDrugSpinner);
                String ret = String.valueOf(spinnerDrugs.getSelectedItem());
                intent.putExtra(EXTRA_STATED_DRUG, ret);

                Spinner spinnerConc = findViewById(R.id.concDrugSpinner);
                String conc = String.valueOf(spinnerConc.getSelectedItem());
                intent.putExtra(EXTRA_STATED_CONC, conc);

                */

                // get the selected drug and concentration to pass on to the result activity
                NumberPicker spinnerDrugs = findViewById(R.id.statedDrugSpinner);
                String[] drugList = spinnerDrugs.getDisplayedValues();
                int drugIndex = spinnerDrugs.getValue();
                String ret = drugList[drugIndex];
                intent.putExtra(EXTRA_STATED_DRUG, ret);

                /*NumberPicker spinnerConc = findViewById(R.id.concDrugSpinner);
                String[] conList = spinnerConc.getDisplayedValues();
                int concIndex = spinnerConc.getValue();
                String conc = conList[concIndex];
                intent.putExtra(EXTRA_STATED_CONC, conc);*/
                String conc = concentraionStrings[concIndex];
                intent.putExtra(EXTRA_STATED_CONC, conc);

                startActivity(intent);

                HoldCamera = true;
            }
        });


    }

    private void setDrugSpinnerItems(){

        ArrayList<String> drugEntries = new ArrayList<>();
        String[] projection = {
                BaseColumns._ID,
                DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME,
        };

        String selection = DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK + " = ?";
        String[] selectionArgs = {ProjectName};
        String sortOrder = DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME + " ASC";

        String drugName;
        try( Cursor cursor = db.query(DrugsContract.DrugsEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)) {
            while(cursor.moveToNext()){

                drugName = cursor.getString(cursor.getColumnIndexOrThrow(DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME));
                drugEntries.add(drugName);
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String statedDrug = prefs.getString("Drug", "unknown");


        String[] drugsArray = drugEntries.toArray(new String[drugEntries.size()]);
        //int pos = drugsArray.getPosition(statedDrug);
        sDrugs = findViewById(R.id.statedDrugSpinner);
        sDrugs.setDisplayedValues(null);
        sDrugs.setMinValue(0);
        sDrugs.setValue(0);

        if(drugsArray.length > 0){
            sDrugs.setMaxValue(drugEntries.size() - 1);
            sDrugs.setDisplayedValues(drugsArray);
        }else{
            // use defaults to ensure something is in the picker
            sDrugs.setMaxValue(Defaults.Drugs.size() - 1);
            sDrugs.setDisplayedValues(Defaults.Drugs.toArray(new String[Defaults.Drugs.size()]));
        }
        //restore previous selection
        for(int i=0; i < drugsArray.length; i++){
            if(drugsArray[i].contains(statedDrug)){
                sDrugs.setValue(i);
            }
        }
    }

    //New updates function to match iOS version functionality
    public void newCheckForUpdates(){

        String[] nets;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // these will be the exact names coming from the "networks" API
        neuralNetName = prefs.getString("neuralNet", "");
        secondaryNeuralNetName = prefs.getString("secondarynet", "");

        nets = new String[]{neuralNetName, secondaryNeuralNetName};

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        WorkRequest myUpdateWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                .addTag("neuralnet_updates").setInputData(new Data.Builder()
                        .putStringArray("projectkeys", nets)
                        .build()
                )
                .build();

        WorkManager.getInstance(this).enqueue(myUpdateWork);
    }

    private String[] getNetworkNames(){

        String[] nets;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // these will be the exact names coming from the "networks" API
        neuralNetName = prefs.getString("neuralNet", "");
        secondaryNeuralNetName = prefs.getString("secondarynet", "");

        nets = new String[]{neuralNetName, secondaryNeuralNetName};

        return nets;
    }

    public void checkForUpdates(String project) {
        String[] projectFolders;

        // check the currently selected project for updated NN files on app start
        //if (project.length() > 0) {
            //Keep this for backwards compatibility, but change to presenting the network names in the settings to
            // match the iOS version and simplify
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
                    //03-02-22 add secondary net setting and change stored value to actual network name
                    projectFolders = getNetworkNames();
                    break;
                    //return;
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
        //}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //attach the menu for settings and queue to the app bar
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.maintoolbarmenu, menu);
        inflater.inflate(R.menu.iconmenutoolbar, menu);
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
            case R.id.menu_settings:
                Intent is = new Intent(this, SettingsActivity.class);
                startActivity(is);
                return true;
            case R.id.menu_queue:
                Intent iq2 = new Intent(this, UploadQueueActivity.class);
                startActivity(iq2);
                return true;
            case R.id.upload_queue:
                Intent iq = new Intent(this, UploadQueueActivity.class);
                startActivity(iq);
                return true;
            case R.id.menu_about:
                Intent a = new Intent(this, AboutActivity.class);
                startActivity(a);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void startImageCapture(View view) {
        Log.i("GBR", "Image capture starting");

        // Android 13 does not use these permissions and will not prompt for them
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2 &&
                ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                | (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)))
        {
            Log.d("GBR", "Request camera permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 90);
        } else {

            try {
  /*
                ListenableFuture<List<WorkInfo>> workInfoListenable = WorkManager.getInstance(getApplicationContext()).getWorkInfosByTag("neuralnet_download");

                // get any download workers
                List<WorkInfo> workinfos = workInfoListenable.get();

                boolean Continue = true;

                //check if they are all finished or not
                for(WorkInfo inf : workinfos){
                    if(!inf.getState().isFinished()){
                        Continue = false;

                    }
                }
*/
                // only allow proceeding if all downloads are finished
                if(workerSemaphore) {
                    Log.d("GBR", "Trying to start Camera");
                    Intent intent = new Intent(this, Camera2Activity.class);
                    startActivityForResult(intent, 10);
                }else{
                    Log.d("GBR", "Not starting Camera");
                    Toast.makeText(getBaseContext(), R.string.pleasewaitdownload, Toast.LENGTH_LONG).show();
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ProjectName = prefs.getString("neuralnet", "");

        networkLabel = findViewById(R.id.neuralnet_name_view);
        networkLabel.setText(ProjectName);

        projectLabel = findViewById(R.id.project_name_view);
        String project = prefs.getString("project", "");
        projectLabel.setText(project);

        setDrugSpinnerItems();

        WorkManager manager = WorkManager.getInstance(getApplicationContext());
        LiveData<List<WorkInfo>> workInfos = manager.getWorkInfosByTagLiveData("neuralnet_download");

        workInfos.observe(this, listOfWorkInfo -> {

            //Log.d("WORKER_OBSERVER", "In Observer, onResume.");
            if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
                //Log.d("WORKER_OBSERVER", "No workers found.");
                workerSemaphore = true;
                return;
            }

            boolean foundBusy = false;

            for (WorkInfo workInfo : listOfWorkInfo) {
                if (!workInfo.getState().isFinished()) {
                    foundBusy = true;
                    //Log.d("WORKER_OBSERVER", "Found busy worker.");
                    workerSemaphore = false;
                }
            }

            if(foundBusy){
                workerSemaphore = false;
            }else{
                workerSemaphore = true;
                //Log.d("WORKER_OBSERVER", "Setting semaphore true.");
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 10) {
            // this is just here to make sure it calls LoadModels if we've just downloaded a new NN file
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            //tensorflowView.onSharedPreferenceChanged(prefs, "neuralnet");
            String proj = prefs.getString("neuralnet", "");
            tensorflowView.LoadModel(prefs, proj);
            tensorflowView.predict(data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
