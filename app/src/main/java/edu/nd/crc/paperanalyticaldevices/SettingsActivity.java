package edu.nd.crc.paperanalyticaldevices;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.json.JSONArray;
import org.json.JSONObject;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

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

    private static ProgressBar progressBar;

    private static AsyncTask task;

    private static Button doneButton;

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

        progressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        progressBar.setVisibility(View.INVISIBLE);

        doneButton = (Button) findViewById(R.id.button6);

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
                    //File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);

                    String projectFile = sharedPreferences.getString(settingValue + "filename", "");

                    //if(projectFile.isEmpty() && !isInternetAvailable()){
                        //@TODO  start an activity here asking user to return when internet is available
                    //}

                    String projectFolder = "";
                    String[] projectFolders = {};

                    switch(settingValue){
                        case "FHI360-App":
                            //File fhi = new File(newDir.getPath(), fhiName);
                            //if(!fhi.exists()) {
                                //String url = baseUrl + "/" + subFhi;// + "/" + modelVersion + "/" + fhiName;
                                //task = new DownloadNeuralNet().execute(url);
                            //}
                            //File conc = new File(newDir.getPath(), fhiConcName);
                            //if(!conc.exists()) {
                                //String url2 = baseUrl + "/" + subFhiConc;// + "/" + modelVersion + "/" + fhiConcName;
                                //task = new DownloadNeuralNet().execute(url2);
                            //}
                            projectFolder = subFhi;
                            projectFolders = new String[]{subFhi, subFhiConc};
                            break;
                        case "Veripad idPAD":

                            //File idpad = new File(newDir.getPath(), idPadName);
                            //if(!idpad.exists()) {
                                //String url3 = baseUrl + "/" + subId;// + "/" + modelVersion + "/" + idPadName;
                                //task = new DownloadNeuralNet().execute(url3);
                            //}
                            projectFolder = subId;
                            projectFolders = new String[]{subId};
                            break;
                        case "MSH Tanzania":

                            //File msh = new File(newDir.getPath(), mshName);
                            //if(!msh.exists()) {
                                //String url4 = baseUrl + "/" + subMsh;// + "/" + modelVersion + "/" + mshName;
                                //task = new DownloadNeuralNet().execute(url4);
                            //}
                            projectFolder = subMsh;
                            projectFolders = new String[]{subMsh};
                            break;
                        default:
                            return;
                    }

/*
                    Constraints constraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .build();

                    WorkRequest myUploadWork =  new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                            .addTag("neuralnet_updates").setInputData(new Data.Builder()
                                    .putString("projectkey", projectFolder).putStringArray("projectkeys", projectFolders)
                                    .build()
                            )
                            .build();

                    WorkManager.getInstance(getBaseContext()).enqueue(myUploadWork);
                    */

                    new UpdatesAsyncTask().execute(projectFolder);
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

    public boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager = ( (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public boolean isInternetAvailable(){
        try{
            InetAddress address = InetAddress.getByName("pad.crc.nd.edu");
            return !address.equals("");
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
        return false;
    }

    public class UpdatesAsyncTask extends AsyncTask<String, String, String> {

        //@TODO make this a setting and fetch a unique one from an API on first start
        private String api_key = "5NWT4K7IS60WMLR3J2LV";

        private  String TAG_LIST = "list";
        private  String TAG_WEIGHTS = "weights_url";
        private  String TAG_NAME = "name";
        private  String TAG_TYPE = "type";
        private  String TAG_DESCRIPTION = "description";
        private  String TAG_VERSION = "version";
        private  String TAG_DRUGS = "drugs";

        //private  String subFhiConc = "fhi360_conc_large_lite";
        //private  String subFhi = "fhi360_small_lite";
        //private  String subId = "idPAD_small_lite";
        //private  String subMsh = "msh_tanzania_3k_10_lite";



        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            doneButton.setClickable(false);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMax(100);
        }

        @Override
        protected String doInBackground(String... project){
            int count;
            BufferedReader reader = null;
            HttpURLConnection conn = null;

            String[] projectFolders = {project[0]};
            if(project[0].equals(subFhi)){
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

            try{

                {
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

                    while( (line = reader.readLine()) != null){
                        buffer.append(line + "\n");
                    }

                    //return buffer.toString();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = prefs.edit();


                    for(String projectSet : projectFolders){

                        String projectVersionString = prefs.getString(projectSet + "version", "0.0");
                        Double projectVersion = Double.parseDouble(projectVersionString);


                        //Log.d("PADS_JSON", buffer.toString());
                        JSONObject jsonObject = new JSONObject(buffer.toString());
                        JSONArray listArray = jsonObject.getJSONArray(TAG_LIST);
                        //Log.d("PADS_JSON", jsonObject.toString());

                        for(int i = 0; i < listArray.length(); i++){
                            JSONObject item = listArray.getJSONObject(i);

                            //Log.d("PADS_JSON", "item: " + item.toString());
                            String projectName = item.getString(TAG_NAME);
                            if( projectName.equals(projectSet) /*projectName.equals(fhiConcName) || projectName.equals(fhiName) || projectName.equals(veripadIdName) || projectName.equals(mshTanzaniaName)*/){

                                String weightsUrl = item.getString(TAG_WEIGHTS);
                                Log.d("PADS_URL", weightsUrl);
                                String versionString = item.getString(TAG_VERSION);
                                Double version = Double.parseDouble(versionString);

                                if(version > projectVersion){
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

                                    byte data[] = new byte[1024];

                                    long total = 0;

                                    while ((count = input.read(data)) != -1) {
                                        total += count;

                                        publishProgress(String.valueOf( (total * 100) / lengthOfFile));
                                        output.write(data, 0, count);
                                    }

                                    output.flush();

                                    output.close();
                                    input.close();

                                }

                            }
                        }
                    } //for each project name

                }
            }catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... progress){

            progressBar.setProgress(Integer.parseInt(progress[0]));

        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            progressBar.setVisibility(View.INVISIBLE);
            doneButton.setClickable(true);

        }


    }

/*
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
*/

    //Async class to do tflite model downloads
    /*
    private class DownloadNeuralNet extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //showDialog(progress_bar_type);
            doneButton.setClickable(false);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setMax(100);
        }

        @Override
        protected String doInBackground(String... f_url){
            int count;
            List<String> filesList = new ArrayList<>();

            try{

                //get the preferences so we can store versions and filenames there
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = prefs.edit();

                String projectFolder = f_url[0].substring(f_url[0].lastIndexOf("/") + 1);

                Log.d("ModelFolder", projectFolder);
                editor.putString("ProjectFolder", projectFolder);
                editor.commit();

                String loggedVersionString = prefs.getString(projectFolder + "version", "0.0/");
                Double loggedVersion = Double.parseDouble(loggedVersionString);

                Log.d("ModelVersionLogged", loggedVersion.toString());
                String latestVersionString = getLatestNNVersion(f_url[0]);
                Double latestVersion = Double.parseDouble(latestVersionString);

                Log.d("ModelVersionLatest", latestVersion.toString());

                //if(!latestVersion.equals(loggedVersion)) {
                if(latestVersion > loggedVersion){
                    // get the latest files to download if we don't already have the latest vesion logged for the project
                    filesList = getLatestVersionFiles(f_url[0] + "/" + latestVersion);

                    editor.putString(projectFolder + "version", latestVersion.toString());
                    editor.commit();
                }

                for(String file : filesList) {

                    URL url = new URL(f_url[0] + "/" + latestVersion + file);
                    URLConnection connection = url.openConnection();
                    connection.connect();

                    int lengthOfFile = connection.getContentLength();

                    InputStream input = new BufferedInputStream(url.openStream(), 8192);

                    //String baseDataDir = Environment.getDataDirectory().toString();

                    Context context = getBaseContext();
                    File newDir = context.getDir("tflitemodels", Context.MODE_PRIVATE);
                    if (!newDir.exists()) {
                        newDir.mkdirs();
                    }

                    String newFileName = URLUtil.guessFileName(String.valueOf(url), null, null);
                    //File newFile = new File(newDir, "idPAD_small_1_6.tflite");

                    //keep track of the file name in case they change with new versions
                    editor.putString(projectFolder + "filename", newFileName);
                    editor.commit();

                    File newFile = new File(newDir, newFileName);

                    //String appDataDir = baseDataDir + "/data/" + BuildConfig.APPLICATION_ID + "/app_tflitemodels";
                    //Log.d("SETTINGS", baseDataDir);
                    //OutputStream output = new FileOutputStream(baseDataDir + "/idPAD_small_1_6.tflite");

                    OutputStream output = new FileOutputStream(newFile);

                    byte data[] = new byte[1024];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;

                        //publishProgress("" + (int) ((total * 100) / lengthOfFile));
                        //progressBar.setProgress((int) ((total * 100) / lengthOfFile));
                        publishProgress(String.valueOf( (total * 100) / lengthOfFile));

                        output.write(data, 0, count);
                    }

                    output.flush();

                    output.close();
                    input.close();

                }

            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        private String getLatestNNVersion(String url){
            //parse the root project folder on the web and return highest number sub-folder found
            Double latest = 1.0;

            String latestString = "1.0/";
            try{
                Document doc = Jsoup.connect(url).timeout(0).get();

                doc.select("img").remove();
                Elements links = doc.select("a");

                List<String> listmainlinks = new ArrayList<>();

                for(Element link : links){
                    String linkInnerH = link.html();
                    String linkHref = link.attr("href");
                    //System.out.println("linkHref: "+ linkHref);
                    //System.out.println("linkInnerH: "+ linkInnerH);
                    if(linkInnerH.equals("") ||linkInnerH.equals(" ")||linkInnerH.equals(null) || linkHref.contains("?C=N;O=D")||
                            linkHref.contains("?C=M;O=A")||linkHref.contains("?C=S;O=A") ||linkHref.contains("?C=D;O=A")){ }
                    else if(linkHref.contains("/")){

                        if(!linkInnerH.contains("Parent Directory")) {
                            listmainlinks.add(linkHref);
                            String temp = linkHref.replace("/", "");
                            Double tempDouble = Double.parseDouble(temp);
                            if(tempDouble > latest){
                                latest = tempDouble;
                                latestString = linkHref;
                            }
                        }
                    }else{
                        //listweblinks.add(url + linkHref);
                    }
                }




            }catch(IOException e){
                e.printStackTrace();
            }
            return latestString;

        }

        private List<String> getLatestVersionFiles(String url){

            //parse version directory for tflite files and return a list
            List<String> listmainlinks = new ArrayList<>();

            try{
                Document doc = Jsoup.connect(url).timeout(0).get();

                doc.select("img").remove();
                Elements links = doc.select("a");



                for(Element link : links){
                    String linkInnerH = link.html();
                    String linkHref = link.attr("href");
                    //System.out.println("linkHref: "+ linkHref);
                    //System.out.println("linkInnerH: "+ linkInnerH);
                    if(linkInnerH.equals("") ||linkInnerH.equals(" ")||linkInnerH.equals(null) || linkHref.contains("?C=N;O=D")||
                            linkHref.contains("?C=M;O=A")||linkHref.contains("?C=S;O=A") ||linkHref.contains("?C=D;O=A")){ }
                    else if(linkHref.contains("/")){

                        if(!linkInnerH.contains("Parent Directory")) {
                            //listmainlinks.add(linkHref);

                        }
                    }else{
                        //listweblinks.add(url + linkHref);
                        listmainlinks.add(linkHref);
                    }
                }




            }catch(IOException e){
                e.printStackTrace();
            }

            return listmainlinks;
        }

        protected void onProgressUpdate(String... progress){
            //pDialog.setProgress(Integer.parseInt(progress[0]));
            progressBar.setProgress(Integer.parseInt(progress[0]));
            //if(progressBar.getProgress() >= 100){
                //task.cancel(true);
            //}
        }

        @Override
        protected  void onPostExecute(String f_url){
            //dismissDialog(progress_bar_type);
            progressBar.setVisibility(View.INVISIBLE);
            doneButton.setClickable(true);
        }

    }*/
}