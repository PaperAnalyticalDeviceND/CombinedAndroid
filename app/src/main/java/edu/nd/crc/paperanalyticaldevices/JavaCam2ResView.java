package edu.nd.crc.paperanalyticaldevices;

import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import org.opencv.android.JavaCamera2View;

import java.util.Arrays;

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
        //disconnectCamera();

        //connectCamera(960, 720); 1920x1080, 3840x2160, 1280x720
        //connectCamera(1920, 1080);
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());
        //connectCamera(ScreenSize.x, ScreenSize.y);

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


}
