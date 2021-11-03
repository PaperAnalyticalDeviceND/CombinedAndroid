package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
//import android.support.design.widget.FloatingActionButton;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

//import static android.support.v4.content.FileProvider.getUriForFile;
import static java.lang.Math.sqrt;

public class CameraActivity extends AppCompatActivity implements CvCameraViewListener2 {

    //Activity to open the camera and aquire a preview image after the PAD fiducials are located
    //uses the OpenCV 4.5.4 JavaCameraView class
    //@TODO update to JavaCamera2View

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[] {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private JavaCamResView mOpenCvCameraView;

    public Mat mRgba, mRgbaTemp;
    private Mat mTemplate;
    private String LOG_TAG = "PAD";
    private ProgressDialog dialog, progdialog;
    private Mat mRgbaModified;
    private static int IMAGE_WIDTH = 720;
    private String qrText = new String();

    //saved contour results
    private boolean markersDetected = false;
    private Mat testMat;
    private Mat cropped;
    private AlertDialog ad = null;
    List<Point> last_points = null;

    //UI
    private FloatingActionButton analyzeButton;

    private Intent mResultIntent = new Intent();

    ProgressBar progressBar;

    private boolean allPermissionsGranted(){
        for( String permission : REQUIRED_PERMISSIONS ){
            if(ContextCompat.checkSelfPermission(getBaseContext(), permission ) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }

        return true;
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("PADS", "OpenCV loaded successfully");
                    mOpenCvCameraView.setCameraPermissionGranted();
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    private class DataPoint implements Comparable<DataPoint>  {
        public int i;
        public float Distance, Diameter;
        public Point Center;
        public Boolean valid = true;

        public DataPoint(int ii, float id, float idi, Point imc){
            i = ii;
            Distance = id;
            Diameter = idi;
            Center = imc;
        }

        public int compareTo(DataPoint other) {
            return new Float(Distance).compareTo(new Float(other.Distance));
        }
    };

    private int progressStatus = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        Log.d("PADS", "Entering CameraActivity");

        //progressBar = findViewById(R.id.indeterminateBar);
        //progressBar.animate();
        //progressBar.setVisibility(View.INVISIBLE);
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
                        Thread.sleep(50);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
*/

        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 91);

        if(allPermissionsGranted()){
            Log.d(LOG_TAG, "Permissions granted. CameraActivity.onCreate()");
        }else{
            Log.e(LOG_TAG, "Hardware Permissions not granted.");
        }
/* //debug code
        Intent callingIntent = getIntent();
        if(callingIntent == null){
            Log.d(LOG_TAG, "Calling Intent NOT found in CameraActivity.onCreate");
        }
*/
        mOpenCvCameraView = (JavaCamResView) findViewById(R.id.activity_surface_view);
        analyzeButton = (FloatingActionButton) findViewById(R.id.floatingAnalyze);

        //progressBar.setProgress(100, true);
        //progressBar.incrementProgressBy(100);
        //progressBar.setVisibility(View.INVISIBLE);
        //Toast.makeText(this, this.getString(R.string.maininstructions), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        //progressBar.setVisibility(View.VISIBLE);
        //progressBar.animate();

        Intent callingIntent = getIntent();
        if(callingIntent == null){
            Log.d(LOG_TAG, "Calling Intent NOT found in CameraActivity.onResume");
        }

        Toast.makeText(this, this.getString(R.string.maininstructions), Toast.LENGTH_LONG).show();



        if (!OpenCVLoader.initDebug()) {
            Log.d("PADs", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d("PADs", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mRgba = new Mat();
        mRgbaTemp = new Mat();
        testMat = new Mat();
        cropped = new Mat();

        // Parse input template file
        Bitmap tBM = BitmapFactory.decodeStream(this.getClass().getResourceAsStream("/template.png"));

        // Convert to OpenCV matrix
        Mat tMat = new Mat();
        Utils.bitmapToMat(tBM, tMat);

        // Parse input test file
        Bitmap tBM2 = BitmapFactory.decodeStream(this.getClass().getResourceAsStream("/test42401.png"));
        // Convert to OpenCV matrix
        Utils.bitmapToMat(tBM2, testMat);

        mTemplate = new Mat();
        Imgproc.cvtColor(tMat, mTemplate, Imgproc.COLOR_BGRA2GRAY);

        Log.d("PADS", "Entering CameraActivity.onResume()");

        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.enableView();
        Log.d("PADS", "Leaving CameraActivity.onResume()");

        //progressBar.setVisibility(View.INVISIBLE);
    }


    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void goToSettings(View view){
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void onCameraViewStarted(int width, int height) {
        Log.d("PADS", "Entering CameraActivity.onCameraViewStarted");

        /*
        progressBar.setVisibility(View.VISIBLE);
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
                        Thread.sleep(50);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
*/
        mOpenCvCameraView.Setup();
        //progressBar.setVisibility(View.INVISIBLE);
    }

    public void onCameraViewStopped() {
        Log.d(LOG_TAG, "Entering onCameraViewStopped()");
    }

    @Override
    public void finish() {
        Log.i("PAD", "Sending result:" + mResultIntent.toString());
        setResult(RESULT_OK, mResultIntent);
        super.finish();
    }
/*
Function to return to MainActivity with no actionable result
Set as onClick for the floatingBack button
 */
    public void goHome(View view){
        setResult(RESULT_CANCELED, mResultIntent);
        super.finish();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgbaModified = inputFrame.rgba();

        Log.d(LOG_TAG, "Entering onCameraFrame()");

        Intent callingIntent = getIntent();
        if(callingIntent == null){
            Log.d(LOG_TAG, "Calling Intent NOT found in CameraActivity.onResume");
        }else{
            Log.d(LOG_TAG, "Calling Intent found in CameraActivity.onResume");
        }

        //woiting on dialog?
        if(ad != null) return mRgbaModified;

        mRgbaModified.copyTo(mRgbaTemp);

        boolean portrait = true;
        Mat work = new Mat();
        float ratio;

        if(mRgbaModified.size().height >  mRgbaModified.size().width) {
            Log.d(LOG_TAG, "Portrait image detected.");
            Imgproc.resize(inputFrame.gray(), work, new Size(IMAGE_WIDTH, (mRgbaModified.size().height * IMAGE_WIDTH) / mRgbaModified.size().width), 0, 0, Imgproc.INTER_LINEAR);
            ratio = (float)mRgbaModified.size().width / (float)IMAGE_WIDTH;
        }else{
            portrait = false;
            Log.d(LOG_TAG, "Landscape image detected.");
            Imgproc.resize(inputFrame.gray(), work, new Size((mRgbaModified.size().width * IMAGE_WIDTH) / mRgbaModified.size().height, IMAGE_WIDTH), 0, 0, Imgproc.INTER_LINEAR);
            Core.transpose(work, work);
            Core.flip(work, work, 1);
            ratio = (float)mRgbaModified.size().height / (float)IMAGE_WIDTH;
        }

        //create source points
        List<Point> src_points = new Vector<>();

        //Look for fiducials
        try {
            boolean fiducialsAcquired = ContourDetection.GetFudicialLocations(mRgbaModified, work, src_points, portrait);

            //auto analyze?
            if (fiducialsAcquired) {

                //setup to check not moving fast
                boolean moving = false;

                //setup last points or find norm difference
                if (last_points == null) {
                    last_points = new Vector<>();
                    for (int i = 0; i < 6; i++) {
                        last_points.add(new Point(-1, -1));
                    }
                    moving = true;
                } else {
                    //test distance
                    double norm = 0;
                    for (int i = 0; i < 6; i++) {
                        if (last_points.get(i).x > 0 && src_points.get(i).x > 0) {
                            norm += (last_points.get(i).x - src_points.get(i).x) * (last_points.get(i).x - src_points.get(i).x) + (last_points.get(i).y - src_points.get(i).y) * (last_points.get(i).y - src_points.get(i).y);
                        }
                    }
                    double sqrt_norm = sqrt(norm) / ratio;

                    //test if moving too quickley
                    if (sqrt_norm > 10) {
                        moving = true;
                    }
                    Log.i("ContoursOut", String.format("norm diff %f", sqrt(norm)));
                }

                //copy last point
                Collections.copy(last_points, src_points);

                //return if appears to be moving
                if (moving) return mRgbaModified;

                Log.i("ContoursOut", String.format("Source points (%f, %f),(%f, %f),(%f, %f),(%f, %f),(%f, %f),(%f, %f).",
                        src_points.get(0).x, src_points.get(0).y, src_points.get(1).x, src_points.get(1).y, src_points.get(2).x,
                        src_points.get(2).y, src_points.get(3).x, src_points.get(3).y, src_points.get(4).x, src_points.get(4).y,
                        src_points.get(5).x, src_points.get(5).y));

                //get pad version
                int pad_version = 0;
                int pad_index = 0;

                //smaller image?
                Rect roi = new Rect(0, 0, 720 / 2, 1220 / 2);
                Mat smallImg = new Mat(work, roi);

                //grab QR code
                String qr_data = null;
                try {
                    qr_data = readQRCode(smallImg);
                    if (qr_data.substring(0, 21).equals("padproject.nd.edu/?s=")) {
                        pad_version = 10;
                        pad_index = 0;
                    } else if (qr_data.substring(0, 21).equals("padproject.nd.edu/?t=")) {
                        pad_version = 20;
                        pad_index = 1;
                    }
                    qrText = qr_data;
                } catch (Exception e) {
                    Log.i("ContoursOut", "QR error" + e.toString());
                }

                if (pad_version != 0) {
                    Log.i("ContoursOut", "Version " + Integer.toString(pad_version));
                    //save successful frame
                    mRgbaTemp.copyTo(mRgba);

                    //flag saved
                    markersDetected = true;

                    //enable button once acquired
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            analyzeButton.setEnabled(true);
                        }
                    });

                    // rectify image, include QR/Fiducial points
                    float dest_points[][] = {{85, 1163, 686, 1163, 686, 77, 244, 64, 82, 64, 82, 226}, {85, 1163, 686, 1163, 686, 77, 255, 64, 82, 64, 82, 237}};

                    //Note: sending color corrected image to rectifyer
                    boolean transformedOk = ContourDetection.RectifyImage(mRgba, mTemplate, cropped, src_points, dest_points[pad_index]);

                    //error?
                    if (transformedOk) {
                        Log.i("ContoursOut", String.format("Transformed correctly"));

                        //ask if we want to save data for email, also block updates until done.
                        showSaveDialog();
                    }
                }
            }
        }catch (Exception e){
            Log.d("PADS", "Fudicial Location find exception:" + e.toString());
        }

        return mRgbaModified;
    }


    public void doAnalysis(View view) {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle("Paper Analytical Device (PAD) project\nat the University of Notre Dame.")
                .setMessage(Html.fromHtml("The PAD projects brings crowdsourcing to the testing of theraputic drugs.<br><a href=\"http://padproject.nd.edu\">http://padproject.nd.edu</a>"))
                .create();
        d.show();

        mOpenCvCameraView.StopPreview();
        // Make the textview clickable. Must be called after show()
        ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }


    private void CompressOutputs( File[] files, File output ) throws Exception{
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        byte buffer[] = new byte[4096];
        for (int i = 0; i < files.length; i++) {
            BufferedInputStream origin = new BufferedInputStream(new FileInputStream(files[i]), buffer.length);

            ZipEntry entry = new ZipEntry(files[i].getPath().substring(files[i].getPath().lastIndexOf("/") + 1));
            out.putNextEntry(entry);

            int count;
            while ((count = origin.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, count);
            }

            origin.close();
        }
        out.close();
    }


    public void showSaveDialog(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("UI thread", "I am the UI thread");

                AlertDialog.Builder alert = new AlertDialog.Builder(CameraActivity.this);
                alert.setTitle("Fiducials acquired!");
                alert.setMessage("Store PAD image?");
                alert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                                Date today = Calendar.getInstance().getTime();

                                File imagePath = new File(CameraActivity.this.getFilesDir(), "images");
                                File padImageDirectory = new File(imagePath + "/PAD/" + df.format(today));
                                padImageDirectory.mkdirs();

                                //save rectified image
                                File cFile = new File(padImageDirectory, "rectified.png");
                                Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_BGRA2RGB);
                                Imgcodecs.imwrite(cFile.getPath(), cropped);

                                //save original image
                                File oFile = new File(padImageDirectory, "original.png");
                                Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGRA2RGB);
                                Imgcodecs.imwrite(oFile.getPath(), mRgba);

                                //gallery?
                                try {
                                    MediaStore.Images.Media.insertImage(getContentResolver(), cFile.getPath(),
                                            df.format(today), "Rectified Image");
                                    MediaStore.Images.Media.insertImage(getContentResolver(), oFile.getPath(),
                                            df.format(today), "Original Image");
                                    //Log.i("ContoursOut", "Saved to gallery");
                                } catch (Exception e) {
                                    Log.i("ContoursOut", "Cannot save to gallery" + e.toString());
                                }

                                Intent intent = getIntent();
                                if( intent != null /* && intent.getData() != null */){
                                    try{
                                        File target = new File(padImageDirectory, "compressed.zip");
                                        CompressOutputs(new File[]{ cFile, oFile }, target);
                                        mResultIntent.setData(FileProvider.getUriForFile(CameraActivity.this, "edu.nd.crc.paperanalyticaldevices.fileprovider", target));
                                        mResultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        mResultIntent.putExtra("qr", qrText);
                                        mResultIntent.putExtra("timestamp", Calendar.getInstance().getTimeInMillis());
                                        finish();
                                    }catch( Exception e ) {
                                        Log.i("ContoursOut", "Cannot compress files: " + e.toString());
                                    }
                                }else {
                                    Log.i("ContoursOut", cFile.getPath());

                                    Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                    i.setType("message/rfc822");
                                    i.setType("application/image");
                                    //i.putExtra(Intent.EXTRA_EMAIL, new String[]{"paperanalyticaldevices@gmail.com"});
                                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mcurran2@nd.edu"});
                                    i.putExtra(Intent.EXTRA_SUBJECT, "PADs");
                                    i.putExtra(Intent.EXTRA_TEXT, "Pad image (" + qrText + ")");
                                    ArrayList<Uri> uris = new ArrayList<Uri>();
                                    uris.add(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(cFile.getPath())));
                                    uris.add(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(oFile.getPath())));
                                    i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    try {
                                        startActivity(i);
                                    } catch (android.content.ActivityNotFoundException ex) {
                                        Log.i("ContoursOut", "There are no email clients installed.");
                                    }

                                    //start preview
                                    mOpenCvCameraView.StartPreview();

                                    dialog.dismiss();

                                    ad = null;
                                }
                            }
                        }
                );
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //start preview
                                mOpenCvCameraView.StartPreview();

                                dialog.dismiss();

                                ad = null;
                            }
                        }
                );
                ad = alert.show();

            }
        });

        //stop preview while we wait for response
        mOpenCvCameraView.StopPreview();
    }


    public static String readQRCode(Mat mTwod){
        Bitmap bMap = Bitmap.createBitmap(mTwod.width(), mTwod.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mTwod, bMap);
        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new MultiFormatReader();
        //....doing the actually reading
        Result result = null;
        try {
            result = reader.decode(bitmap);
        } catch (NotFoundException e) {
            Log.i("ContoursOut", "QR error" + e.toString());
            e.printStackTrace();
        } catch (ChecksumException e) {
            Log.i("ContoursOut", "QR error" + e.toString());
            e.printStackTrace();
        } catch (FormatException e) {
            Log.i("ContoursOut", "QR error" + e.toString());
            e.printStackTrace();
        }
        Log.i("ContoursOut", String.format("QR: %s", result.getText()));

        //return
        return result.getText();
    }


    public class PredictionGuess implements Comparable<PredictionGuess> {
        public int Index;
        public float Confidence;
        public int NetIndex;

        public PredictionGuess(int i, float c, int j) {
            this.Index = i;
            this.Confidence = c;
            this.NetIndex = j;
        }

        @Override
        public int compareTo(PredictionGuess that) {
            if( this.Confidence > that.Confidence ){
                return -1;
            }else{
                return 1;
            }
        }
    }

}