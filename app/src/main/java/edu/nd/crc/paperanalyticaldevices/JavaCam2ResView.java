package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.opencv.android.JavaCamera2View;

public class JavaCam2ResView extends JavaCamera2View {

    private boolean mPreviewShowing;
    private FloatingActionButton flashButton;

    public JavaCam2ResView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPreviewShowing = true;
    }

    public void Setup() {
        //get actual screensize if possible
        WindowManager sManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        Point ScreenSize = new Point(getWidth(), getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sManager.getDefaultDisplay().getRealSize(ScreenSize);
        }
        Log.i("ContoursOut", String.format("Screen Size %d, %d.", ScreenSize.x, ScreenSize.y));
        Log.d("PAD", "JavaCam2ResView Setup()");
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());

        StartPreview();
    }

    public void StopPreview() {
        disconnectCamera();
        mPreviewShowing = false;
    }

    public void StartPreview() {
        Point ScreenSize = new Point(getWidth(), getHeight());
        connectCamera(ScreenSize.x, ScreenSize.y);
        mPreviewShowing = true;
    }
}
