package edu.nd.crc.paperanalyticaldevices;

import static com.google.common.primitives.Doubles.min;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.Objdetect;
import org.opencv.objdetect.ArucoDetector;
import org.opencv.objdetect.DetectorParameters;
import org.opencv.objdetect.Dictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
public class ArucoDetection {

    private static final double IMAGE_WIDTH = 720.0;

    public static boolean GetArucoLocations(Mat mRgbaModified, Mat work, float[] src_points, float[] dst_points){
//public static boolean GetArucoLocations(Mat mRgbaModified, Mat work, List<Point> src_points)
        Dictionary dictionary= Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50);
        DetectorParameters parameters = new DetectorParameters();
        ArucoDetector detector = new ArucoDetector(dictionary, parameters);

        float ratio;

        /*if (portrait) {
            ratio = (float) mRgbaModified.size().width / (float) IMAGE_WIDTH;
        } else {
            ratio = (float) mRgbaModified.size().height / (float) IMAGE_WIDTH;
        }*/

        double horiz_line = 730.0 / 2.0;
        double scale_ratio = min(work.size().height / 1220, 1.0) * .95;
        if (scale_ratio > .85) {
            scale_ratio = 0.85;
        }
        double scale_offset = ((work.size().height - (1163 * scale_ratio)) / 2) - (64 * scale_ratio);

        // All the coordinates of the markers, upper right corner looking at it horizontally with text at the right,
        // which is actually the upper left corner in its correct orientation
        List<Point> destPoints = new ArrayList<Point>();
        List<Point> targetPoints = new ArrayList<Point>();
        // the image
        /*targetPoints.add(new Point(1038, 670)); // marker 0
        targetPoints.add(new Point(186, 670)); // marker 1
        targetPoints.add(new Point(186, 145)); // marker 2
        targetPoints.add(new Point(1038, 145)); // marker 3*/

        targetPoints.add(new Point(160, 58)); // marker 0
        targetPoints.add(new Point(1010, 58)); // marker 1
        targetPoints.add(new Point(1010, 586)); // marker 2
        targetPoints.add(new Point(160, 586)); // marker 3

        // draw some squares as a rough guide
        for(int i=0;  i < 4; i++){
            Imgproc.rectangle(mRgbaModified, targetPoints.get(i), new Point(targetPoints.get(i).x - 80, targetPoints.get(i).y + 80), new Scalar(0, 0, 255), 5);
        }
        // draw some arrows to match up with the PAD
        Imgproc.line(mRgbaModified, new Point(954, 202), new Point(1022, 202), new Scalar(0, 255, 255), 5); // body
        Imgproc.line(mRgbaModified, new Point(990, 192), new Point(954, 202), new Scalar(0, 255, 255), 5); // head
        Imgproc.line(mRgbaModified, new Point(990, 210), new Point(954, 202), new Scalar(0, 255, 255), 5);

        Imgproc.line(mRgbaModified, new Point(90, 202), new Point(158, 202), new Scalar(0, 255, 255), 5); // body
        Imgproc.line(mRgbaModified, new Point(128, 192), new Point(158, 202), new Scalar(0, 255, 255), 5); // head
        Imgproc.line(mRgbaModified, new Point(128, 210), new Point(158, 202), new Scalar(0, 255, 255), 5);

        /*destPoints.add(new Point(1036, 604)); // marker 0
        destPoints.add(new Point(186, 604)); // marker 1
        destPoints.add(new Point(186, 78)); // marker 2
        destPoints.add(new Point(1036, 78)); // marker 3*/
        // transport to vertical
        /*destPoints.add(new Point(60, 1040)); // marker 0
        destPoints.add(new Point(60, 187)); // marker 1
        destPoints.add(new Point(588, 187)); // marker 2
        destPoints.add(new Point(588, 1040)); // marker 3*/

        List<Mat> corners = new ArrayList<Mat>();
        //List<Mat> rejectedImgPoints = new ArrayList<Mat>();
        Mat ids = new Mat();

        //float[] srcData = new float[8];
        float[] target_points = new float[8];

        detector.detectMarkers(work, corners, ids);
        //detector.detectMarkers(mRgbaModified, corners, ids);

        Log.d("PADS", "ArucoDetection: GetArucoLocations: corners.size() = " + corners.size());
        Log.d("PADS", "ArucoDetection: GetArucoLocations: ids = " + ids);

        if(corners.size() > 3) {

            for(int i = 0; i < corners.size(); i++) {
                int id = Double.valueOf(ids.get(i, 0)[0]).intValue();
                //Log.d("ARUCO", "ID: " + id);
                //Log.d("ARUCO", "ID: " + Double.intValue(ids.get(i, 0)[0]));
                //Log.d("PADS", "ArucoDetection: GetArucoLocations: corners.get(" + i + ") = " + corners.get(i));
                Mat corner = corners.get(i);
                Point p1 = new Point(corner.get(0,0)[0], corner.get(0,0)[1]);
                /*Point p2 = new Point(corner.get(0,1)[0], corner.get(0,1)[1]);
                Point p3 = new Point(corner.get(0,2)[0], corner.get(0,2)[1]);
                Point p4 = new Point(corner.get(0,3)[0], corner.get(0,3)[1]);*/
                //Log.d("ARUCO", "Point1: " + p1);
                /*Log.d("ARUCO", "Point2: " + p2);
                Log.d("ARUCO", "Point3: " + p3);
                Log.d("ARUCO", "Point4: " + p4);*/

                // here we map the correct markers to the correct destination points
                src_points[i * 2] = (float) p1.x;
                src_points[i * 2 + 1] = (float) p1.y;
                dst_points[i * 2] = (float) targetPoints.get(id).x;
                dst_points[i * 2 + 1] = (float) targetPoints.get(id).y;
                target_points[i * 2] = (float) targetPoints.get(id).x;
                target_points[i * 2 + 1] = (float) targetPoints.get(id).y;
            }
            Log.d("ARUCO", "Detect srcData: " + Arrays.toString(src_points));
            Log.d("ARUCO", "Detect dstData: " + Arrays.toString(dst_points));
            Imgproc.cvtColor(mRgbaModified, mRgbaModified, Imgproc.COLOR_RGBA2RGB);

            Objdetect.drawDetectedMarkers(mRgbaModified, corners, ids, new Scalar(255, 0, 0));

            for(int j = 0; j < src_points.length; j++){
                float diff = abs(src_points[j] - target_points[j]);
                if(diff > 150.0){
                    return false;
                }
            }
            //boolean rectified = RectifyImage(mRgbaModified, srcData, dstData);
            //src_points = srcData;
            return true;
        }
        return false;
    }

    public static boolean RectifyImage(Mat input, Mat output, float[] src_points, float[] dest_points){

        Mat destinationpoints = new Mat(4, 2, CvType.CV_32F);
        Mat sourcePoints = new Mat(4, 2, CvType.CV_32F);
        destinationpoints.put(0, 0, dest_points);
        sourcePoints.put(0, 0, src_points);

        //MatOfPoint2f src = new MatOfPoint2f(sourcePoints);
        //MatOfPoint2f dst = new MatOfPoint2f(destinationpoints);

        //Mat transform = Imgproc.getAffineTransform(src, dst);
        Mat transform = Imgproc.getPerspectiveTransform(sourcePoints, destinationpoints);
        Log.d("ARUCO", "tranform: " + transform.dump());
        //Imgproc.warpAffine(input, input, transform, input.size());
        Imgproc.warpPerspective(input, output, transform, input.size()); // keep it landscape
        //Imgproc.warpPerspective(input, output, transform, new Size(input.size().height, input.size().width)); // switch to portrait

        // crop out the top and bottom so we just have the area inside the aruco markers
        /*Point upperLeft = new Point(54, 117);
        Point lowerRight = new Point(660, 1046);*/
        Point upperLeft = new Point(155, 54);
        Point lowerRight = new Point(1078, 656);
        Rect cropRect = new Rect(upperLeft, lowerRight);
        Mat imageROI = new Mat(output, cropRect);
        imageROI.copyTo(output);

        /*for (int i = 0; i < 7; i++) {
            double px = 664 - (82 * i);
            Imgproc.line(output, new Point(px, 1), new Point(px, 601), new Scalar(0, 255, 0), 2);
        }*/

        return true;
    }
}
/*
0  ID: 1.0
1  ID: 0.0
2  ID: 2.0
3  ID: 3.0
  ArucoDetection: GetArucoLocations: corners.get(0) = Mat [ 1*4*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0xdd5f86c0, dataAddr=0xcf3cf940 ]
  Point1: {333.0, 672.0}
  ArucoDetection: GetArucoLocations: corners.get(1) = Mat [ 1*4*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0xcf948910, dataAddr=0xcf3cfcc0 ]
  Point1: {1137.0, 653.0}
  ArucoDetection: GetArucoLocations: corners.get(2) = Mat [ 1*4*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0xcf9497b8, dataAddr=0xcf3cfd80 ]
  Point1: {332.0, 153.0}
  ArucoDetection: GetArucoLocations: corners.get(3) = Mat [ 1*4*CV_32FC2, isCont=true, isSubmat=false, nativeObj=0xcf94ad98, dataAddr=0xd1344800 ]
  Point1: {1137.0, 166.0}
 */