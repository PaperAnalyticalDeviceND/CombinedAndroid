package edu.nd.crc.paperanalyticaldevices;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class ResultActivity extends AppCompatActivity {

    // for data versioning in the database
    public static final Integer notesVersion = 1;
    public static final Integer buildNumber = 14; // update this to follow the gradle version code

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

        String neuralnet = defaultPrefs.getString("neuralnet", "None");
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
                                .putString("ORIGINAL_IMAGE", FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", new File(new File(getFilesDir(), timestamp), "original.png")).toString())
                                .putString("RECTIFIED_IMAGE", FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", new File(new File(getFilesDir(), timestamp), "rectified.png")).toString())
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
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_IMAGE_CAPTURED, FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", new File(new File(getFilesDir(), timestamp), "original.png")).toString());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_IMAGE_RECTIFIED, FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", new File(new File(getFilesDir(), timestamp), "rectified.png")).toString());
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_PREDICTED_DRUG, getDrug());  // actually expected drug, but I don't want to do another DB migration

        WorkInfoDbHelper dbHelper = new WorkInfoDbHelper(getBaseContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.insert(WorkInfoContract.WorkInfoEntry.TABLE_NAME, null, dbValues);

        Toast.makeText(this, "Results added to upload queue", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(this::finish, 1250);
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