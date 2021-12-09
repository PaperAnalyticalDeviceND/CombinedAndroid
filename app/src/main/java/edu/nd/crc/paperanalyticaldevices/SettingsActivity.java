package edu.nd.crc.paperanalyticaldevices;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class SettingsActivity extends AppCompatActivity {
    public static final int progress_bar_type = 0;
    // hard coding all these names for now until we have API details
    private static final String baseUrl = "https://pad.crc.nd.edu/neuralnetworks/tf_lite/";
    private static final String subFhiConc = "fhi360_conc_large_lite";
    private static final String subFhi = "fhi360_small_lite";
    private static final String subId = "idPAD_small_lite";
    private static final String subMsh = "msh_tanzania_3k_10_lite";
    private static final String modelVersion = "1.0";
    private static final String idPadName = "idPAD_small_1_6.tflite";
    private static final String fhiName = "fhi360_small_1_21.tflite";
    private static final String fhiConcName = "fhi360_conc_large_1_21.tflite";
    private static final String mshName = "model_small_1_10.tflite";
    private static ProgressBar progressBar;
    private static AsyncTask task;
    private static Button doneButton;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_constraint_layout);
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

        progressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        progressBar.setVisibility(View.INVISIBLE);

        doneButton = (Button) findViewById(R.id.button6);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // set up a listener so that if a new model is selected its expected file path can be checked, and then if not exists it can be created and
        //the file(s) fetched

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("neuralnet")) {

                    String settingValue = sharedPreferences.getString("neuralnet", "");
                    Context context = getBaseContext();

                    String projectFile = sharedPreferences.getString(settingValue + "filename", "");

                    String projectFolder = "";
                    String[] projectFolders = {};

                    switch (settingValue) {
                        case "FHI360-App":
                            projectFolder = subFhi;
                            projectFolders = new String[]{subFhi, subFhiConc};
                            break;
                        case "Veripad idPAD":
                            projectFolder = subId;
                            projectFolders = new String[]{subId};
                            break;
                        case "MSH Tanzania":
                            projectFolder = subMsh;
                            projectFolders = new String[]{subMsh};
                            break;
                        default:
                            return;
                    }

                    new UpdatesAsyncTask().execute(projectFolder);
                }
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view) {
        finish();
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("pad.crc.nd.edu");
            return !address.equals("");
        } catch (UnknownHostException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }
        return false;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    /**
     * Query API for newer versions of neural nets and download, or do fresh download on settings change.
     */
    public class UpdatesAsyncTask extends AsyncTask<String, String, String> {

        //@TODO make this a setting and fetch a unique one from an API on first start
        private final String api_key = "5NWT4K7IS60WMLR3J2LV";

        private final String TAG_LIST = "list";
        private final String TAG_WEIGHTS = "weights_url";
        private final String TAG_NAME = "name";
        private final String TAG_TYPE = "type";
        private final String TAG_DESCRIPTION = "description";
        private final String TAG_VERSION = "version";
        private final String TAG_DRUGS = "drugs";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            doneButton.setClickable(false);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMax(100);
        }

        @Override
        protected String doInBackground(String... project) {
            int count;
            BufferedReader reader = null;
            HttpURLConnection conn = null;

            String[] projectFolders = {project[0]};
            if (project[0].equals(subFhi)) {
                projectFolders = new String[]{project[0], subFhiConc};
            }

            Uri.Builder builder = new Uri.Builder();
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

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();

                for (String projectSet : projectFolders) {

                    String projectVersionString = prefs.getString(projectSet + "version", "0.0");
                    Double projectVersion = Double.parseDouble(projectVersionString);

                    JSONObject jsonObject = new JSONObject(buffer.toString());
                    JSONArray listArray = jsonObject.getJSONArray(TAG_LIST);

                    for (int i = 0; i < listArray.length(); i++) {
                        JSONObject item = listArray.getJSONObject(i);

                        String projectName = item.getString(TAG_NAME);
                        if (projectName.equals(projectSet)) {

                            String weightsUrl = item.getString(TAG_WEIGHTS);
                            Log.d("PADS_URL", weightsUrl);
                            String versionString = item.getString(TAG_VERSION);
                            Double version = Double.parseDouble(versionString);

                            if (version > projectVersion) {
                                // then get updated files and update the shared preferences with new data

                                URL url = new URL(weightsUrl);
                                URLConnection connection = url.openConnection();
                                connection.connect();

                                int lengthOfFile = connection.getContentLength();

                                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                                Context context = getBaseContext();
                                File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);
                                if (!newDir.exists()) {
                                    newDir.mkdirs();
                                }

                                String newFileName = URLUtil.guessFileName(String.valueOf(url), null, null);
                                //store the values in shared preferences
                                editor.putString(projectSet + "filename", newFileName);
                                editor.putString(projectSet + "version", versionString);
                                editor.commit();

                                Log.d("UPDATES_WORKER", "Updating: " + projectSet + "filename to " + newFileName);
                                Log.d("UPDATES_WORKER", "Updating: " + projectSet + "version to " + versionString);

                                File newFile = new File(newDir, newFileName);

                                OutputStream output = new FileOutputStream(newFile);

                                byte[] data = new byte[1024];

                                long total = 0;

                                while ((count = input.read(data)) != -1) {
                                    total += count;

                                    publishProgress(String.valueOf((total * 100) / lengthOfFile));
                                    output.write(data, 0, count);
                                }

                                output.flush();

                                output.close();
                                input.close();

                            }

                        }
                    }
                }
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {

            progressBar.setProgress(Integer.parseInt(progress[0]));

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.INVISIBLE);
            doneButton.setClickable(true);

        }
    }
}