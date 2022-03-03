package edu.nd.crc.paperanalyticaldevices;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.w3c.dom.Text;

import java.io.File;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.UUID;

public class ResultActivity extends AppCompatActivity {
    SharedPreferences mPreferences = null;

    String qr = "";
    String timestamp = "";

    boolean unsafeForConsumption = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Setup Preferences
        mPreferences = getSharedPreferences(MainActivity.PROJECT, MODE_PRIVATE);

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

        }else{
            //sDrugs.setSelection(aDrugs.getPosition(mPreferences.getString("Drug", tDrugs)));
            statedDrug = mPreferences.getString("Drug", tDrugs);
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
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void saveData(View view) {
        Log.i("GB", "Button pushed");

        String compressedNotes = "Predicted drug = ";
        compressedNotes += getBatch();
        compressedNotes += ", ";
        compressedNotes += getNotes();

        if (unsafeForConsumption) {
            // from the Suspected unsafe? toggle button
            compressedNotes += ", Suspected unsafe.  ";
        } else {
            compressedNotes += ", Suspected safe.  ";
        }

        String userName = mPreferences.getString("username", "Unknown");
        // attach stored user's name
        compressedNotes += " User: " + userName + ".  ";

        String neuralnet = mPreferences.getString("neuralnet", "None");
        //attach stored neural net used
        compressedNotes += "Neural net: " + neuralnet + ".  ";

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
                                .putString("NOTES", compressedNotes)
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
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_NOTES, compressedNotes);
        dbValues.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP, timestamp);

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