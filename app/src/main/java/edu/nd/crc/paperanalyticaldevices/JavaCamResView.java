package edu.nd.crc.paperanalyticaldevices;

import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.JavaCameraView;

import java.util.List;

public class JavaCamResView extends JavaCameraView {
    private boolean mPreviewShowing;

    public JavaCamResView(Context context, AttributeSet attrs) {
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

        //normal initialize
        StopPreview();
        disconnectCamera();

        //connectCamera(960, 720); 1920x1080, 3840x2160, 1280x720
        //connectCamera(1920, 1080);
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());
        connectCamera(ScreenSize.x, ScreenSize.y);

        Camera.Parameters params = this.mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if(null != flashModes && !flashModes.isEmpty() && flashModes.contains(FLASH_MODE_TORCH)) {
            params.setFlashMode(FLASH_MODE_TORCH);
        }
        List<String> focusModes = params.getSupportedFocusModes();
        if(null != focusModes && focusModes.contains(FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        //params.setPreviewSize(3840, 2160);
        this.mCamera.setParameters(params);
        StartPreview();
    }

    public void togglePreview(){
        if(mPreviewShowing){
            Log.d("Preview", "Stop");
            StopPreview();
        }else{
            Log.d("Preview", "Start");
            StartPreview();
        }
    }

    public void StopPreview(){
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mPreviewShowing = false;
    }

    public void StartPreview(){
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        mPreviewShowing = true;
    }
}