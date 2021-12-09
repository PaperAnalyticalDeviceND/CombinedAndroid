package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.JavaCamera2View;

public class JavaCam2ResView extends JavaCamera2View {
    public JavaCam2ResView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void Setup() {
        //get actual screensize if possible
        WindowManager sManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        Point ScreenSize = new Point(getWidth(), getHeight());
        sManager.getDefaultDisplay().getRealSize(ScreenSize);
        Log.i("ContoursOut", String.format("Screen Size %d, %d.", ScreenSize.x, ScreenSize.y));
        Log.d("PAD", "JavaCam2ResView Setup()");
        Log.d("Preview", "Width" + getWidth() + ", " + getHeight());

        StartPreview();
    }

    public void StopPreview() {
        disconnectCamera();
    }

    public void StartPreview() {
        Point ScreenSize = new Point(getWidth(), getHeight());
        connectCamera(ScreenSize.x, ScreenSize.y);
    }
}
