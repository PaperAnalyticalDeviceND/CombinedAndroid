package edu.nd.crc.paperanalyticaldevices;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

import edu.nd.crc.paperanalyticaldevices.api.ArtifactsAPIService;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.ResponseBody;


public class ResultActivity extends AppCompatActivity {

    // for data versioning in the database
    public static final Integer notesVersion = 1;
    public static final Integer buildNumber = 17; // update this to follow the gradle version code

    SharedPreferences mPreferences = null;
    SharedPreferences defaultPrefs = null;

    String qr = "";
    String timestamp = "";
    String predictedDrug = "";
    double probability = 0.000;
    Integer nnConcentration = 0;
    Integer plsConc = 0;
    boolean plsUsed = false;

    boolean unsafeForConsumption = false;

    String neuralnet = "";

    // Artifacts stuff
    String authToken = "";

    Integer taskId = 0;

    String baseUrl = "api-pad.artifactsofresearch.io";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Setup Preferences
        mPreferences = getSharedPreferences(MainActivity.PROJECT, MODE_PRIVATE);
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Setup compatability toolbar
        // make sure the manifest specifies a NoAppBar theme or this will create an exception
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        Switch okToConsumeSwitch = findViewById(R.id.oktoconsumetoggleswitch);
        //set up toggle switch "Suspected unsafe?"
        //send this value to the API in the Notes
        okToConsumeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                unsafeForConsumption = true;
                //Toast.makeText(getBaseContext(), R.string.unsafetoconsume, Toast.LENGTH_SHORT).show();
            } else {
                unsafeForConsumption = false;
                //Toast.makeText(getBaseContext(), R.string.safetoconsume, Toast.LENGTH_SHORT).show();
            }
        });

        // Handle calling intent
        Intent intent = getIntent();

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageURI(intent.getData());
        if (null == intent.getData()) {

            imageView.setImageBitmap(BitmapFactory.decodeStream(getClass().getResourceAsStream("/test42401.png")));
        }

        String sPredicted = "";
        //check exists first to avoid exception when passing it to the array adapter
        if (intent.hasExtra(MainActivity.EXTRA_PREDICTED)) {
            sPredicted = intent.getStringExtra(MainActivity.EXTRA_PREDICTED);
        }
  /*
        Spinner sResult = findViewById(R.id.batchSpinner);
        ArrayAdapter<String> aPredicted = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Collections.singletonList(sPredicted));
        sResult.setEnabled(false);
        sResult.setAdapter(aPredicted);
        sResult.setSelection(aPredicted.getPosition(sPredicted));
*/

        TextView predictedTextView = findViewById(R.id.predicteddrugtext);
        predictedTextView.setText(sPredicted);
        predictedTextView.setTextColor(Color.RED);

        if (intent.hasExtra(MainActivity.EXTRA_SAMPLEID)) {
            qr = intent.getStringExtra(MainActivity.EXTRA_SAMPLEID);
            TextView vSample = findViewById(R.id.idText);
            vSample.setText("PAD ID: " + parseQR(qr));
        }

        if (intent.hasExtra(MainActivity.EXTRA_TIMESTAMP)) {
            timestamp = intent.getStringExtra(MainActivity.EXTRA_TIMESTAMP);
            TextView vTimestamp = findViewById(R.id.timeText);

            Timestamp javaTimestamp = new Timestamp(Long.parseLong(timestamp));
            Date date = new Date(javaTimestamp.getTime());

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            String newDate = sdf.format(date);

            //vTimestamp.setText(this.timestamp);
            vTimestamp.setText(newDate);
        }

        // Handle Drug List
        String tDrugs;
        ArrayAdapter<String> aDrugs;
        if (intent.hasExtra(MainActivity.EXTRA_LABEL_DRUGS) && intent.getStringArrayExtra(MainActivity.EXTRA_LABEL_DRUGS) != null) {
            String[] drugs = intent.getStringArrayExtra(MainActivity.EXTRA_LABEL_DRUGS);
            aDrugs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, drugs);
            tDrugs = drugs[0];
        } else {
            aDrugs = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Defaults.Drugs);
            tDrugs = Defaults.Drugs.get(0);
        }

        // expected drug
        Spinner sDrugs = findViewById(R.id.drugSpinner);
        sDrugs.setAdapter(aDrugs);
        //sDrugs.setSelection(aDrugs.getPosition(mPreferences.getString("Drug", tDrugs)));
        String statedDrug;
        if(intent.hasExtra(MainActivity.EXTRA_STATED_DRUG) && intent.getStringExtra(MainActivity.EXTRA_STATED_DRUG) != null){
            statedDrug = intent.getStringExtra(MainActivity.EXTRA_STATED_DRUG);
            defaultPrefs.edit().putString("Drug", statedDrug).commit();
        }else{
            //sDrugs.setSelection(aDrugs.getPosition(mPreferences.getString("Drug", tDrugs)));
            //statedDrug = mPreferences.getString("Drug", tDrugs);
            statedDrug = defaultPrefs.getString("Drug", tDrugs);
        }
        sDrugs.setSelection(aDrugs.getPosition(statedDrug));

        if(!sPredicted.substring(0, statedDrug.length()).equals(statedDrug) ){
            okToConsumeSwitch.setChecked(true);
            unsafeForConsumption = true;
        }


        // Handle Brands  (Drug concentration)
        Spinner sBrands = findViewById(R.id.brandSpinner);
        ArrayAdapter<String> aBrands = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Defaults.Brands);
        sBrands.setAdapter(aBrands);

        if(intent.hasExtra(MainActivity.EXTRA_STATED_CONC) && intent.getStringExtra(MainActivity.EXTRA_STATED_CONC) != null){
            String statedConc = intent.getStringExtra(MainActivity.EXTRA_STATED_CONC);
            sBrands.setSelection(aBrands.getPosition(statedConc));
        }else {

            sBrands.setSelection(aBrands.getPosition(mPreferences.getString("Brand", Defaults.Brands.get(0))));
        }

        if(intent.hasExtra(MainActivity.EXTRA_NN_CONC) ){
            //nnConcentration = intent.getStringExtra(MainActivity.EXTRA_NN_CONC);
            nnConcentration = intent.getIntExtra(MainActivity.EXTRA_NN_CONC, 0);
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLS_CONC) ){
            //plsConc = intent.getStringExtra(MainActivity.EXTRA_PLS_CONC);
            plsConc = intent.getIntExtra(MainActivity.EXTRA_PLS_CONC, 0);
        }

        if(intent.hasExtra(MainActivity.EXTRA_PREDICTED_DRUG) && intent.getStringExtra(MainActivity.EXTRA_PREDICTED_DRUG) != null){
            predictedDrug = intent.getStringExtra(MainActivity.EXTRA_PREDICTED_DRUG);
        }

        if(intent.hasExtra(MainActivity.EXTRA_PROBABILITY) ){
            //probability = intent.getStringExtra(MainActivity.EXTRA_PROBABILITY);
            probability = intent.getDoubleExtra(MainActivity.EXTRA_PROBABILITY, 0.000);
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLS_USED) ){
            plsUsed = intent.getBooleanExtra(MainActivity.EXTRA_PLS_USED, false);
        }

        if(intent.hasExtra(ArtifactsActivity.Companion.getEXTRA_AUTH_TOKEN())){
            authToken = intent.getStringExtra(ArtifactsActivity.Companion.getEXTRA_AUTH_TOKEN());
        }

        if(intent.hasExtra(ArtifactsActivity.Companion.getEXTRA_BASE_URL())){
            baseUrl = intent.getStringExtra(ArtifactsActivity.Companion.getEXTRA_BASE_URL());
        }

        if(intent.hasExtra(ArtifactsActivity.Companion.getEXTRA_TASK_ID())){
            taskId = intent.getIntExtra(ArtifactsActivity.Companion.getEXTRA_TASK_ID(), 0);
        }

        if(intent.hasExtra(ArtifactsActivity.Companion.getEXTRA_NEURAL_NET())){
            neuralnet = intent.getStringExtra(ArtifactsActivity.Companion.getEXTRA_NEURAL_NET());
        }else{
            neuralnet = defaultPrefs.getString("neuralnet", "None");
        }

        Uri originalFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(new File(getFilesDir(), timestamp), "original.png"));
        Uri rectifiedFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(new File(getFilesDir(), timestamp), "rectified.png"));
        Log.d("ARTIFACTS", originalFileUri.toString());
        Log.d("ARTIFACTS", originalFileUri.getPath());
        Log.d("ARTIFACTS", rectifiedFileUri.toString());
        Log.d("ARTIFACTS", rectifiedFileUri.getPath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.HoldCamera = false;
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
            case R.id.upload_queue:
                Intent iq = new Intent(this, UploadQueueActivity.class);
                startActivity(iq);
                return true;
            case R.id.menu_settings:
                Intent is = new Intent(this, SettingsActivity.class);
                startActivity(is);
                return true;
            case R.id.menu_queue:
                Intent iq2 = new Intent(this, UploadQueueActivity.class);
                startActivity(iq2);
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

    public void saveData(View view) {
        Log.i("GB", "Button pushed");

        HashMap<String, Object> hashMap = new HashMap<>();

        hashMap.put("Notes version", notesVersion);
        hashMap.put("Predicted drug", predictedDrug);
        hashMap.put("Prediction score", probability);
        hashMap.put("Quantity NN", nnConcentration);
        hashMap.put("Quantity PLS", plsConc);
        hashMap.put("PLS used", plsUsed);
        hashMap.put("Notes", getNotes());
        hashMap.put("App type", "Android");
        hashMap.put("Build", buildNumber);

        String compressedNotes = "Predicted drug = ";
        compressedNotes += getBatch();
        compressedNotes += ", ";
        compressedNotes += getNotes();

        if (unsafeForConsumption) {
            // from the Suspected unsafe? toggle button
            compressedNotes += ", Suspected unsafe.  ";
            hashMap.put("Safe", "Suspected unsafe");
        } else {
            compressedNotes += ", Suspected safe.  ";
            hashMap.put("Safe", "Suspected safe");
        }

        String userName = defaultPrefs.getString("username", "Unknown");
        // attach stored user's name
        compressedNotes += " User: " + userName + ".  ";
        hashMap.put("User", userName);

        //neuralnet = defaultPrefs.getString("neuralnet", "None");
        //attach stored neural net used
        compressedNotes += "Neural net: " + neuralnet + ".  ";
        hashMap.put("Neural net", neuralnet);

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        compressedNotes += "Phone ID: " + androidId + ". ";
        hashMap.put("Phone ID", androidId);

        JSONObject jsonNotes = new JSONObject(hashMap);
        String jsonNotesString = jsonNotes.toString();
        Log.d("JSON Output", jsonNotesString);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        Uri originalFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(new File(getFilesDir(), timestamp), "original.png"));
        Uri rectifiedFileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                new File(new File(getFilesDir(), timestamp), "rectified.png"));
        /*Log.d("ARTIFACTS", originalFileUri.toString());
        Log.d("ARTIFACTS", originalFileUri.getPath());
        Log.d("ARTIFACTS", rectifiedFileUri.toString());
        Log.d("ARTIFACTS", rectifiedFileUri.getPath());*/

        WorkRequest myUploadWork = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(constraints)
                .addTag("result_upload")
                .setInputData(
                        new Data.Builder()
                                .putString("SAMPLE_NAME", getDrug())
                                .putString("SAMPLE_ID", parseQR(qr))
                                //.putString("NOTES", compressedNotes)
                                .putString("NOTES", jsonNotesString)
                                .putString("QUANTITY", getPercentage(getBrand()))
                                .putString("TIMESTAMP", timestamp)
                                .putString("ORIGINAL_IMAGE", originalFileUri.toString())
                                .putString("RECTIFIED_IMAGE", rectifiedFileUri.toString())
                                .build()
                )
                .build();

        WorkManager.getInstance(this).enqueue(myUploadWork);
        Log.d("PAD", "Results added to upload queue.");

        UUID workId = myUploadWork.getId();

        Log.d("ENQUEUED WORK", workId.toString());

        //write to a SQLite table so we can get the info out for the Queue activity
        ContentValues dbValues = new ContentValues();
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID, workId.toString());
        //dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME, getDrug());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME, getBatch());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID, parseQR(qr));
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY, getPercentage(getBrand()));
        //dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_NOTES, compressedNotes);
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_NOTES, jsonNotesString);
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP, timestamp);
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_IMAGE_CAPTURED,
                originalFileUri.toString());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_IMAGE_RECTIFIED,
                rectifiedFileUri.toString());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_PREDICTED_DRUG, getDrug());  // actually expected drug, but I don't want to do another DB migration

        WorkInfoDbHelper dbHelper = new WorkInfoDbHelper(getBaseContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.insert(WorkInfoContract.WorkInfoEntry.TABLE_NAME, null, dbValues);

        if(authToken != ""){
            Log.d("ARTIFACTS", "Trying to send artifacts data with token: " + authToken);
            Log.d("ARTIFACTS", "Task ID: " + taskId);
            //ArtifactsAPIService apiService = ArtifactsAPIService.Companion.getInstance(baseUrl);
            TimeZone tz = TimeZone.getTimeZone("UTC");
            Timestamp javaTimestamp = new Timestamp(Long.parseLong(timestamp));
            Date date = new Date(javaTimestamp.getTime());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(tz);
            String newDate = sdf.format(date);
            Log.d("ARTIFACTS", "Datetime: " + newDate);

            // task_notes = "PAD ID: " + padId + "\r\n" + neuralNet + "\r\n" + prediction + "\r\n" + notes
            String prediction = predictedDrug + " (" + probability + ")";
            if(nnConcentration != 0){
                prediction = prediction + ", " + nnConcentration + "%";
            }
            if(plsConc != 0){
                prediction = prediction + ",\r\n(PLS " + plsConc + "%)";
            }
            String taskNotes = "PAD ID: " + parseQR(qr) + "\r\n" + neuralnet + "\r\n" + prediction + "\r\n" + getNotes();

            String result = "positive";
            if(unsafeForConsumption){
                result = "negative";
            }

            File targetDir = new File(getApplication().getFilesDir(), timestamp);
            File rectifiedFile = new File(targetDir, "rectified.png");
            File originalFile = new File(targetDir, "original.png");
            ArtifactsResultViewModel vm = new ViewModelProvider(this).get(ArtifactsResultViewModel.class);
            vm.sendResult(this, authToken, baseUrl, newDate, taskId, taskNotes, result, rectifiedFileUri, originalFileUri, rectifiedFile, originalFile);

            // correct parameters
            /*sendArtifactResult(authToken, baseUrl, newDate, taskId, taskNotes, result,
                    rectifiedFileUri,
                    originalFileUri);*/
        }

        Toast.makeText(this, "Results added to upload queue", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(this::finish, 1550);
    }

    public MultipartBody.Part prepareStringPart(String name, String text){
        return MultipartBody.Part.createFormData(name, text);
    }

    public MultipartBody.Part prepareFilePart(String name, Uri fileUri, File file){
        //File file = new File(fileUri.toString());
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
        return MultipartBody.Part.createFormData(name, file.getName(), requestFile);
    }

    /*public RequestBody makeFileRequest(Uri fileUri, File file){

        return RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
    }

    public RequestBody makeTextRequest(String text){
        return RequestBody.create(MediaType.parse("text/plain"), text);
    }*/

    public void sendArtifactResult(String authToken, String baseUrl, String testDate, Integer taskId, String taskNotes, String result,
                                    Uri rectFileUri, Uri rawFileUri){


        ArtifactsAPIService apiService = ArtifactsAPIService.Companion.getInstance("https://" + baseUrl);
        /*
        val dateField = prepareStringPart("test_date", testDate)
                val taskNotesField = prepareStringPart("task_notes", taskNotes)
                val resultField = prepareStringPart("result", result)
                val rectFileField = prepareFilePart(context, "files", rectFile)
                val rawFileField = prepareFilePart(context, "files", rawFile)
                apiService.sendArtifactsResult(token = "Bearer $authToken", taskId = taskId,
                    rectFile = rectFileField, rawFile = rawFileField, testDate = dateField,
                    taskNotes = taskNotesField, result = resultField)
         */
        //Map<String, RequestBody> map = new HashMap<>();
        File targetDir = new File(getApplication().getFilesDir(), timestamp);
        File rectifiedFile = new File(targetDir, "rectified.png");
        File originalFile = new File(targetDir, "original.png");
        MultipartBody.Part testDatePart = prepareStringPart("test_date", testDate);
        MultipartBody.Part taskNotesPart = prepareStringPart("task_notes", taskNotes);
        MultipartBody.Part resultPart = prepareStringPart("result", result);
        MultipartBody.Part rectFilePart = prepareFilePart("files", rectFileUri, rectifiedFile);
        MultipartBody.Part rawFilePart = prepareFilePart("files", rawFileUri, originalFile);
        // correct parameters
        /*Call<ResponseBody> res = apiService.sendArtifactsResult("Bearer " + authToken,
                taskId,
                rectFilePart,
                rawFilePart,
                testDatePart,
                taskNotesPart,
                resultPart);
        try {
            ResponseBody body = res.execute().body();
            Log.d("ARTIFACTS", body.toString());
        }catch(IOException e){
            e.printStackTrace();
        }*/

        /*RequestBody testDateRequest = makeTextRequest(testDate);
        map.put("test_date", testDateRequest);
        RequestBody taskNotesRequest = makeTextRequest(taskNotes);
        map.put("task_notes", taskNotesRequest);
        RequestBody resultRequest = makeTextRequest(result);
        map.put("result", resultRequest);
        File rect = new File(rectFile.getPath());
        RequestBody rectFileRequest = makeFileRequest(rectFile, rect);
        map.put("name=\"files\"; filename=\"" + rect.getName() + "\"", rectFileRequest);
        File raw = new File(rawFile.getPath());
        RequestBody rawRequest = makeFileRequest(rawFile, raw);
        map.put("name=\"files\"; filename=\"" + raw.getName() + "\"", rawRequest);

        ResponseBody res = apiService.sendArtifactsResult("Bearer " + authToken, taskId, map);
        */
        //Log.d("ARTIFACTS", res.toString());

    }

    public void discardData(View view) {
        finish();
    }

    private String getDrug() {
        Spinner spinner = findViewById(R.id.drugSpinner);
        String ret = String.valueOf(spinner.getSelectedItem());
        mPreferences.edit().putString("Drug", ret).apply();
        return ret;
    }

    private String getBrand() {
        Spinner spinner = findViewById(R.id.brandSpinner);
        String ret = String.valueOf(spinner.getSelectedItem());
        if (ret.isEmpty()) {
            return "100%";
        }
        mPreferences.edit().putString("Brand", ret).apply();
        return ret;
    }

    private String getPercentage(String raw) {
        return raw.substring(0, raw.length() - 1);
    }

    private String getBatch() {
        /*
        Spinner spinner = findViewById(R.id.batchSpinner);
        String ret = String.valueOf(spinner.getSelectedItem());
        */
        TextView predictedText = findViewById(R.id.predicteddrugtext);
        String ret = predictedText.getText().toString();
        if (ret.isEmpty()) {
            ret = "n/a";
        }
        return ret.toLowerCase();
    }

    private String getNotes() {
        EditText editText = findViewById(R.id.editText);
        return String.valueOf(editText.getText());
    }

    public String parseQR(String qr) {
        if (qr.startsWith("padproject.nd.edu/?s=") || qr.startsWith("padproject.nd.edu/?t=")) {
            return qr.substring(21);
        }
        return qr;
    }
}