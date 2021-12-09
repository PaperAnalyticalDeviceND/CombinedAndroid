    package edu.nd.crc.paperanalyticaldevices;

    import android.Manifest;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.net.Uri;
    import android.os.Bundle;
    import android.os.Handler;
    import android.util.Log;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.widget.ProgressBar;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.Toolbar;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import androidx.preference.PreferenceManager;
    import androidx.work.Constraints;
    import androidx.work.Data;
    import androidx.work.NetworkType;
    import androidx.work.OneTimeWorkRequest;
    import androidx.work.WorkManager;
    import androidx.work.WorkRequest;

    import com.google.firebase.FirebaseApp;
    import com.google.firebase.analytics.FirebaseAnalytics;
    import com.google.firebase.crashlytics.FirebaseCrashlytics;

    import org.opencv.android.CameraBridgeViewBase;
    import org.opencv.android.OpenCVLoader;
    import org.tensorflow.lite.DataType;
    import org.tensorflow.lite.Interpreter;
    import org.tensorflow.lite.support.common.FileUtil;
    import org.tensorflow.lite.support.image.ImageProcessor;
    import org.tensorflow.lite.support.image.TensorImage;
    import org.tensorflow.lite.support.image.ops.ResizeOp;
    import org.tensorflow.lite.support.metadata.MetadataExtractor;
    import org.tensorflow.lite.support.metadata.schema.ModelMetadata;
    import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

    import java.io.BufferedInputStream;
    import java.io.BufferedOutputStream;
    import java.io.BufferedReader;
    import java.io.File;
    import java.io.FileInputStream;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.nio.MappedByteBuffer;
    import java.nio.channels.FileChannel;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.zip.ZipEntry;
    import java.util.zip.ZipInputStream;

    public class MainActivity extends AppCompatActivity {
        //static final String PROJECT = "FHI360-App";
        static final String PROJECT = "";
        String ProjectName;
        public static boolean HoldCamera = false;

        public static final String EXTRA_SAMPLEID = "e.nd.paddatacapture.EXTRA_SAMPLEID";
        public static final String EXTRA_TIMESTAMP = "e.nd.paddatacapture.EXTRA_TIMESTAMP";
        public static final String EXTRA_PREDICTED = "e.nd.paddatacapture.EXTRA_PREDICTED";
        public static final String EXTRA_LABEL_DRUGS = "e.nd.paddatacapture.EXTRA_LABEL_DRUGS";

        // NN storage, now setting up array for multiple NN
        //final int number_of_models = 2;
        int number_of_models;
        //String[] model_list = {"fhi360_small_1_21.tflite", "fhi360_conc_large_1_21.tflite"};
        String[] model_list = {};

        ImageProcessor[] imageProcessor = {null, null};
        TensorImage[] tImage =  {null, null};
        TensorBuffer[] probabilityBuffer = {null, null};

        /** An instance of the driver class to run model inference with Tensorflow Lite. */
        protected Interpreter[] tflite = {null, null};

        /** The loaded TensorFlow Lite model. */
        private MappedByteBuffer[] tfliteModel = {null, null};

        /** Options for configuring the Interpreter. */
        private final Interpreter.Options[] tfliteOptions = {new Interpreter.Options(), new Interpreter.Options()};

        final String[] ASSOCIATED_AXIS_LABELS = {"labels.txt", "labels.txt"};
        List<String>[] associatedAxisLabels = (ArrayList<String>[])new ArrayList[2];

        // pls class
        Partial_least_squares pls = null;

        ProgressBar progressBar;
        private int progressStatus = 0;
        private Handler handler = new Handler();

        private SharedPreferences.OnSharedPreferenceChangeListener listener;

        private static String baseUrl = "https://pad.crc.nd.edu/neuralnetworks/tf_lite/";

        private static String subFhiConc = "fhi360_conc_large_lite";
        private static String subFhi = "fhi360_small_lite";
        private static String subId = "idPAD_small_lite";
        private static String subMsh = "msh_tanzania_3k_10_lite";

        //these filenames should get updated from SharedPreferences if new versions are published
        //the SettingsActivity will check for a newer version when the project setting is changed.
        private String idPadName = "idPAD_small_1_6.tflite";
        private String fhiName = "fhi360_small_1_21.tflite";
        private String fhiConcName = "fhi360_conc_large_1_21.tflite";
        private String mshName = "model_small_1_10.tflite";

        private boolean sync = false;

        private FirebaseAnalytics mFirebaseAnalytics;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            FirebaseApp.initializeApp(this);
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

            //initialize opencv
            if (!OpenCVLoader.initDebug()) {
                // Handle initialization error
                Log.i("GBT", "Opencv not loaded");
            }

            //check that a project has been selected, otherwise we can't do anything
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            idPadName = prefs.getString(subId + "filename", idPadName);
            fhiName = prefs.getString(subFhi + "filename", fhiName);
            fhiConcName = prefs.getString(subFhiConc + "filename", fhiConcName);
            mshName = prefs.getString(subMsh + "filename", mshName);

            listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if(key.equals("neuralnet") ){

                        //the Project can be separate from the neural net
                        ProjectName = sharedPreferences.getString("neuralnet", "");

                        idPadName = sharedPreferences.getString(subId + "filename", idPadName);
                        fhiName = sharedPreferences.getString(subFhi + "filename", fhiName);
                        fhiConcName = sharedPreferences.getString(subFhiConc + "filename", fhiConcName);
                        mshName = sharedPreferences.getString(subMsh + "filename", mshName);

                        switch(ProjectName){
                            case "FHI360-App":
                                model_list = new String[]{fhiName, fhiConcName};
                                number_of_models = 2;
                                break;
                            case "Veripad idPAD":
                                model_list = new String[] {idPadName};
                                number_of_models = 1;
                                break;
                            case "MSH Tanzania":
                                model_list = new String[] {mshName};
                                number_of_models = 1;
                                break;
                            default:
                                number_of_models = 0;
                                break;
                        }

                        //InitializeModels();
                    }
                }
            };

            prefs.registerOnSharedPreferenceChangeListener(listener);

            String project = prefs.getString("neuralnet", "");
            ProjectName = project;

            sync = prefs.getBoolean("sync", false);

            if(sync) {
                checkForUpdates(project);
            }

            /*
            String projectFolder = "";
            String[] projectFolders = {};

            // check the currently selected project for updated NN files on app start
            if(project.length() > 0){


                switch(project){
                    case "FHI360-App":

                        //String url = baseUrl + "/" + subFhi;// + "/" + modelVersion + "/" + fhiName;
                        //new updatesCheck().execute(url);


                        //String url2 = baseUrl + "/" + subFhiConc;// + "/" + modelVersion + "/" + fhiConcName;
                        //new updatesCheck().execute(url2);

                        projectFolder = subFhi;
                        projectFolders = new String[]{fhiName, fhiConcName};
                        break;
                    case "Veripad idPAD":


                        //String url3 = baseUrl + "/" + subId;// + "/" + modelVersion + "/" + idPadName;
                        //new updatesCheck().execute(url3);

                        projectFolder = subId;
                        projectFolders = new String[]{subId};
                        break;
                    case "MSH Tanzania":


                        //String url4 = baseUrl + "/" + subMsh;// + "/" + modelVersion + "/" + mshName;
                        //new updatesCheck().execute(url4);

                        projectFolder = subMsh;
                        projectFolders = new String[]{subMsh};
                        break;
                    default:
                        Intent i = new Intent(this, SettingsActivity.class);
                        startActivity(i);
                        return;
                }

                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build();

                WorkRequest myUploadWork =  new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                        .addTag("neuralnet_updates").setInputData(new Data.Builder()
                            .putString("projectkey", projectFolder).putStringArray("projectkeys", projectFolders)
                                .build()
                        )
                        .build();

                WorkManager.getInstance(this).enqueue(myUploadWork);
/*
                InitializeModels();
            }else{
                //go to settings to get the Project set so we can load the models
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }

/*  //offloading this code to a separate function now that the tensor files can be dynamic
            // Initialization code for TensorFlow Lite
            // Initialise the models
            for(int num_mod=0; num_mod < number_of_models; num_mod++) {
                //final int num_mod = 0;
                try {
                    //tfliteModel[num_mod] = FileUtil.loadMappedFile(this, model_list[num_mod]);
                    tfliteModel[num_mod] = loadTensorFile(this, model_list[num_mod]);

                    // does it have metadata?
                    MetadataExtractor metadata = new MetadataExtractor(tfliteModel[num_mod]);
                    if (metadata.hasMetadata()) {
                        // create new list
                        associatedAxisLabels[num_mod] = new ArrayList<>();

                        // get labels
                        InputStream a = metadata.getAssociatedFile("labels.txt");
                        BufferedReader r = new BufferedReader(new InputStreamReader(a));
                        String line;
                        while ((line = r.readLine()) != null) {
                            associatedAxisLabels[num_mod].add(line);
                        }

                        // other metadata
                        ModelMetadata mm = metadata.getModelMetadata();
                        Log.e("GBR", mm.description());
                        Log.e("GBR", mm.version());

                    } else {
                        try {
                            associatedAxisLabels[num_mod] = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS[num_mod]);
                        } catch (IOException e) {
                            Log.e("GBR", "Error reading label file", e);
                        }
                    }

                    // create interpreter
                    tflite[num_mod] = new Interpreter(tfliteModel[num_mod], tfliteOptions[num_mod]);

                    // Reads type and shape of input and output tensors, respectively.
                    int imageTensorIndex = 0;
                    int[] imageShape = tflite[num_mod].getInputTensor(imageTensorIndex).shape(); // {1, 227, 227, 3}
                    DataType imageDataType = tflite[num_mod].getInputTensor(imageTensorIndex).dataType();

                    //output
                    int probabilityTensorIndex = 0;

                    // get output shape
                    int[] probabilityShape =  tflite[num_mod].getOutputTensor(0).shape(); // {1, NUM_CLASSES}
                    DataType probabilityDataType = tflite[num_mod].getOutputTensor(probabilityTensorIndex).dataType();

                    // Create an ImageProcessor with all ops required. For more ops, please
                    // refer to the ImageProcessor Architecture section in this README.
                    imageProcessor[num_mod] = new ImageProcessor.Builder()
                                    .add(new ResizeOp(imageShape[2], imageShape[1], ResizeOp.ResizeMethod.BILINEAR))
                                    .build();

                    // Create a TensorImage object. This creates the tensor of the corresponding
                    // tensor type DataType.FLOAT32.
                    tImage[num_mod] = new TensorImage(imageDataType);

                    // Create a container for the result and specify that this is not a quantized model.
                    // Hence, the 'DataType' is defined as DataType.FLOAT32
                    probabilityBuffer[num_mod] = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                    //TensorBuffer.createFixedSize(new int[]{1, 10}, DataType.FLOAT32);

                } catch (IOException e) {
                    Log.e("GBR", "Error reading model", e);
                }
            }

            // setup pls
            pls = new Partial_least_squares(this);
*/
            // setup remainder
            setContentView(R.layout.activity_main);

            //put in a top toolbar with a menu dropdown
            Toolbar myToolbar = findViewById(R.id.toolbar);
            setSupportActionBar(myToolbar);
        }

        protected List<? extends CameraBridgeViewBase> getCameraViewList() {
            return new ArrayList<CameraBridgeViewBase>();

        }

        public void checkForUpdates(String project){


            String[] projectFolders = {};

            // check the currently selected project for updated NN files on app start
            if(project.length() > 0) {


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
                        //Intent i = new Intent(this, SettingsActivity.class);
                        //startActivity(i);
                        return;
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
            }else{
                //go to settings to get the Project set so we can load the models
                //Intent i = new Intent(this, SettingsActivity.class);
                //startActivity(i);
            }
        }


        private void InitializeModels(){

            //@TODO  this will need to be fully dynamic in case models are added to other projects
            switch(ProjectName){
                case "FHI360-App":
                    model_list = new String[]{fhiName, fhiConcName};
                    number_of_models = 2;
                    break;
                case "Veripad idPAD":
                    model_list = new String[] {idPadName};
                    number_of_models = 1;
                    break;
                case "MSH Tanzania":
                    model_list = new String[] {mshName};
                    number_of_models = 1;
                    break;
                default:
                    number_of_models = 0;
                    //Toast.makeText(this, R.string.no_model_set, Toast.LENGTH_LONG).show();
                    break;
            }
            // Initialization code for TensorFlow Lite
            // Initialise the models
            for(int num_mod=0; num_mod < number_of_models; num_mod++) {
                //final int num_mod = 0;
                try {
                    //tfliteModel[num_mod] = FileUtil.loadMappedFile(this, model_list[num_mod]);
                    tfliteModel[num_mod] = loadTensorFile(this, model_list[num_mod]);

                    Log.d("InitializeModels", model_list[num_mod]);

                    // does it have metadata?
                    MetadataExtractor metadata = new MetadataExtractor(tfliteModel[num_mod]);
                    if (metadata.hasMetadata()) {
                        // create new list
                        associatedAxisLabels[num_mod] = new ArrayList<>();

                        // get labels
                        InputStream a = metadata.getAssociatedFile("labels.txt");
                        BufferedReader r = new BufferedReader(new InputStreamReader(a));
                        String line;
                        while ((line = r.readLine()) != null) {
                            associatedAxisLabels[num_mod].add(line);
                        }

                        // other metadata
                        ModelMetadata mm = metadata.getModelMetadata();
                        Log.e("GBR", mm.description());
                        Log.e("GBR", mm.version());

                    } else {
                        try {
                            associatedAxisLabels[num_mod] = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS[num_mod]);
                        } catch (IOException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            Log.e("GBR", "Error reading label file", e);
                        }
                    }

                    // create interpreter
                    tflite[num_mod] = new Interpreter(tfliteModel[num_mod], tfliteOptions[num_mod]);

                    // Reads type and shape of input and output tensors, respectively.
                    int imageTensorIndex = 0;
                    int[] imageShape = tflite[num_mod].getInputTensor(imageTensorIndex).shape(); // {1, 227, 227, 3}
                    DataType imageDataType = tflite[num_mod].getInputTensor(imageTensorIndex).dataType();

                    //output
                    int probabilityTensorIndex = 0;

                    // get output shape
                    int[] probabilityShape =  tflite[num_mod].getOutputTensor(0).shape(); // {1, NUM_CLASSES}
                    DataType probabilityDataType = tflite[num_mod].getOutputTensor(probabilityTensorIndex).dataType();

                    // Create an ImageProcessor with all ops required. For more ops, please
                    // refer to the ImageProcessor Architecture section in this README.
                    imageProcessor[num_mod] = new ImageProcessor.Builder()
                            .add(new ResizeOp(imageShape[2], imageShape[1], ResizeOp.ResizeMethod.BILINEAR))
                            .build();

                    // Create a TensorImage object. This creates the tensor of the corresponding
                    // tensor type DataType.FLOAT32.
                    tImage[num_mod] = new TensorImage(imageDataType);

                    // Create a container for the result and specify that this is not a quantized model.
                    // Hence, the 'DataType' is defined as DataType.FLOAT32
                    probabilityBuffer[num_mod] = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                    //TensorBuffer.createFixedSize(new int[]{1, 10}, DataType.FLOAT32);

                } catch (IOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Log.e("GBR", "Error reading model", e);
                }
            }

            // setup pls
            pls = new Partial_least_squares(this);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu){

            //attach the menu for settings and queue to the app bar
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.maintoolbarmenu, menu);
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
                default:
                    // If we got here, the user's action was not recognized.
                    // Invoke the superclass to handle it.
                    return super.onOptionsItemSelected(item);

            }
            //return true;
        }

            public void startImageCapture(View view){
            Log.i("GBR", "Image capture starting");
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED)
                    | (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 90);
            } else {
                //progressBar.setVisibility(View.VISIBLE);
/*
                new Thread(new Runnable() {
                    public void run(){
                        while( progressStatus < 100){
                            progressStatus += 1;

                            handler.post(new Runnable(){
                                public void run(){
                                    progressBar.setProgress(progressStatus);
                                }
                            });
                            try{
                                Thread.sleep(5);
                            }catch(InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
*/

                //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pads://capture"));
                Intent intent = new Intent(this, Camera2Activity.class);
                startActivityForResult(intent, 10);
            }
        }

        //ActivityResultLauncher<String> mGetResult = registerForActivityResult()

/*
        public void startCameraActivity(View view){



        }
*/
        @Override
        protected void onResume() {
            super.onResume();
            if( !HoldCamera ) {
                //startImageCapture(null);
            }
            InitializeModels();
        }

        private void UncompressOutputs( InputStream fin, File targetDirectory ) throws Exception {
            byte[] buffer = new byte[4096];
            try (BufferedInputStream bis = new BufferedInputStream(fin); ZipInputStream stream = new ZipInputStream(bis)) {
                ZipEntry entry;
                while ((entry = stream.getNextEntry()) != null) {

                    File f = new File(targetDirectory.getPath(), entry.getName());
                    try (FileOutputStream fos = new FileOutputStream(targetDirectory.getPath() + "/" + entry.getName());
                         BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

                        int len;
                        while ((len = stream.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }


                }
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            Log.i("GBT", "onActivityResult");
            if (resultCode == RESULT_OK && requestCode == 10) {
                Uri resultData = data.getData();
                if (resultData != null) {
                    try {
                        // Create Output Directory
                        String timestamp = "";
                        if (data.hasExtra("timestamp"))
                            timestamp = String.format("%d", data.getExtras().getLong("timestamp"));

                        File targetDir = new File(this.getFilesDir(), timestamp);
                        targetDir.mkdirs();

                        // Extract Files
                        UncompressOutputs(getContentResolver().openInputStream(resultData), targetDir);

                        // Handle rectified image
                        File rectifiedFile = new File(targetDir, "rectified.png");

                        // crop input image
                        Bitmap bmRect = BitmapFactory.decodeFile(rectifiedFile.getPath());
                        Bitmap bm = Bitmap.createBitmap(bmRect, 71, 359, 636, 490);

                        // create output string
                        String output_string = new String();

                        // categorize for each model in list
                        for (int num_mod = 0; num_mod < number_of_models; num_mod++) {
                            tImage[num_mod].load(bm);
                            tImage[num_mod] = imageProcessor[num_mod].process(tImage[num_mod]);

                            // Running inference
                            if (null != tflite[num_mod]) {
                                // categorize
                                tflite[num_mod].run(tImage[num_mod].getBuffer(), probabilityBuffer[num_mod].getBuffer());
                                float[] probArray = probabilityBuffer[num_mod].getFloatArray();
                                int maxidx = findMaxIndex(probArray);

                                // concat to output string
                                output_string += associatedAxisLabels[num_mod].get(maxidx);
                                if (num_mod == 0) {
                                    output_string += String.format(" (%.3f)", probabilityBuffer[num_mod].getFloatArray()[maxidx]);
                                }

                                if (num_mod != number_of_models - 1) {
                                    output_string += ", ";
                                }
                                // print results
                                Log.i("GBR", String.valueOf(probabilityBuffer[num_mod].getFloatArray()[0]));
                                Log.i("GBR", String.valueOf(probabilityBuffer[num_mod].getFloatArray()[maxidx]));
                                Log.i("GBR", associatedAxisLabels[num_mod].get(maxidx));
                            }
                        }

                        // calculate concentration from PLSR method
                        // get drug if available
                        String[] drug = output_string.split(" ", 2);
                        String drugStr = "albendazole";
                        if (drug.length > 1) {
                            drugStr = drug[0].toLowerCase();
                        }

                        if (ProjectName.equals("FHI360-App")) {
                            // call
                            double concentration = pls.do_pls(bmRect, drugStr);

                            // add conc. result to string
                            output_string += "%, (PLS " + (int) concentration + "%)";
                        }

                        Intent intent = new Intent(this, ResultActivity.class);
                        intent.setData(Uri.fromFile(rectifiedFile));
                        intent.putExtra(EXTRA_PREDICTED, output_string);
                        if (data.hasExtra("qr"))
                            intent.putExtra(EXTRA_SAMPLEID, data.getExtras().getString("qr"));
                        if (data.hasExtra("timestamp")) intent.putExtra(EXTRA_TIMESTAMP, timestamp);
                        if (null != associatedAxisLabels[0] && associatedAxisLabels[0].size() > 0)
                            intent.putExtra(EXTRA_LABEL_DRUGS, (String[]) associatedAxisLabels[0].toArray(new String[0]));
                        startActivity(intent);

                        HoldCamera = true;

                        Log.i("GBR", output_string + "%");
                    } catch (Exception e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                // do nothing for now
            }

            Log.i("GBR", String.valueOf(resultCode));
            Log.i("GBR", String.valueOf(requestCode));
        }

        private static final int findMaxIndex(float [] arr) {
            float max = arr[0];
            int maxIdx = 0;
            for(int i = 1; i < arr.length; i++) {
                if(arr[i] > max) {
                    max = arr[i];
                    maxIdx = i;
                }
            }
            return maxIdx;
        };

        @Override
        public void onDestroy(){
            //progressBar.setVisibility(View.INVISIBLE);
            super.onDestroy();
        }
/*
Utility function to load the tensor models from internal storage instead of assets
 */
        public MappedByteBuffer loadTensorFile(Context context, String filename) throws IOException {

            File dir = context.getDir("tflitemodels", Context.MODE_PRIVATE);

            File tensorFile = new File(dir.getPath(), filename);

            MappedByteBuffer buffer;
            try{
                FileInputStream input = new FileInputStream(tensorFile);
                try{
                    FileChannel fileChannel = input.getChannel();
                    buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, tensorFile.length());
                }catch(Throwable t){
                    try{
                        input.close();
                    }catch(Throwable t2){
                        t2.addSuppressed(t);
                    }
                    throw t;
                }
                input.close();
            }catch(Throwable t3){
                throw t3;
            }
            return buffer;
        }
/*
        private class getUpdated extends AsyncTask<String, String, String> {

            String thisProject;
            String projectFolder;
            //String url;
            ProgressBar progressBar;
            List<String> filesList = new ArrayList<>();

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                progressBar = (ProgressBar) findViewById(R.id.mainProgressBar);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(String result){
                super.onPostExecute(result);
            }

            @Override
            protected String doInBackground(String... version){

                int count;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = prefs.edit();

                thisProject = prefs.getString("neuralnet", "");

                String modelVersion = version[0];

                List<String> filesList = new ArrayList<>();
                List<String> urlsList = new ArrayList<>();

                switch(thisProject){
                    case "FHI360-App":

                        String url = baseUrl + "/" + subFhi + "/" + modelVersion + "/";// + fhiName;
                        filesList = getLatestVersionFiles(url);

                        for(String file : filesList){
                            urlsList.add(url + file);
                        }

                        String url2 = baseUrl + "/" + subFhiConc + "/" + modelVersion + "/";// + fhiConcName;
                        List<String> otherFiles = new ArrayList<>();
                        otherFiles = getLatestVersionFiles(url2);

                        for(String file : otherFiles){
                            //filesList.add(file);
                            urlsList.add(url2 + file);
                        }



                        break;
                    case "Veripad idPAD":


                        String url3 = baseUrl + "/" + subId + "/" + modelVersion + "/";// + idPadName;
                        filesList = getLatestVersionFiles(url3);
                        for(String file : filesList){
                            urlsList.add(url3 + file);
                        }

                        break;
                    case "MSH Tanzania":


                        String url4 = baseUrl + "/" + subMsh + "/" + modelVersion + "/";// + mshName;
                        filesList = getLatestVersionFiles(url4);
                        for(String file : filesList){
                            urlsList.add(url4 + file);
                        }

                        break;
                    default:
                        return null;
                }

                try {
                    for (String file : urlsList) {

                        URL url = new URL(file);
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
                            publishProgress(String.valueOf((total * 100) / lengthOfFile));

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

            protected void onProgressUpdate(String... progress){

                progressBar.setProgress(Integer.parseInt(progress[0]));

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

        }

        private class updatesCheck extends AsyncTask<String, String, String> {

            AlertDialog alertDialog;

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            }

            @Override
            protected void onPostExecute(String result){
                super.onPostExecute(result);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String projectFolder = prefs.getString("ProjectFolder", "");
                String projectFolderVersion = prefs.getString(projectFolder + "version", "1.0/");

                if(!result.equals(projectFolderVersion)) {
                    alertDialog.setTitle("Download Update Now?");
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            new getUpdated().execute(result);
                        }
                    });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });

                    alertDialog.show();
                }
            }

            @Override
            protected String doInBackground(String... f_url){


                try{
                    String latestVersion = getLatestNNVersion(f_url[0]);

                    if(!latestVersion.equals("1.0/")){
                        //ask to download the latest
                        return latestVersion;
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }

                return "1.0/";
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
        }
*/
    }
