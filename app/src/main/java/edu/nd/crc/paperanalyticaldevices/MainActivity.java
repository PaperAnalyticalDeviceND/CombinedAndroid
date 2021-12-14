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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

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
    public static final String EXTRA_SAMPLEID = "e.nd.paddatacapture.EXTRA_SAMPLEID";
    public static final String EXTRA_TIMESTAMP = "e.nd.paddatacapture.EXTRA_TIMESTAMP";
    public static final String EXTRA_PREDICTED = "e.nd.paddatacapture.EXTRA_PREDICTED";
    public static final String EXTRA_LABEL_DRUGS = "e.nd.paddatacapture.EXTRA_LABEL_DRUGS";
    static final String PROJECT = "";
    public static boolean HoldCamera = false;
    private static final String subFhiConc = "fhi360_conc_large_lite";
    private static final String subFhi = "fhi360_small_lite";
    private static final String subId = "idPAD_small_lite";
    private static final String subMsh = "msh_tanzania_3k_10_lite";
    final String[] ASSOCIATED_AXIS_LABELS = {"labels.txt", "labels.txt"};
    /**
     * Options for configuring the Interpreter.
     */
    private final Interpreter.Options[] tfliteOptions = {new Interpreter.Options(), new Interpreter.Options()};
    /**
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    protected Interpreter[] tflite = {null, null};
    String ProjectName;
    // NN storage, now setting up array for multiple NN
    int number_of_models;
    String[] model_list = {};
    ImageProcessor[] imageProcessor = {null, null};
    TensorImage[] tImage = {null, null};
    TensorBuffer[] probabilityBuffer = {null, null};
    List<String>[] associatedAxisLabels = (ArrayList<String>[]) new ArrayList[2];
    // pls class
    Partial_least_squares pls = null;

    /**
     * The loaded TensorFlow Lite model.
     */
    private final MappedByteBuffer[] tfliteModel = {null, null};
    //these filenames should get updated from SharedPreferences if new versions are published
    //the SettingsActivity will check for a newer version when the project setting is changed.
    private String idPadName = "idPAD_small_1_6.tflite";
    private String fhiName = "fhi360_small_1_21.tflite";
    private String fhiConcName = "fhi360_conc_large_1_21.tflite";
    private String mshName = "model_small_1_10.tflite";

    private static int findMaxIndex(float[] arr) {
        float max = arr[0];
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

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

        //the Project can be separate from the neural net
        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
            if (key.equals("neuralnet")) {

                //the Project can be separate from the neural net
                ProjectName = sharedPreferences.getString("neuralnet", "");

                idPadName = sharedPreferences.getString(subId + "filename", idPadName);
                fhiName = sharedPreferences.getString(subFhi + "filename", fhiName);
                fhiConcName = sharedPreferences.getString(subFhiConc + "filename", fhiConcName);
                mshName = sharedPreferences.getString(subMsh + "filename", mshName);

                switch (ProjectName) {
                    case "FHI360-App":
                        model_list = new String[]{fhiName, fhiConcName};
                        number_of_models = 2;
                        break;
                    case "Veripad idPAD":
                        model_list = new String[]{idPadName};
                        number_of_models = 1;
                        break;
                    case "MSH Tanzania":
                        model_list = new String[]{mshName};
                        number_of_models = 1;
                        break;
                    default:
                        number_of_models = 0;
                        break;
                }
            }
        };

        prefs.registerOnSharedPreferenceChangeListener(listener);

        String project = prefs.getString("neuralnet", "");
        ProjectName = project;

        boolean sync = prefs.getBoolean("sync", false);

        if (sync) {
            checkForUpdates(project);
        }

        // setup remainder
        setContentView(R.layout.activity_main);

        //put in a top toolbar with a menu dropdown
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
    }

    public void checkForUpdates(String project) {
        String[] projectFolders;

        // check the currently selected project for updated NN files on app start
        if (project.length() > 0) {
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
        }
    }

    private void InitializeModels() {
        //@TODO  this will need to be fully dynamic in case models are added to other projects
        switch (ProjectName) {
            case "FHI360-App":
                model_list = new String[]{fhiName, fhiConcName};
                number_of_models = 2;
                break;
            case "Veripad idPAD":
                model_list = new String[]{idPadName};
                number_of_models = 1;
                break;
            case "MSH Tanzania":
                model_list = new String[]{mshName};
                number_of_models = 1;
                break;
            default:
                number_of_models = 0;
                break;
        }
        // Initialization code for TensorFlow Lite
        // Initialise the models
        for (int num_mod = 0; num_mod < number_of_models; num_mod++) {
            try {
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
                int[] probabilityShape = tflite[num_mod].getOutputTensor(0).shape(); // {1, NUM_CLASSES}
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

            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Log.e("GBR", "Error reading model", e);
            }
        }

        // setup pls
        pls = new Partial_least_squares(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    }

    public void startImageCapture(View view) {
        Log.i("GBR", "Image capture starting");
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                | (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 90);
        } else {
            Intent intent = new Intent(this, Camera2Activity.class);
            startActivityForResult(intent, 10);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        InitializeModels();
    }

    private void UncompressOutputs(InputStream fin, File targetDirectory) throws Exception {
        byte[] buffer = new byte[4096];
        try (BufferedInputStream bis = new BufferedInputStream(fin); ZipInputStream stream = new ZipInputStream(bis)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {

                File f = new File(targetDirectory.getPath(), entry.getName());
                String canonicalPath = f.getCanonicalPath();
                if (!canonicalPath.startsWith(targetDirectory.getPath())) {
                    throw new SecurityException();
                }

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

                    File targetDir = new File(getFilesDir(), timestamp);
                    targetDir.mkdirs();

                    // Extract Files
                    UncompressOutputs(getContentResolver().openInputStream(resultData), targetDir);

                    // Handle rectified image
                    File rectifiedFile = new File(targetDir, "rectified.png");

                    // crop input image
                    Bitmap bmRect = BitmapFactory.decodeFile(rectifiedFile.getPath());
                    Bitmap bm = Bitmap.createBitmap(bmRect, 71, 359, 636, 490);

                    // create output string
                    StringBuilder output_string = new StringBuilder();

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
                            output_string.append(associatedAxisLabels[num_mod].get(maxidx));
                            if (num_mod == 0) {
                                output_string.append(String.format(" (%.3f)", probabilityBuffer[num_mod].getFloatArray()[maxidx]));
                            }

                            if (num_mod != number_of_models - 1) {
                                output_string.append(", ");
                            }
                            // print results
                            Log.i("GBR", String.valueOf(probabilityBuffer[num_mod].getFloatArray()[0]));
                            Log.i("GBR", String.valueOf(probabilityBuffer[num_mod].getFloatArray()[maxidx]));
                            Log.i("GBR", associatedAxisLabels[num_mod].get(maxidx));
                        }
                    }

                    // calculate concentration from PLSR method
                    // get drug if available
                    String[] drug = output_string.toString().split(" ", 2);
                    String drugStr = "albendazole";
                    if (drug.length > 1) {
                        drugStr = drug[0].toLowerCase();
                    }

                    if (ProjectName.equals("FHI360-App")) {
                        // call
                        double concentration = pls.do_pls(bmRect, drugStr);

                        // add conc. result to string
                        output_string.append("%, (PLS ").append((int) concentration).append("%)");
                    }

                    Intent intent = new Intent(this, ResultActivity.class);
                    intent.setData(Uri.fromFile(rectifiedFile));
                    intent.putExtra(EXTRA_PREDICTED, output_string.toString());
                    if (data.hasExtra("qr"))
                        intent.putExtra(EXTRA_SAMPLEID, data.getExtras().getString("qr"));
                    if (data.hasExtra("timestamp")) intent.putExtra(EXTRA_TIMESTAMP, timestamp);
                    if (null != associatedAxisLabels[0] && associatedAxisLabels[0].size() > 0)
                        intent.putExtra(EXTRA_LABEL_DRUGS, associatedAxisLabels[0].toArray(new String[0]));
                    startActivity(intent);

                    HoldCamera = true;

                    Log.i("GBR", output_string + "%");
                } catch (Exception e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    e.printStackTrace();
                }
            }
        }

        Log.i("GBR", String.valueOf(resultCode));
        Log.i("GBR", String.valueOf(requestCode));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
    Utility function to load the tensor models from internal storage instead of assets
     */
    public MappedByteBuffer loadTensorFile(Context context, String filename) throws IOException {

        File dir = context.getDir("tflitemodels", Context.MODE_PRIVATE);

        File tensorFile = new File(dir.getPath(), filename);

        MappedByteBuffer buffer;
        try {
            FileInputStream input = new FileInputStream(tensorFile);
            try {
                FileChannel fileChannel = input.getChannel();
                buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, tensorFile.length());
            } catch (Throwable t) {
                try {
                    input.close();
                } catch (Throwable t2) {
                    t2.addSuppressed(t);
                }
                throw t;
            }
            input.close();
        } catch (Throwable t3) {
            throw t3;
        }
        return buffer;
    }
}
