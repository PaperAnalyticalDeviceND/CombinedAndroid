package edu.nd.crc.paperanalyticaldevices;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PredictionModel extends AndroidViewModel implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String subFhiConc = "fhi360_conc_large_lite";
    private static final String subFhi = "fhi360_small_lite";
    private static final String subId = "idPAD_small_lite";
    private static final String subMsh = "msh_tanzania_3k_10_lite";

    private String idPadName = "idPAD_small_1_6.tflite";
    private String fhiName = "fhi360_small_1_21.tflite";
    private String fhiConcName = "fhi360_conc_large_1_21.tflite";
    private String mshName = "model_small_1_10.tflite";

    public class Result {
        public Uri RectifiedImage;
        public String Predicted = "";
        public Optional<String> QRCode = Optional.empty();
        public Optional<String> Timestamp = Optional.empty();
        public String[] Labels = new String[0];
    }

    private final MutableLiveData<Result> resultData = new MutableLiveData<>();

    private String CurrentProject = "";
    private List<TensorflowNetwork> networks = new ArrayList<>();
    private PartialLeastSquares pls = null;

    private Boolean usePls;

    public PredictionModel(@NonNull Application application) {
        super(application);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());

        LoadModel(preferences, preferences.getString("neuralnet", ""));
        preferences.registerOnSharedPreferenceChangeListener(this);

        usePls = preferences.getBoolean("pls", false);
    }

    public LiveData<Result> getResult() {
        return resultData;
    }

    public void predict(final Intent data) {
        Uri resultStream = data.getData();
        if (resultStream != null) {
            try {
                Result retVal = new Result();

                // Create Output Directory
                String timestamp = "";
                if (data.hasExtra("timestamp")) {
                    timestamp = String.format("%d", data.getExtras().getLong("timestamp"));
                }

                File targetDir = new File(getApplication().getFilesDir(), timestamp);
                targetDir.mkdirs();

                // Extract Files
                UncompressOutputs(getApplication().getContentResolver().openInputStream(resultStream), targetDir);

                // Handle rectified image
                File rectifiedFile = new File(targetDir, "rectified.png");

                // crop input image
                Bitmap bmRect = BitmapFactory.decodeFile(rectifiedFile.getPath());
                Bitmap bm = Bitmap.createBitmap(bmRect, 71, 359, 636, 490);

                // Predict
                List<TensorflowNetwork.Result> results = new ArrayList<>(networks.size());
                for(TensorflowNetwork nn: networks){
                    results.add(nn.infer(bm));
                }

                if (networks.size() > 0 ) retVal.Labels = networks.get(0).Labels.toArray(new String[0]);

                StringBuilder output_string = new StringBuilder();
                for( int i = 0; i < results.size(); i++ ){
                    output_string.append(results.get(i).Label);
                    if (i == 0) {
                        output_string.append(String.format(" (%.3f)", results.get(i).Probability));
                    }

                    if (results.size() > 0) {
                        output_string.append(", ");
                    }
                }


                // calculate concentration from PLSR method
                // get drug if available
                String[] drug = output_string.toString().split(" ", 2);
                String drugStr = "albendazole";
                if (drug.length > 1) {
                    drugStr = drug[0].toLowerCase();
                }

                //if (CurrentProject.equals("FHI360-App") && pls != null) {
                if (usePls && pls != null) {
                    // call
                    double concentration = pls.calculate(bmRect, drugStr);

                    // add conc. result to string
                    output_string.append("%, (PLS ").append((int) concentration).append("%)");
                }

                retVal.RectifiedImage = Uri.fromFile(rectifiedFile);
                retVal.Predicted = output_string.toString();
                if (data.hasExtra("qr")) retVal.QRCode = Optional.of(data.getExtras().getString("qr"));
                if (data.hasExtra("timestamp")) retVal.Timestamp = Optional.of(timestamp);

                resultData.setValue(retVal);
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                e.printStackTrace();
            }
        }
    }

    private static String validateZipEntry(String zipEntryRelativePath, File destDir) throws IOException {
        File zipEntryTarget = new File(destDir, zipEntryRelativePath);
        String zipCanonicalPath = zipEntryTarget.getCanonicalPath();

        if (zipCanonicalPath.startsWith(destDir.getCanonicalPath())) {
            return (zipCanonicalPath);
        }

        throw new IllegalStateException("ZIP entry tried to write outside destination directory");
    }

    private static void UncompressOutputs(InputStream fin, File destDir) throws Exception {
        final int BUFFER_SIZE = 16384;
        final int DEFAULT_MAX_ENTRIES = 1024;
        final int DEFAULT_MAX_SIZE = 1024 * 1024 * 64;

        if (destDir.exists()) {
            if (destDir.list().length > 0) {
                throw new IOException("Your destination directory is not empty!");
            }
        } else {
            destDir.mkdirs();
        }

        try {
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fin))) {
                ZipEntry entry;
                int entries = 0;
                long total = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    int bytesRead;
                    final byte[] data = new byte[BUFFER_SIZE];
                    final String zipCanonicalPath = validateZipEntry(entry.getName(), destDir);

                    if (entry.isDirectory()) {
                        new File(zipCanonicalPath).mkdirs();
                    } else {
                        new File(zipCanonicalPath).getParentFile().mkdirs();

                        final FileOutputStream fos = new FileOutputStream(zipCanonicalPath);
                        final BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);

                        while (total + BUFFER_SIZE <= DEFAULT_MAX_SIZE && (bytesRead = zis.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, bytesRead);
                            total += bytesRead;
                        }

                        dest.flush();
                        fos.getFD().sync();
                        dest.close();

                        if (total + BUFFER_SIZE > DEFAULT_MAX_SIZE) {
                            throw new IllegalStateException("Too much output from ZIP");
                        }
                    }

                    zis.closeEntry();
                    entries++;

                    if (entries > DEFAULT_MAX_ENTRIES) {
                        throw new IllegalStateException("Too many entries in ZIP");
                    }
                }
            }
        } catch (Throwable t) {
            throw new Exception("Problem in unzip operation, rolling back", t);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("neuralnet")) {
            final String project = sharedPreferences.getString("neuralnet", "");
            if(CurrentProject.equals(project)){
                return;
            }

            LoadModel(sharedPreferences, project);
        }
        if(key.equals("secondary")){
            String secondary = sharedPreferences.getString("secondary", "");
            LoadModel(sharedPreferences, secondary);
        }
    }

    public void LoadModel(SharedPreferences sharedPreferences, String project){
        networks.clear();
        try {
            String projectFolder;

            String[] selectedNetworks = new String[]{sharedPreferences.getString("neuralnet", ""), sharedPreferences.getString("secondary", "")};
          for(String selected : selectedNetworks){
              //switch (sharedPreferences.getString("neuralnet", "")) {
              // leave the old values for backwards compatibility, but use new values in default case
            switch (selected) {
                case "Veripad idPAD":
                    //projectFolder = subId;
                    File idpadFile = new File(getApplication().getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), sharedPreferences.getString(subId + "filename", idPadName));
                    if(idpadFile.exists()) {
                        MainActivity.setSemaphore(true);
                        networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(), sharedPreferences.getString(subId + "filename", idPadName)));
                    }else{
                        DownloadModels(sharedPreferences);
                    }
                    break;
                case "MSH Tanzania":
                    //projectFolder = subMsh;
                    File mshFile = new File(getApplication().getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), sharedPreferences.getString(subMsh + "filename", mshName));
                    if(mshFile.exists()) {
                        MainActivity.setSemaphore(true);
                        networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(), sharedPreferences.getString(subMsh + "filename", mshName)));
                    }else{
                        DownloadModels(sharedPreferences);
                    }
                    break;
                //default:
                case "FHI360-App":
                    File fhiFile = new File(getApplication().getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), sharedPreferences.getString(subFhi + "filename", fhiName));
                    if(fhiFile.exists()) {
                        MainActivity.setSemaphore(true);
                        networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(), sharedPreferences.getString(subFhi + "filename", fhiName)));
                        networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(), sharedPreferences.getString(subFhiConc + "filename", fhiConcName)));
                        if (pls == null) {
                            pls = PartialLeastSquares.from(getApplication().getApplicationContext());
                        }
                    }else{
                        DownloadModels(sharedPreferences);
                    }
                    //projectFolder = subFhi;
                    break;

                case "":
                    break;

                default:
                    File networkFile = new File(getApplication().getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), sharedPreferences.getString(selected + "filename", "notafile"));
                    if(networkFile.exists()){
                        MainActivity.setSemaphore(true);
                        networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(), sharedPreferences.getString(selected + "filename", "")));

                        Boolean usePls = sharedPreferences.getBoolean("pls", false);
                        if(usePls && pls == null){
                            pls = PartialLeastSquares.from(getApplication().getApplicationContext());
                        }
                    }else{
                        DownloadSpecifiedModel(sharedPreferences, selected);
                    }

                    break;
            } //end switch case

          } //end for loop


            CurrentProject = project;

            //new UpdatesAsyncTask().execute(projectFolder);


        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();

        }
    }

    public void DownloadSpecifiedModel(SharedPreferences sharedPreferences, String networkName){

        String[] projectFolders;

        switch(networkName){
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
                //return;
                projectFolders = new String[]{networkName};
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        WorkRequest myUploadWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                .addTag("neuralnet_download").setInputData(new Data.Builder()
                        .putStringArray("projectkeys", projectFolders)
                        .build()
                )
                .build();

        Log.d("PredictionModel", "Queueing neuralnet_download worker.");
        WorkManager.getInstance(this.getApplication().getApplicationContext()).enqueue(myUploadWork);
        //bit of a workaround, locks out the camera until the background download worker is finished
        MainActivity.setSemaphore(false);

    }

    public void DownloadModels(SharedPreferences sharedPreferences){

        String[] projectFolders;
        String selected = sharedPreferences.getString("neuralnet", "");
        //switch (sharedPreferences.getString("neuralnet", "")) {
        switch(selected){
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
                //return;
                projectFolders = new String[]{selected};
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        WorkRequest myUploadWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                .addTag("neuralnet_download").setInputData(new Data.Builder()
                        .putStringArray("projectkeys", projectFolders)
                        .build()
                )
                .build();

        Log.d("PredictionModel", "Queueing neuralnet_download worker.");
        WorkManager.getInstance(this.getApplication().getApplicationContext()).enqueue(myUploadWork);
        //bit of a workaround, locks out the camera until the background download worker is finished
        MainActivity.setSemaphore(false);
    }



}
