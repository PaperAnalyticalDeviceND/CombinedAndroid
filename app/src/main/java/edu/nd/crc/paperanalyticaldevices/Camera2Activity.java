package edu.nd.crc.paperanalyticaldevices;

import static android.Manifest.permission.CAMERA;
import static java.lang.Math.sqrt;

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

public class Camera2Activity extends Activity implements CvCameraViewListener2 {
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
    int min = 50, max = 90, current = 125;
    private Slider slider = null;

    SharedPreferences mPreferences = null;

    //UI
    private final Intent mResultIntent = new Intent();

    public Camera2Activity() {
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
            Log.i("ContoursOut", "QR error" + e.toString());
            e.printStackTrace();
        }
        if(null != result) {
            Log.i("ContoursOut", String.format("QR: %s", result.getText()));

            //return
            return result.getText();
        }else{
            return "";
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 91);

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);

        thresholdText = findViewById(R.id.threshold_text);

        mPreferences = getSharedPreferences(MainActivity.PROJECT, MODE_PRIVATE);

        current = mPreferences.getInt("Threshold", 75);

        /*
        thresholdSeekBar = findViewById(R.id.threshold_seek_bar);
        thresholdSeekBar.setMin(min);
        thresholdSeekBar.setMax(max);
        thresholdSeekBar.setProgress(max - min);
        thresholdSeekBar.setProgress(current - min);

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current = progress + min;
                //textView.setText("" + current);
                thresholdText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        */

        slider = findViewById(R.id.slider);
        slider.setValue(current);

        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                //thresholdText.setText("" + value);
                current = (int) value;
            }
        });

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
    public void onPause() {
        super.onPause();

        mPreferences.edit().putInt("Threshold", current).apply();

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

    public void goHome(View view) {
        mOpenCvCameraView.StopPreview();
        setResult(RESULT_CANCELED, mResultIntent);
        super.finish();
    }

    public void goToSettings(View view) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void onDestroy() {
        super.onDestroy();

        mPreferences.edit().putInt("Threshold", current).apply();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void finish() {
        mOpenCvCameraView.StopPreview();
        setResult(RESULT_OK, mResultIntent);
        mPreferences.edit().putInt("Threshold", current).apply();
        super.finish();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.d("PAD", "onCameraViewStarted()");
        mOpenCvCameraView.Setup();
    }

    public void onCameraViewStopped() {
        Log.d("PAD", "onCameraViewStopped()");
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.StopPreview();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat mRgbaModified = inputFrame.rgba();

        //waiting on dialog?
        if (ad != null) return mRgbaModified;

        mRgbaModified.copyTo(mRgbaTemp);

        boolean portrait = true;
        Mat work = new Mat();
        float ratio;

        if (mRgbaModified.size().height > mRgbaModified.size().width) {
            Imgproc.resize(inputFrame.gray(), work, new Size(IMAGE_WIDTH, (mRgbaModified.size().height * IMAGE_WIDTH) / mRgbaModified.size().width), 0, 0, Imgproc.INTER_LINEAR);
            ratio = (float) mRgbaModified.size().width / (float) IMAGE_WIDTH;
        } else {
            portrait = false;
            Imgproc.resize(inputFrame.gray(), work, new Size((mRgbaModified.size().width * IMAGE_WIDTH) / mRgbaModified.size().height, IMAGE_WIDTH), 0, 0, Imgproc.INTER_LINEAR);
            Core.transpose(work, work);
            Core.flip(work, work, 1);
            ratio = (float) mRgbaModified.size().height / (float) IMAGE_WIDTH;
        }

        //create source points
        List<Point> src_points = new Vector<>();

        //Look for fiducials
        try {
            boolean fiducialsAcquired = ContourDetection.GetFudicialLocations(mRgbaModified, work, src_points, portrait, (current));

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
                String qr_data;
                try {
                    qr_data = readQRCode(smallImg);
                    if (qr_data.startsWith("padproject.nd.edu/?s=")) {
                        pad_version = 10;
                        pad_index = 0;
                    } else if (qr_data.startsWith("padproject.nd.edu/?t=")) {
                        pad_version = 20;
                        pad_index = 1;
                    }
                    qrText = qr_data;
                } catch (Exception e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Log.i("ContoursOut", "QR error: " + e.toString());
                }

                if (pad_version != 0) {
                    Log.i("ContoursOut", "Version " + pad_version);
                    //save successful frame
                    mRgbaTemp.copyTo(mRgba);

                    //flag saved

                    // rectify image, include QR/Fiducial points
                    float[][] dest_points = {{85, 1163, 686, 1163, 686, 77, 244, 64, 82, 64, 82, 226}, {85, 1163, 686, 1163, 686, 77, 255, 64, 82, 64, 82, 237}};

                    //Note: sending color corrected image to rectifyer
                    boolean transformedOk = ContourDetection.RectifyImage(mRgba, mTemplate, cropped, src_points, dest_points[pad_index]);

                    //error?
                    if (transformedOk) {
                        Log.i("ContoursOut", "Transformed correctly");

                        //ask if we want to save data for email, also block updates until done.
                        showSaveDialog();
                    } else {
                        Log.d("ControusOut", "Transform Failed.");
                    }
                }
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.d("PADS", "Fudicial Location find exception:" + e.toString());
            e.printStackTrace();
        }

        return mRgbaModified;
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

    /**
     * Show dialog to save data
     */
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
                        if (intent != null /*&& intent.getData() != null */) {
                            try {
                                File target = new File(padImageDirectory, "compressed.zip");
                                CompressOutputs(new File[]{cFile, oFile}, target);
                                mResultIntent.setData(FileProvider.getUriForFile(this, "edu.nd.crc.paperanalyticaldevices.fileprovider", target));
                                mResultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                mResultIntent.putExtra("qr", qrText);
                                mResultIntent.putExtra("timestamp", Calendar.getInstance().getTimeInMillis());
                                finish();
                            } catch (Exception e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                Log.i("ContoursOut", "Cannot compress files: " + e.toString());
                            }
                        } else {
                            Log.i("ContoursOut", cFile.getPath());

                            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            i.setType("message/rfc822");
                            i.setType("application/image");
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mcurran2@nd.edu"});
                            i.putExtra(Intent.EXTRA_SUBJECT, "PADs");
                            i.putExtra(Intent.EXTRA_TEXT, "Pad image (" + qrText + ")");
                            ArrayList<Uri> uris = new ArrayList<>();
                            uris.add(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(cFile.getPath())));
                            uris.add(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", new File(oFile.getPath())));
                            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            try {
                                startActivity(i);
                            } catch (android.content.ActivityNotFoundException ex) {
                                FirebaseCrashlytics.getInstance().recordException(ex);
                                Log.i("ContoursOut", "There are no email clients installed.");
                            }

                            //start preview
                            mOpenCvCameraView.StartPreview();

                            dialog.dismiss();

                            ad = null;
                        }
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

    public static class PredictionGuess implements Comparable<PredictionGuess> {
        public int Index;
        public float Confidence;
        public int NetIndex;

        public PredictionGuess(int i, float c, int j) {
            Index = i;
            Confidence = c;
            NetIndex = j;
        }

        @Override
        public int compareTo(PredictionGuess that) {
            if (Confidence > that.Confidence) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
