package edu.nd.crc.paperanalyticaldevices;

//import androidx.appcompat.app.AppCompatActivity;
import static android.Manifest.permission.CAMERA;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.slider.Slider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
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
import org.opencv.android.CameraBridgeViewBase;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArucoCameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private static final int IMAGE_WIDTH = 720;
    public Mat mRgba, mRgbaTemp;
    List<Point> last_points = null;
    private JavaCam2ResView mOpenCvCameraView;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("PADS", "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };
    private Mat mTemplate;
    private String qrText = "";
    //saved contour results
    private Mat cropped;
    private AlertDialog ad = null;
    private SeekBar thresholdSeekBar = null;
    private TextView thresholdText = null;
    int min = 66, max = 92, current = 74;
    private Slider slider = null;

    SharedPreferences mPreferences = null;

    //UI
    private final Intent mResultIntent = new Intent();

    public ArucoCameraActivity() {
        Log.i("PADS", "Instantiated new " + this.getClass());
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setContentView(R.layout.activity_aruco_camera);
        setContentView(R.layout.activity_aruco_camera);

        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 91);

        mOpenCvCameraView = findViewById(R.id.aruco_surface_view);

        //thresholdText = findViewById(R.id.threshold_text);

        mPreferences = getSharedPreferences(MainActivity.PROJECT, MODE_PRIVATE);

        /*current = mPreferences.getInt("Threshold", 74);
        if(current % 2 != 0){
            current -= 1;
        }
        if(current < 66){
            current = 66;
        }

        slider = findViewById(R.id.slider);
        slider.setValue(current);

        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                //thresholdText.setText("" + value);
                current = (int) value;
            }
        });*/
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("PAD", "onCameraViewStarted()");
        mOpenCvCameraView.Setup();
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("PAD", "onCameraViewStopped()");
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.StopPreview();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat mRgbaModified = inputFrame.rgba();

        //waiting on dialog?
        if (ad != null) return mRgbaModified;

        mRgbaModified.copyTo(mRgbaTemp);

        //Mat work = new Mat();
        Mat work = inputFrame.gray();
        float ratio;

        //create source points
        //List<Point> src_points = new Vector<>();
        float[] src_points = new float[8];
        float[] dst_points = new float[8];
        try{
            boolean arucosAcquired = ArucoDetection.GetArucoLocations(mRgbaModified, work, src_points, dst_points);
            // if this comes back true then call RectifyImage, trigger save dialog
            Log.d("ARUCO", "srcData: " + Arrays.toString(src_points));
            Log.d("ARUCO", "dstData: " + Arrays.toString(dst_points));
            if(arucosAcquired){
                mRgbaTemp.copyTo(mRgba);
                boolean rectified = ArucoDetection.RectifyImage(mRgba, cropped, src_points, dst_points);

                if(rectified){
                    Rect roi = new Rect(456, 2, 76, 76);
                    Mat smallImg = new Mat(cropped, roi);
                    qrText = readQRCode(smallImg);
                    showSaveDialog();
                }
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.d("PADS", "Fudicial Location find exception:" + e.toString());
            e.printStackTrace();
        }


        return mRgbaModified;
    }

    public static String readQRCode(Mat mTwod) {
        Bitmap bMap = Bitmap.createBitmap(mTwod.width(), mTwod.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mTwod, bMap);
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new MultiFormatReader();
        //....doing the actually reading
        Result result = null;
        try {
            result = reader.decode(bitmap);
        } catch (NotFoundException | ChecksumException | FormatException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e("ARUCO", "QR error" + e.getMessage());
            e.printStackTrace();
        }
        if(null != result) {
            Log.i("ARUCO", String.format("QR: %s", result.getText()));

            //return
            return result.getText();
        }else{
            return "";
        }
    }

    public void goHome(View view) {
        mOpenCvCameraView.StopPreview();
        setResult(RESULT_CANCELED, mResultIntent);
        super.finish();
    }

    public void goToSettings(View view) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void decreaseThreshold(View view){
        if(current > 66){
            current -= 2;
        }
        slider.setValue(current);
    }

    public void increaseThreshold(View view){
        if(current < 92){
            current += 2;
        }
        slider.setValue(current);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    public void onDestroy() {
        super.onDestroy();

        //mPreferences.edit().putInt("Threshold", current).apply();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //mPreferences.edit().putInt("Threshold", current).apply();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("PADs", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("PADs", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mRgba = new Mat();
        mRgbaTemp = new Mat();
        Mat testMat = new Mat();
        cropped = new Mat();

        // Parse input template file
        Bitmap tBM = BitmapFactory.decodeStream(getClass().getResourceAsStream("/template.png"));

        // Convert to OpenCV matrix
        Mat tMat = new Mat();
        Utils.bitmapToMat(tBM, tMat);

        // Parse input test file
        Bitmap tBM2 = BitmapFactory.decodeStream(getClass().getResourceAsStream("/test42401.png"));

        // Convert to OpenCV matrix
        Utils.bitmapToMat(tBM2, testMat);

        mTemplate = new Mat();
        Imgproc.cvtColor(tMat, mTemplate, Imgproc.COLOR_BGRA2GRAY);

        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.enableView();
    }

    @Override
    public void finish() {
        mOpenCvCameraView.StopPreview();
        setResult(RESULT_OK, mResultIntent);
        //mPreferences.edit().putInt("Threshold", current).apply();
        super.finish();
    }

    private void CompressOutputs(File[] files, File output) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));

        byte[] buffer = new byte[4096];
        for (File file : files) {
            BufferedInputStream origin = new BufferedInputStream(new FileInputStream(file), buffer.length);

            ZipEntry entry = new ZipEntry(file.getPath().substring(file.getPath().lastIndexOf("/") + 1));
            out.putNextEntry(entry);

            int count;
            while ((count = origin.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, count);
            }

            origin.close();
        }
        out.close();
    }

    public void showSaveDialog() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d("UI thread", "I am the UI thread");

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Fiducials acquired!");
            alert.setMessage("Store PAD image?");
            alert.setPositiveButton("OK",
                    (dialog, which) -> {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                        Date today = Calendar.getInstance().getTime();

                        File imagePath = new File(getFilesDir(), "images");
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
                                    df.format(today), "Origional Image");
                        } catch (Exception e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            Log.i("ContoursOut", "Cannot save to gallery" + e.toString());
                        }

                        Intent intent = getIntent();
                        if (intent != null) {
                            try {
                                File target = new File(padImageDirectory, "compressed.zip");
                                CompressOutputs(new File[]{cFile, oFile}, target);
                                mResultIntent.setData(FileProvider.getUriForFile(this, "edu.nd.crc.paperanalyticaldevices.fileprovider", cFile));
                                mResultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                mResultIntent.putExtra(MainActivity.EXTRA_SAMPLEID, qrText);
                                String timestamp = String.format("%d", Calendar.getInstance().getTimeInMillis());
                                mResultIntent.putExtra(MainActivity.EXTRA_TIMESTAMP, timestamp);
                                finish();
                            } catch (Exception e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                Log.i("ContoursOut", "Cannot compress files: " + e.toString());
                            }
                        }
                        /*Intent intent = getIntent();
                        if(intent != null) {
                            try {
                                //Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
                                File target = new File(padImageDirectory, "compressed.zip");
                                CompressOutputs(new File[]{cFile, oFile}, target);
                                intent.setData(FileProvider.getUriForFile(this, "edu.nd.crc.paperanalyticaldevices.fileprovider", cFile));
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.putExtra(MainActivity.EXTRA_SAMPLEID, qrText);
                                String timestamp = String.format("%d", Calendar.getInstance().getTimeInMillis());
                                //intent.putExtra("timestamp", Calendar.getInstance().getTimeInMillis());
                                intent.putExtra(MainActivity.EXTRA_TIMESTAMP, timestamp);

                                //startActivity(intent);
                                finish();
                            } catch (Exception e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                Log.i("ContoursOut", "Cannot compress files: " + e.toString());
                            }
                        }*/

                    }
            );
            alert.setNegativeButton("Cancel",
                    (dialog, which) -> {
                        //start preview
                        mOpenCvCameraView.StartPreview();

                        dialog.dismiss();

                        ad = null;
                    }
            );
            ad = alert.show();

        });
    }
}