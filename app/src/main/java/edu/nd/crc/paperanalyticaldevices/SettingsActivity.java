package edu.nd.crc.paperanalyticaldevices;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class SettingsActivity extends AppCompatActivity {

    // hard coding all these names for now until we have API details
    private static String baseUrl = "https://pad.crc.nd.edu/neuralnetworks/tf_lite/";
    //private static String fileUrl = "https://pad.crc.nd.edu/neuralnetworks/tf_lite/idPAD_small_lite/1.0/idPAD_small_1_6.tflite";

    private static String subFhiConc = "fhi360_conc_large_lite";
    private static String subFhi = "fhi360_small_lite";
    private static String subId = "idPAD_small_lite";
    private static String subMsh = "msh_tanzania_3k_10_lite";

    private static String modelVersion = "1.0";

    private static String idPadName = "idPAD_small_1_6.tflite";
    private static String fhiName = "fhi360_small_1_21.tflite";
    private static String fhiConcName = "fhi360_conc_large_1_21.tflite";
    private static String mshName = "model_small_1_10.tflite";

    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_constraint_layout);
        //setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setDisplayHomeAsUpEnabled(true);
            // remove this so we can use this activity from anywhere
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // set up a listener so that if a new model is selected its expected file path can be checked, and then if not exists it can be created and
        //the file(s) fetched

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("neuralnet") ){

                    //new DownloadNeuralNet().execute(fileUrl);

                    String settingValue = sharedPreferences.getString("neuralnet", "");
                    Context context = getBaseContext();
                    File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);

                    switch(settingValue){
                        case "fhi360":
                            File fhi = new File(newDir.getPath(), fhiName);
                            if(!fhi.exists()) {
                                String url = baseUrl + "/" + subFhi + "/" + modelVersion + "/" + fhiName;
                                new DownloadNeuralNet().execute(url);
                            }
                            File conc = new File(newDir.getPath(), fhiConcName);
                            if(!conc.exists()) {
                                String url = baseUrl + "/" + subFhiConc + "/" + modelVersion + "/" + fhiConcName;
                                new DownloadNeuralNet().execute(url);
                            }
                            break;
                        case "idpad":

                            File idpad = new File(newDir.getPath(), idPadName);
                            if(!idpad.exists()) {
                                String url = baseUrl + "/" + subId + "/" + modelVersion + "/" + idPadName;
                                new DownloadNeuralNet().execute(url);
                            }
                            break;
                        case "mshtanzania":

                            File msh = new File(newDir.getPath(), mshName);
                            if(!msh.exists()) {
                                String url = baseUrl + "/" + subMsh + "/" + modelVersion + "/" + mshName;
                                new DownloadNeuralNet().execute(url);
                            }
                            break;
                        default:
                            return;
                    }

                }
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view){
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }


    //Async class to do tflite model downloads
    class DownloadNeuralNet extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        @Override
        protected String doInBackground(String... f_url){
            int count;

            try{
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                int lengthOfFile = connection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                //String baseDataDir = Environment.getDataDirectory().toString();

                Context context = getBaseContext();
                File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);
                if(!newDir.exists()){
                    newDir.mkdirs();
                }

                String newFileName = URLUtil.guessFileName(String.valueOf(url), null, null);
                //File newFile = new File(newDir, "idPAD_small_1_6.tflite");

                File newFile = new File(newDir, newFileName);

                //String appDataDir = baseDataDir + "/data/" + BuildConfig.APPLICATION_ID + "/app_tflitemodels";
                //Log.d("SETTINGS", baseDataDir);
                //OutputStream output = new FileOutputStream(baseDataDir + "/idPAD_small_1_6.tflite");

                OutputStream output = new FileOutputStream(newFile);

                byte data[] = new byte[1024];

                long total = 0;

                while((count = input.read(data)) != -1){
                    total += count;

                    publishProgress("" + (int) ((total * 100) / lengthOfFile));

                    output.write(data, 0, count);
                }

                output.flush();

                output.close();
                input.close();



            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... progress){
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected  void onPostExecute(String f_url){
            dismissDialog(progress_bar_type);
        }

    }
}