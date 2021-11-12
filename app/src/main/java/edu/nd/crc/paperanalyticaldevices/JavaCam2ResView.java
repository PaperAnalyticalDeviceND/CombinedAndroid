package edu.nd.crc.paperanalyticaldevices;

import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;

import java.util.Arrays;

public class JavaCam2ResView extends JavaCamera2View {

    private boolean mPreviewShowing;
    private FloatingActionButton flashButton;

    public JavaCam2ResView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPreviewShowing = true;
    }


    public void Setup(){
        //get actual screensize if possible
        WindowManager sManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        Point ScreenSize = new Point(getWidth(), getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sManager.getDefaultDisplay().getRealSize(ScreenSize);
        }
        Log.i("ContoursOut", String.format("Screen Size %d, %d.", ScreenSize.x, ScreenSize.y));
        Log.d("PAD", "JavaCam2ResView Setup()");
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        //isTorchOn = prefs.getBoolean("torchon", false);

        //normal initialize
        StopPreview();
        //disconnectCamera();

        //connectCamera(960, 720); 1920x1080, 3840x2160, 1280x720
        //connectCamera(1920, 1080);
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());
        //connectCamera(ScreenSize.x, ScreenSize.y);

        //flashButton = (FloatingActionButton) findViewById(R.id.floatingAnalyze);
        StartPreview();

    }

    public void StopPreview(){

        //Log.d("PAD", "JavaCam2ResView StopPreview()");
        disconnectCamera();
        mPreviewShowing = false;
    }

    public void StartPreview(){
        //Log.d("PAD", "JavaCam2ResView StartPreview()");
        Point ScreenSize = new Point(getWidth(), getHeight());
        connectCamera(ScreenSize.x, ScreenSize.y);
        mPreviewShowing = true;
    }

    /*
    private boolean isTorchOn;

    public void toggleTorch(){


        Log.d("PAD", "toggleTorch()");
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {

            //StopPreview();
            //StartPreview();
            //flashButton = (FloatingActionButton) findViewById(R.id.floatingAnalyze);
            String camList[] = manager.getCameraIdList();
            //CameraDevice cameraDevice = mCaptureSession.getDevice();
            for (String cameraID : camList) {

                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                if ( null != mCaptureSession && null != mCameraDevice
                        && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK && cameraID.equals( mCameraID)) {
                    if (isTorchOn) {


                        //if(cameraDevice.getId().equals(mCameraID)) {
                            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                            isTorchOn = false;
                        //}
                        //flashButton.setImageResource(R.drawable.baseline_flashlight_on_24);
                    } else {
                        //if(cameraDevice.getId().equals(mCameraID)) {
                            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                            isTorchOn = true;
                        //}
                        //flashButton.setImageResource(R.drawable.baseline_flashlight_off_24);
                    }
                    break;
                }
            }

        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }
*/

}
