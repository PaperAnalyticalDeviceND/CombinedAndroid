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

    private static final double IMAGE_WIDTH = 1030.0;

    public static boolean GetArucoLocations(Mat mRgbaModified, Mat work, float[] src_points, float[] dst_points, boolean portrait, Mat pointsOrder){
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
        Log.d("ARUCO", "Size: w=" + work.size().width + " h=" + work.size().height);
        // work landscape = w1030 x h2227.0
        // work portrait w720.0 x h960.0

        if (portrait) {
            ratio = (float) mRgbaModified.size().width / (float) IMAGE_WIDTH;
            // portrait is like (w720xh960) 960x720
            Log.d("ARUCO", "Portrait original size: w=" + mRgbaModified.size().width + " h=" + mRgbaModified.size().height);
            //Portrait original size: w=720.0 h=960.0
            //Portrait ratio 1.000000
            // Scale ratio: 0.7475409836065573 Offset: -2.5377049180327376
            //Log.d("ContourDetection", String.format("Portrait ratio %f", ratio)); // ratio = 1.0
        } else {
            ratio = (float) mRgbaModified.size().height / (float) IMAGE_WIDTH;
            //landscape is like (w2336xh1080)
            Log.d("ARUCO", "Landscape original size: w=" + mRgbaModified.size().width + " h=" + mRgbaModified.size().height);
            //Landscape original size: w=2336.0 h=1080.0
            // Landscape ratio 1.500000
            // Scale ratio: 0.85 Offset: 229.82500000000002
            //Log.d("ContourDetection", String.format("Landscape ratio %f", ratio)); // ratio = 1.5
        }

        double horiz_line = 1030 / 2.0; // 515
        double scale_ratio = min(work.size().height / 951, 1.0) * .95; // portrait ~= 0.75
        if (scale_ratio > .85) {
            scale_ratio = 0.85;
        }
//        double scale_offset = ((work.size().height - (1163 * scale_ratio)) / 2) - (64 * scale_ratio); // -4 ?
        //double scale_offset = ((work.size().height - (890 * scale_ratio)) / 2) - (307 * scale_ratio);
        double scale_offset = ((work.size().height - (892 * scale_ratio)) / 2) - (56 * scale_ratio);
        //double scale_offset = 460;
        Log.d("ARUCO", "Scale ratio: " + scale_ratio + " Offset: " + scale_offset);

        // All the coordinates of the markers, upper right corner looking at it horizontally with text at the right,
        // which is actually the upper left corner in its correct orientation
        List<Point> destPoints = new ArrayList<Point>();
        List<Point> targetPoints = new ArrayList<Point>();
        List<Point> guideSquares = new ArrayList<Point>();
        List<Integer> pointsMap = new ArrayList<Integer>();
        // the image
        /*targetPoints.add(new Point(1038, 670)); // marker 0
        targetPoints.add(new Point(186, 670)); // marker 1
        targetPoints.add(new Point(186, 145)); // marker 2
        targetPoints.add(new Point(1038, 145)); // marker 3*/

        // coords for airgap pad 1
//        targetPoints.add(new Point(160, 58)); // marker 0 - 4
//        targetPoints.add(new Point(1010, 58)); // marker 1 - 3
//        targetPoints.add(new Point(1010, 586)); // marker 2 - 8
//        targetPoints.add(new Point(160, 586)); // marker 3 - 0

        //coords for airgap pad 2
        // (115, 173), (728, 173), (728, 659), (115, 659)
        //srcData:
        //         [504.0, 960.0, 514.0, 285.0, 1354.0, 945.0, 1347.0, 293.0]
        //dstData: [584.0, 930.0, 584.0, 285.0, 1400.0, 930.0, 1400.0, 285.0] // found by pasting the airgap2 pad over a preview frame image
//        targetPoints.add(new Point(508, 285)); // marker 0 - 4  - rotated 90ccw 684, 844
//        targetPoints.add(new Point(1350, 285)); // marker 1 - 3 - 684, 68
//        targetPoints.add(new Point(1350, 950)); // marker 2 - 8 - 1300, 68
//        targetPoints.add(new Point(508, 950)); // marker 3 - 0 - 1300, 844
        //List<Integer> f_locs = Arrays.asList(684, 844, 684, 68, 1300, 68, 1300, 844);
        // flip landscape to portrait:  x = (height - y) * ratio, y = (x) * ratio
        // [684, 844] -> [236, 684] scaled -> [157, 455]
        // [684, 68]  -> [1012, 684] scaled -> [674, 455]
        // [1300, 68] -> [1012, 1300] scaled -> [674, 866]
        // [1300, 844] -> [236, 1300] scaled -> [157, 866]
        //List<Integer> f_locs = Arrays.asList(157, 455, 674, 455, 674, 866, 157, 866); // scaled down portrait coords
        // maybe just do transform with markers 4 and 8 (opposite corners)
        // 4 = (153, 228), 8 = (962, 869)
        List<Integer> f_locs = Arrays.asList(153, 228, 962, 228, 962, 869, 153, 869);
        for (int i = 0; i < 4; i++) {
            int x = (int) ((f_locs.get(i * 2) - horiz_line) * scale_ratio + horiz_line - 10); //based on 1030 width artwork
            int y = (int) (f_locs.get(i * 2 + 1) * scale_ratio + scale_offset);
            Log.d("ARUCO", "X: " + x + " Y: " + y);
            // portrait: (199, 337), (585, 337), (585, 644), (199, 644)

            //Point pnt = new Point(x , y );
            Point pnt = new Point(y * ratio, (1030 - x) * ratio);


            if(portrait){
                //pnt = new Point((720 - y), x);
                pnt = new Point(x * ratio, y * ratio);  // this is always the destination point
            }
            guideSquares.add(pnt);

            Point pnt2 = new Point(x * ratio, y * ratio);
            targetPoints.add(pnt2);
        }
//        targetPoints.add(new Point(684, 844)); // marker 0 - 4
//        targetPoints.add(new Point(684, 68)); // marker 1 - 3 -
//        targetPoints.add(new Point(1300, 68)); // marker 2 - 8 -
//        targetPoints.add(new Point(1300, 844)); // marker 3 - 0 -

        //srcData: [1252.0, 61.0, 639.0, 59.0, 637.0, 831.0, 1247.0, 831.0]
        //dstData: [1300.0, 68.0, 684.0, 68.0, 684.0, 844.0, 1300.0, 844.0]  (landscape)
        // rotated for portrait = [1080 - y, x]
        // [1300.0, 68.0] -> [1012, 1300] aruco 8
        // [684.0, 68.0] -> [1012, 684] aruco 3
        // [684.0, 844.0] -> [236, 684] aruco 4
        // [1300.0, 844.0] -> [236, 1300] aruco 0
        // new markers go 4, 3, 8, 0
        pointsMap.add(4);
        pointsMap.add(3);
        pointsMap.add(8);
        pointsMap.add(0);

        // draw some squares as a rough guide
        for(int i=0;  i < 4; i++){
            Log.d("ARUCO", "Drawing: x=" + guideSquares.get(i).x + " y=" + guideSquares.get(i).y);
            Imgproc.rectangle(mRgbaModified, guideSquares.get(i), new Point(guideSquares.get(i).x + 30, guideSquares.get(i).y - 30), new Scalar(0, 0, 255), 3);
//            Imgproc.rectangle(mRgbaModified, targetPoints.get(i), new Point(targetPoints.get(i).x - 30, targetPoints.get(i).y - 30), new Scalar(0, 0, 255), 3);
            //Imgproc.rectangle(mRgbaModified, targetPoints.get(i), new Point(targetPoints.get(i).x - 60, targetPoints.get(i).y + 60), new Scalar(0, 0, 255), 5);
        }


        //add scaling math to QR square coords
        //List<Integer> qr_locs = Arrays.asList(77, 304, 186, 413); // opposing corners
        // or 186,304  77, 413
        // draw an outline of the QR code
        // unscaled portrait coords [278, 458], [119, 618]
        // unscaled landscape coords [458, 798], [616, 955]
        //Imgproc.rectangle(mRgbaModified, new Point(458, 798), new Point(616, 955), new Scalar(0, 25, 250), 2);

        int qr_x1 = (int) ((201 - horiz_line) * scale_ratio + horiz_line - 10);
        int qr_y1 = (int) (35 * scale_ratio + scale_offset);
        int qr_x2 = (int) ((36 - horiz_line) * scale_ratio + horiz_line - 10);
        int qr_y2 = (int) (204 * scale_ratio + scale_offset);

        Point qr_pnt1 = new Point(qr_y1 * ratio, (1030 - qr_x1) * ratio);
        Point qr_pnt2 = new Point(qr_y2 * ratio, (1030 - qr_x2) * ratio);

        if(portrait){
            qr_pnt1 = new Point(qr_x1 * ratio, qr_y1 * ratio);
            qr_pnt2 = new Point(qr_x2 * ratio, qr_y2 * ratio);
        }

        Imgproc.rectangle(mRgbaModified, qr_pnt1, qr_pnt2, new Scalar(0, 25, 250), 2);

        // scaled portrait coords [186, 413], [77, 304]
        // top left [77, 304]
        // bottom right [ 186, 413 ]

        // draw some arrows to match up with the PAD  - previous airgap pad, removed 4-6-26 MJC
        //Imgproc.line(mRgbaModified, new Point(954, 202), new Point(1022, 202), new Scalar(0, 255, 255), 5); // body
        //Imgproc.line(mRgbaModified, new Point(990, 192), new Point(954, 202), new Scalar(0, 255, 255), 5); // head
        //Imgproc.line(mRgbaModified, new Point(990, 210), new Point(954, 202), new Scalar(0, 255, 255), 5);

        //Imgproc.line(mRgbaModified, new Point(90, 202), new Point(158, 202), new Scalar(0, 255, 255), 5); // body
        //Imgproc.line(mRgbaModified, new Point(128, 192), new Point(158, 202), new Scalar(0, 255, 255), 5); // head
        //Imgproc.line(mRgbaModified, new Point(128, 210), new Point(158, 202), new Scalar(0, 255, 255), 5);

        // rotated 90ccw
        // bottom arrow starts 730, 800
        /*  // 6-6-26 MJC, changed to scaled points below
        Imgproc.line(mRgbaModified, new Point(730, 800), new Point(730, 840), new Scalar(0, 255, 255), 5);
        Imgproc.line(mRgbaModified, new Point(730, 800), new Point(710, 833), new Scalar(0, 255, 255), 5);
        Imgproc.line(mRgbaModified, new Point(730, 800), new Point(750, 833), new Scalar(0, 255, 255), 5);
        // top 724, 84
        Imgproc.line(mRgbaModified, new Point(724, 84), new Point(724, 38), new Scalar(0, 255, 255), 5);
        Imgproc.line(mRgbaModified, new Point(724, 84), new Point(704, 54), new Scalar(0, 255, 255), 5);
        Imgproc.line(mRgbaModified, new Point(724, 84), new Point(744, 54), new Scalar(0, 255, 255), 5);
        */

        // each arrow is made of three lines, defined by pairs of points
        int l_tip_x = (int) ((198 - horiz_line) * scale_ratio + horiz_line - 10);
        int l_tip_y = (int) (319 * scale_ratio + scale_offset);
        int r_tip_x = (int) ((943 - horiz_line) * scale_ratio + horiz_line - 10);
        int r_tip_y = (int) (313 * scale_ratio + scale_offset);

        List<Integer> l_tip_point = Arrays.asList(l_tip_x, l_tip_y);
        List<Integer> r_tip_point = Arrays.asList(r_tip_x, r_tip_y);
        List<Integer> l_line_points = Arrays.asList(165, 296, 165, 319, 165, 328);
        List<Integer> r_line_points = Arrays.asList(971, 301, 971, 313, 971, 325);

        Point l_tip = new Point(l_tip_point.get(1) * ratio, (1030 - l_tip_point.get(0)) * ratio);
        Point r_tip = new Point(r_tip_point.get(1) * ratio, (1030 - r_tip_point.get(0)) * ratio);
        if (portrait) {
            l_tip = new Point(l_tip_point.get(0) * ratio, l_tip_point.get(1) * ratio);
            r_tip = new Point(r_tip_point.get(0) * ratio, r_tip_point.get(1) * ratio);
        }

        for (int i = 0; i < 3; i++){
            int l_x = (int) ((l_line_points.get(i * 2) - horiz_line) * scale_ratio + horiz_line - 10); //based on 730 width artwork
            int l_y = (int) (l_line_points.get(i * 2 + 1) * scale_ratio + scale_offset);

            int r_x = (int) ((r_line_points.get(i * 2) - horiz_line) * scale_ratio + horiz_line - 10);
            int r_y = (int) (r_line_points.get(i * 2 + 1) * scale_ratio + scale_offset);

            Point l_pnt = new Point(l_y * ratio, (1030 - l_x) * ratio);
            Point r_pnt = new Point(r_y * ratio, (1030 - r_x) * ratio);

            if(portrait){
                l_pnt = new Point(l_x * ratio, l_y * ratio);
                r_pnt = new Point(r_x * ratio, r_y * ratio);
            }

            Imgproc.line(mRgbaModified, l_tip, l_pnt, new Scalar(0, 255, 255), 3);
            Imgproc.line(mRgbaModified, r_tip, r_pnt, new Scalar(0, 255, 255), 3);
        }

        //List<Integer> crop_coords = Arrays.asList(18, 18, 1012, 937);
        int ul_crop_x = (int) ((0 - horiz_line) * scale_ratio + horiz_line - 10);  // (18-515)*0.75 + 515 -10 = 133
        int ul_crop_y = (int) (0 * scale_ratio + scale_offset); // = 18 * 0.75 + 229.825 = 242
        int lr_crop_x = (int) ((1030 - horiz_line) * scale_ratio + horiz_line - 10);
        int lr_crop_y = (int) (951 * scale_ratio + scale_offset);

        Point ul_crop = new Point(ul_crop_y * ratio, (1030 - ul_crop_x) * ratio);
        Point lr_crop = new Point(lr_crop_y * ratio, (1030 - lr_crop_x) * ratio);

        //Imgproc.rectangle(mRgbaModified, qr_pnt1, qr_pnt2, new Scalar(0, 25, 250), 2);
        Imgproc.rectangle(mRgbaModified, ul_crop, lr_crop, new Scalar(0, 25, 250), 2);

        List<Mat> corners = new ArrayList<Mat>();
        //List<Mat> rejectedImgPoints = new ArrayList<Mat>();
        Mat ids = new Mat();

        //float[] srcData = new float[8];
        float[] target_points = new float[8];

        detector.detectMarkers(work, corners, ids);
        //detector.detectMarkers(mRgbaModified, corners, ids);
        ids.copyTo(pointsOrder);  // output the ids order for later use in cropping the rectified image

        Log.d("PADS", "ArucoDetection: GetArucoLocations: corners.size() = " + corners.size());
        Log.d("PADS", "ArucoDetection: GetArucoLocations: ids = " + ids);

        if(corners.size() > 3) {
            Scalar color = new Scalar(0, 255, 0, 255);
            for(int i = 0; i < corners.size(); i++) {
                int id = Double.valueOf(ids.get(i, 0)[0]).intValue();
                Log.d("ARUCO", "ID: " + id);
                //Log.d("ARUCO", "ID: " + Double.intValue(ids.get(i, 0)[0]));
                //Log.d("PADS", "ArucoDetection: GetArucoLocations: corners.get(" + i + ") = " + corners.get(i));
                Mat corner = corners.get(i);
//                if(!portrait){
//                    Core.transpose(corner, corner);
//                    Core.flip(corner, corner, 1);
//                    corners.set(i, corner);
//                }
                Point p1 = new Point(corner.get(0,0)[0], corner.get(0,0)[1]);
                /*Point p2 = new Point(corner.get(0,1)[0], corner.get(0,1)[1]);
                Point p3 = new Point(corner.get(0,2)[0], corner.get(0,2)[1]);
                Point p4 = new Point(corner.get(0,3)[0], corner.get(0,3)[1]);*/

                int index = pointsMap.indexOf(id);
                // here we map the correct markers to the correct destination points
                src_points[i * 2] = (float) p1.x;
                src_points[i * 2 + 1] = (float) p1.y;
                dst_points[i * 2] = (float) targetPoints.get(index).x;
                dst_points[i * 2 + 1] = (float) targetPoints.get(index).y;
                target_points[i * 2] = (float) targetPoints.get(index).x;
                target_points[i * 2 + 1] = (float) targetPoints.get(index).y;

                Point comDisplay;
                if (portrait) {
                    comDisplay = new Point(p1.x * ratio, p1.y * ratio);
                } else {
                    comDisplay = new Point((p1.y) * ratio, (1030 - p1.x) * ratio);
                }
                Imgproc.circle(mRgbaModified, comDisplay, 10, color, 2, 8, 0);
            }
            Log.d("ARUCO", "Detect srcData: " + Arrays.toString(src_points));
            Log.d("ARUCO", "Detect dstData: " + Arrays.toString(dst_points));
            Imgproc.cvtColor(mRgbaModified, mRgbaModified, Imgproc.COLOR_RGBA2RGB);

            // we need to scale the detected corners back to preview size before drawing them
            //Objdetect.drawDetectedMarkers(mRgbaModified, corners, ids, new Scalar(255, 0, 0));

            for(int j = 0; j < src_points.length; j++){
                float diff = abs(src_points[j] - target_points[j]);
                if(diff > 150.0){
                    Log.d("ARUCO", "Diff: " + diff);
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
        Log.d("Rectify", "Height: " + input.size().height + " Width: " + input.size().width);
        //MatOfPoint2f src = new MatOfPoint2f(sourcePoints);
        //MatOfPoint2f dst = new MatOfPoint2f(destinationpoints);
        double ratio = input.size().height / IMAGE_WIDTH;  // use to calculate the crop corners
        //Mat transform = Imgproc.getAffineTransform(src, dst);
        Mat transform = Imgproc.getPerspectiveTransform(sourcePoints, destinationpoints);
        Log.d("Rectify", "tranform: " + transform.dump());
        //Imgproc.warpAffine(input, output, transform, input.size());
        Imgproc.warpPerspective(input, output, transform, input.size()); // keep it landscape
        // should detect the markers again after transform so we can use them as a reference for cropping
        Dictionary dictionary= Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50);
        DetectorParameters parameters = new DetectorParameters();
        ArucoDetector detector = new ArucoDetector(dictionary, parameters);
        List<Mat> corners = new ArrayList<Mat>();
        Mat ids = new Mat();
        detector.detectMarkers(output, corners, ids);
        Log.d("ARUCO Rectify", "Corners: " + corners.size());
        // (153, 228) id 4, top left ->                           landscape (228, 877)
        // (962, 870) id 8, bottom right, portrait orientation -> landscape (870, 70)
        // use these coords (scaled) to find the edges and crop
        // "top" edge is x-228, "left" edge is y + (1030-877)
        // "right edge is y-70, "bottom" edge is x + (951-870)
        Point ul = null;
        Point lr = null;
        Point ul_crop_corner = null;
        Point lr_crop_corner = null;
        if(corners.size() > 3) {
            for(int i = 0; i < corners.size(); i++) {
                int id = Double.valueOf(ids.get(i, 0)[0]).intValue();
                if(id == 4){
                    Log.d("Rectify", "ID: " + id);
                    Mat corner = corners.get(i);
                    ul = new Point(corner.get(0,0)[0], corner.get(0,0)[1]);
                    //ul_crop_corner = new Point(ul.x - (ratio * 228), ul.y + (ratio * 153));
                    Log.d("Rectify", "Detected UL: " + ul.x + " " + ul.y);
                }
                if(id == 8){
                    Log.d("Rectify", "ID: " + id);
                    Mat corner = corners.get(i);
                    lr = new Point(corner.get(0,0)[0], corner.get(0,0)[1]);
                    //lr_crop_corner = new Point(lr.x + (ratio * 81), lr.y - (ratio * 68));
                    Log.d("Rectify", "Detected LR: " + lr.x + " " + lr.y);
                }
            }
        }else{
            return false;
        }

        // looking at the original in landscape
        float x_dist = (float) abs(lr.x - ul.x); // 871 - 228 = 643
        float y_dist = (float) abs(ul.y - lr.y); // 962 - 153 = 809
        // find the percentage of area needed to pad around the markers to the distance between the marker
        float ul_x_ratio = (float) 228/643;
        float ul_y_ratio = (float) 153/808;
        float lr_x_ratio = (float) 81/643;
        float lr_y_ratio = (float) 68/808;

        ul_crop_corner = new Point(ul.x - (ul_x_ratio * x_dist), ul.y + (ul_y_ratio * y_dist));
        Log.d("Rectify", "UL: " + ul_crop_corner.x + " " + ul_crop_corner.y);
        lr_crop_corner = new Point(lr.x + (lr_x_ratio * x_dist), lr.y - (lr_y_ratio * y_dist));
        Log.d("Rectify", "LR: " + lr_crop_corner.x + " " + lr_crop_corner.y);

        //Imgproc.warpPerspective(input, output, transform, new Size(input.size().height, input.size().width)); // switch to portrait

        // crop out the top and bottom so we just have the area inside the aruco markers

        //Point upperLeft = new Point(464, 0);
        //Point lowerRight = new Point(1373, 994);

        //Point upperLeft = new Point(70, 286);
        //Point lowerRight = new Point(730, 900);

        //Rect cropRect = new Rect(upperLeft, lowerRight);
        Log.d("Rectify", "About to crop");
        if(ul_crop_corner != null && lr_crop_corner != null) {
            Rect cropRect = new Rect(ul_crop_corner, lr_crop_corner);
            Mat imageROI = new Mat(output, cropRect);  //Exception, portrait mode
            //error: (-215:Assertion failed) 0 <= _colRange.start && _colRange.start <= _colRange.end && _colRange.end <= m.cols in function 'Mat'
            imageROI.copyTo(output);
            Log.d("Rectify", "********** CROPPED!!!");
            Imgproc.resize(output, output, new Size((output.size().width * IMAGE_WIDTH) / output.size().height, IMAGE_WIDTH), 0, 0, Imgproc.INTER_LINEAR);

        }else{
            return false;
        }
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