package edu.nd.crc.paperanalyticaldevices;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
        public String PredictedDrug = "";
        public Optional<String> QRCode = Optional.empty();
        public Optional<String> Timestamp = Optional.empty();
        public String[] Labels = new String[0];
        public Integer PLS = null;
        public Double Probability = null;
        public Integer Concentration = null;
    }

    private final MutableLiveData<Result> resultData = new MutableLiveData<>();

    private String CurrentProject = "";
    private List<TensorflowNetwork> networks = new ArrayList<>();
    private PartialLeastSquares pls = null;

    public Boolean usePls;

    public static long downloadId;
    //private Map<String, String> keyValueMap;

    public PredictionModel(@NonNull Application application) {
        super(application);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());

        LoadModel(preferences, preferences.getString("neuralnet", ""));
        preferences.registerOnSharedPreferenceChangeListener(this);

        //usePls = preferences.getBoolean("pls", false);
        String storedPLSModel = preferences.getString("plsmodel", "none");
        if(!storedPLSModel.toLowerCase().equals("none")){
            usePls = true;
        }else{
            usePls = false;
            pls = null;
        }

        //keyValueMap = new HashMap<String, String>();
    }

    public LiveData<Result> getResult() {
        return resultData;
    }

    public void clearNetworks(){
        networks.clear();
    }

    public void predict(final Intent data) {
        Log.d("ARTIFACTS", "Predict");
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
                        output_string.append(String.format(Locale.US," (%.3f)", results.get(i).Probability));
                        // separate this out from the drug for readability, searchability
                        retVal.PredictedDrug = results.get(i).Label;
                        //retVal.Probability = Double.valueOf(String.format("%.3f", results.get(i).Probability));
                        // This String.format can cause NumberFormatExceptions if the user's phone is in a European locale
                        // so we'll force it to US
                        retVal.Probability = Double.valueOf(String.format(Locale.US, "%.3f", results.get(i).Probability));

                        if (results.size() > 1) {
                            output_string.append(", ");
                        }
                    }
                    if(i == 1){
                        output_string.append("%");
                        List<String> concList = new ArrayList<>(Arrays.asList(MainActivity.concentrations));
                        if(concList.contains(results.get(i).Label)) {
                            retVal.Concentration = Integer.valueOf(results.get(i).Label);
                        }
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
                    output_string.append(", (PLS ").append((int) concentration).append("%)");
                    retVal.PLS =  (int) concentration ;
                }

                retVal.RectifiedImage = Uri.fromFile(rectifiedFile);
                retVal.Predicted = output_string.toString();
                Log.d("ARTIFACTS", "Predicted: " + retVal.PredictedDrug);
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
        // MJC 9-16-24  Download selected PLS files
        if(key.equals("plsmodel")){
            // load pls function goes here
            String plsModel = sharedPreferences.getString("plsmodel", "");
            LoadPLS(sharedPreferences, plsModel);
        }
    }

    public void LoadModelForArtifacts(SharedPreferences sharedPreferences, String network){
        Log.d("ARTIFACTS", "Load model " + network);
        try{
            File networkFile = new File(getApplication().getApplicationContext()
                    .getDir("tflitemodels", Context.MODE_PRIVATE).getPath(),
                    sharedPreferences.getString(network + "filename", "notafile"));
            if(networkFile.exists()){
                MainActivity.setSemaphore(true);
                networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(),
                        sharedPreferences.getString(network + "filename", "")));

                //Boolean usePls = sharedPreferences.getBoolean("pls", false);
                String storedPLSModel = sharedPreferences.getString("plsmodel", "none");
                if(!storedPLSModel.toLowerCase().equals("none") /*&& pls == null*/){
                    pls = PartialLeastSquares.from(getApplication().getApplicationContext());
                }else{
                    pls = null;
                }
            }else{
                DownloadSpecifiedModel(sharedPreferences, network);
            }
        }catch(IOException e){
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }
    }

    public void LoadPLS(SharedPreferences sharedPreferences, String plsModel){
        Log.d("PLS", "Load model " + plsModel);
        if(plsModel.toLowerCase().equals("none")){
            return;
        }
        try{
            File plsFile = new File(getApplication().getApplicationContext()
                    .getDir("tflitemodels", Context.MODE_PRIVATE).getPath(),
                    sharedPreferences.getString(plsModel + "filename", "notafile"));
            if(plsFile.exists()){
                MainActivity.setSemaphore(true);
                pls = PartialLeastSquares.from(getApplication().getApplicationContext());
            }else{
                DownloadSpecifiedModel(sharedPreferences, plsModel);
            }
        }catch(IOException e){
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }
    }

    public void LoadModel(SharedPreferences sharedPreferences, String project){
        networks.clear();
        try {
            String projectFolder;

            String[] selectedNetworks = new String[]{sharedPreferences.getString("neuralnet", ""),
                    sharedPreferences.getString("secondary", "")};
            for(String selected : selectedNetworks){
              //switch (sharedPreferences.getString("neuralnet", "")) {
              // leave the old values for backwards compatibility, but use new values in default case
                switch (selected) {
                    case "":
                        break;

                    default:
                        File networkFile = new File(getApplication().getApplicationContext()
                                .getDir("tflitemodels", Context.MODE_PRIVATE).getPath(),
                                sharedPreferences.getString(selected + "filename", "notafile"));
                        if(selected.toLowerCase() == "none"){
                            MainActivity.setSemaphore(true);
                        }else if(networkFile.exists()){
                            MainActivity.setSemaphore(true);
                            networks.add(TensorflowNetwork.from(getApplication().getApplicationContext(),
                                    sharedPreferences.getString(selected + "filename", "")));

                            //Boolean usePls = sharedPreferences.getBoolean("pls", false);
                            String storedPLSModel = sharedPreferences.getString("plsmodel", "none");
                            if( !storedPLSModel.toLowerCase().equals("none") && storedPLSModel.toLowerCase() != "" /*&& pls == null*/){
                                pls = PartialLeastSquares.from(getApplication().getApplicationContext());
                            }else{
                                pls = null;
                            }
                        }else{
                            //DownloadSpecifiedModel(sharedPreferences, selected);
                            DownloadManagerSpecifiedFile(sharedPreferences, selected);
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

    public void setDownloadId(long downloadId){
        this.downloadId = downloadId;
    }

    public static ArrayList<String> getStoredDownloadIds(SharedPreferences sharedPreferences){
        Gson gson = new Gson();
        ArrayList<String> downloadIds = new ArrayList<>();
        String downloadJson = sharedPreferences.getString("download_ids", "");
        Log.d("PADS Download", "JSON ids " + downloadJson);
        String[] downloadIdStrings = gson.fromJson(downloadJson, String[].class);
        if(downloadIdStrings == null){
            return downloadIds;
        }
        for(String id : downloadIdStrings){
            downloadIds.add(id);
        }

        return downloadIds;
    }

    public static boolean storeDownloadId(SharedPreferences sharedPreferences, long downloadId){
        ArrayList<String> downloadIds = getStoredDownloadIds(sharedPreferences);
        downloadIds.add(String.valueOf(downloadId));
        Gson gson = new Gson();
        String downloadJson = gson.toJson(downloadIds);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("download_ids", downloadJson);
        return editor.commit();
    }

    public static boolean removeDownloadId(SharedPreferences sharedPreferences, long downloadId){
        ArrayList<String> downloadIds = getStoredDownloadIds(sharedPreferences);
        if( !downloadIds.contains( String.valueOf(downloadId) ) ){
            return true;
        }
        downloadIds.remove(String.valueOf(downloadId));
        Gson gson = new Gson();
        String downloadJson = gson.toJson(downloadIds);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("download_ids", downloadJson);
        return editor.commit();
    }

    public void DownloadManagerSpecifiedFile(SharedPreferences sharedPreferences, String networkName){
        Log.d("PADS Download", "DownloadManagerSpecifiedFile " + networkName);
        ProjectsDbHelper dbHelper = new ProjectsDbHelper(getApplication());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<String> networkValues = new ArrayList<>();

        String selection = NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL + " != ''";
        String selectionOrder = NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME + " ASC";

        try( Cursor netCursor = db.query(NetworksContract.NetworksEntry.TABLE_NAME,
                null, selection, null, null, null, selectionOrder)){
            while(netCursor.moveToNext()){
                String model = netCursor.getString(netCursor.getColumnIndexOrThrow(NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME));
                String url = netCursor.getString(netCursor.getColumnIndexOrThrow(NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL));
                String version = netCursor.getString(netCursor.getColumnIndexOrThrow(NetworksContract.NetworksEntry.COLUMN_NAME_VERSIONSTRING));
                if(model.equals(networkName)){
                    // do the download
                    String filename = netCursor.getString(netCursor.getColumnIndexOrThrow(NetworksContract.NetworksEntry.COLUMN_NAME_FILENAME));

                    // check existing downloads to make sure we don't start a duplicate
                    DownloadManager downloadManager = (DownloadManager) getApplication().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    Cursor q = downloadManager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING));

                    if(q == null){}else{
                        while(q.moveToNext()){
                            int index = q.getColumnIndex(DownloadManager.COLUMN_TITLE);
                            if(index != -1){
                                String title = q.getString(index);
                                if(title.equals(filename)){
                                    Log.d("PADs Download", "Download already in progress");
                                    return;
                                }
                            }
                        }
                    }

                    MainActivity.setSemaphore(false);
                    downloadId = DoDownload(url, filename);
                    storeDownloadId(sharedPreferences, downloadId);

                    File nnFolder = getApplication().getApplicationContext().getDir("tflitemodels", Context.MODE_PRIVATE);
                    File newFile = new File(nnFolder, filename);
                    Log.d("PADs Download", "Storing filename " + newFile.getName());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(networkName + "filename", newFile.getName());
                    editor.putString(networkName + "version", version);
                    editor.apply();
                }
            }
        }
    }

    public long DoDownload(String url, String filename){
//        File file = new File(getApplication().getApplicationContext()
//                .getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), filename);
        File file = new File(getApplication().getApplicationContext().getExternalFilesDir(null), filename);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file));

        DownloadManager downloadManager = (DownloadManager) getApplication().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        Log.d("PADS Download", "Enqueue download");
        long dId = downloadManager.enqueue(request);
        Log.d("PADS Download", "DownloadID " + dId);
        return dId;
    }

    public void DownloadSpecifiedModel(SharedPreferences sharedPreferences, String networkName){

        String[] projectFolders = new String[]{networkName};

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        WorkRequest myUploadWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                .addTag("neuralnet_download").setInputData(new Data.Builder()
                        .putStringArray("projectkeys", projectFolders)
                        .build()
                )
                .build();

        Log.d("PredictionModel", "Queueing neuralnet_download worker. " + networkName);
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
