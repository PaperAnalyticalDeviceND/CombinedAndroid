package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UpdatesWorker extends Worker {

    private static String TAG_LIST = "list";
    private static String TAG_WEIGHTS = "weights_url";
    private static String TAG_NAME = "name";
    private static String TAG_TYPE = "type";
    private static String TAG_DESCRIPTION = "description";
    private static String TAG_VERSION = "version";
    private static String TAG_DRUGS = "drugs";

    private static String fhiConcName = "fhi360_conc_large_lite";
    private static String fhiName = "fhi360_small_lite";
    private static String veripadIdName = "idPAD_small_lite";
    private static String mshTanzaniaName = "msh_tanzania_3k_10_lite";

    //@TODO make this a setting and fetch a unique one from an API on first start
    private String api_key = "5NWT4K7IS60WMLR3J2LV";

    public UpdatesWorker(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        int count;
        BufferedReader reader = null;
        HttpURLConnection conn = null;

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

            String[] projectFolders = getInputData().getStringArray("projectkeys");

            for(String projectSet : projectFolders){

                //String projectSet = getInputData().getString("projectkey");
                //String projectSet = prefs.getString("projectkey", "");
                String projectVersionString = prefs.getString(projectSet + "version", "0.0");
                Double projectVersion = Double.parseDouble(projectVersionString);


                //Log.d("PADS_JSON", buffer.toString());
                JSONObject jsonObject = new JSONObject(buffer.toString());
                JSONArray listArray = jsonObject.getJSONArray("list");
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

                            //int lengthOfFile = connection.getContentLength();

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
                            editor.commit();

                            Log.d("UPDATES_WORKER", "Updating: " + projectSet + "filename to " + newFileName);
                            Log.d("UPDATES_WORKER", "Updating: " + projectSet + "version to " + versionString);

                            File newFile = new File(newDir, newFileName);

                            OutputStream output = new FileOutputStream(newFile);

                            byte data[] = new byte[1024];

                            long total = 0;

                            while ((count = input.read(data)) != -1) {
                                total += count;

                                output.write(data, 0, count);
                            }

                            output.flush();

                            output.close();
                            input.close();

                        }

                    }
                }
            } //for each project name

        }catch(MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e) {
            e.printStackTrace();
        }catch(JSONException e){
            e.printStackTrace();
        }finally {

            if(conn != null){
                conn.disconnect();
            }
            try{
                if(reader != null){
                    reader.close();
                }
            }catch(IOException e2){
                e2.printStackTrace();
            }

        }


        return Result.success();
    }



}
