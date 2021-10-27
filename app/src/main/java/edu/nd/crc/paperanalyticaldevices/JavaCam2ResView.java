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

import org.opencv.android.JavaCamera2View;

public class JavaCam2ResView extends JavaCamera2View {

    private boolean mPreviewShowing;

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

        //normal initialize
        StopPreview();
        disconnectCamera();

        //connectCamera(960, 720); 1920x1080, 3840x2160, 1280x720
        //connectCamera(1920, 1080);
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());
        connectCamera(ScreenSize.x, ScreenSize.y);

        //Camera.Parameters params = this.mCamera.getParameters();
        //params.setFlashMode(FLASH_MODE_TORCH);
        //params.setFocusMode(FOCUS_MODE_CONTINUOUS_VIDEO);
        //params.setPreviewSize(3840, 2160);
        //this.mCamera.setParameters(params);
        StartPreview();
    }

    public void StopPreview(){
        //mCameraDevice.stopPreview();
        //mCameraDevice.setPreviewCallback(null);
        disconnectCamera();
        mPreviewShowing = false;
    }

    public void StartPreview(){
        //mCamera.startPreview();
        //mCamera.setPreviewCallback(this);
        //connectCamera()

        initializeCamera();
        mPreviewShowing = true;
    }
}
