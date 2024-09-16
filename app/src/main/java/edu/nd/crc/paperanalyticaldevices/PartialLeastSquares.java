package edu.nd.crc.paperanalyticaldevices;

import static org.opencv.imgproc.Imgproc.cvtColor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.opencsv.CSVReader;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class PartialLeastSquares {
    private final List<String> Labels;
    private final List<List<Float>> Coefficients;

    public PartialLeastSquares(final List<String> labels, final List<List<Float>> coefficients){
        Labels = labels;
        Coefficients = coefficients;
    }

    public double calculate(Bitmap bmp, String drug) {
        // find drug index
        int drug_idnx = Labels.indexOf(drug);
        Log.i("GBT-PLS", "drug, " + drug + ", index " + drug_idnx);

        if (drug_idnx < 0) {
            drug_idnx = 0;
        }

        // save results
        double concentration;

        List<double[]> results = new ArrayList<>();

        // load test image from drawable
        Mat BGRMat = new Mat();
        Utils.bitmapToMat(bmp, BGRMat);
        Imgproc.cvtColor(BGRMat, BGRMat, Imgproc.COLOR_RGBA2BGR);

        // CV_8U C3 is type 16
        Log.i("GBT-PLS", "Image load sizes, " + BGRMat.rows() + "," + BGRMat.cols() + "," + BGRMat.type());

        // hsv for intensity calcs
        Mat imgHSV = new Mat();
        cvtColor(BGRMat, imgHSV, Imgproc.COLOR_BGR2HSV);

        // get rectangles to process
        for (int lane = 1; lane < 13; lane++) {
            int laneStart = 17 + (53 * lane) + 12;
            int laneEnd = 17 + (53 * (lane + 1)) - 12;
            for (int region = 0; region < 10; region++) {
                int start = 359;
                int totalLength = 273;
                int regionStart = start + (int) Math.floor((totalLength * region) / 10);
                int regionEnd = start + (int) Math.floor((totalLength * (region + 1)) / 10);

                Rect rroi = new Rect(laneStart, regionStart, laneEnd - laneStart, regionEnd - regionStart);

                Mat roi = new Mat(imgHSV, rroi);
                Mat rgbROI = new Mat(BGRMat, rroi);

                // gt most intense pixels
                Vector<Point> pixels = findMaxIntensitiesFiltered(roi);

                // average over rgb
                double[] rgb = avgPixels(pixels, rgbROI);

                // append
                results.add(rgb);

            }
        }

        // if we are here we should have results
        // get concentration
        concentration = calc_concentration(results, drug_idnx);

        // flag conc.
        Log.i("GBT-PLS", "PLS concentration, " + concentration);

        // return calculated concentration
        return concentration;

    }

    // Takes a HSV image and returns a list of the most intense pixels in it,
    // after applying filtering to minimize black bars on the edges
    private Vector<Point> findMaxIntensitiesFiltered(Mat img) {
        Vector<Point> maxSet = new Vector<>();

        double maxI = 0;
        double centerX = (double) img.rows() / 2.0;
        double centerY = (double) img.cols() / 2.0;

        // loop over x
        for (int i = 0; i < img.rows(); i++) {
            double dX = Math.abs(centerX - i);

            // loop over y
            for (int j = 0; j < img.cols(); j++) {
                double dY = Math.abs(centerY - j);
                double sF = cosCorrectFactor(dX, dY, centerX, centerY);

                double cS = sF * img.get(i, j)[1]; //imgS
                double cV = sF * img.get(i, j)[2]; //imgV

                // black line?
                if (cS <= 35 && cV <= 70) {
                    //continue;
                } else if (cS > maxI) {
                    maxI = (int) img.get(i, j)[1]; //imgS
                    Point point = new Point(i, j);
                    maxSet.clear();
                    maxSet.add(point);
                } else if (cS == maxI) {
                    Point point = new Point(i, j);
                    maxSet.add(point);
                }
            }
        }

        return maxSet;
    }

    // Takes a distance from a center and returns a weight between 0 and 1
    // determined by cosine such that a point at the center has weight 1,
    // and a point at the extremes has weight ~0.
    public double cosCorrectFactor(double dx, double dy, double centerX, double centerY) {
        double relevantD = Math.max((dx / centerX), dy / centerY);
        double relevnatDRads = (Math.PI / 2.0) * relevantD;
        return Math.cos(relevnatDRads);
    }

    // Takes a list of pixels and a BGR image and returns the average
    // RGB pixel values
    private double[] avgPixels(Vector<Point> pixels, Mat img) {
        double totalB = 0;
        double totalG = 0;
        double totalR = 0;

        //  accumulate pixels
        for (int pixeli = 0; pixeli < pixels.size(); pixeli++) {
            int x = (int) pixels.get(pixeli).x;
            int y = (int) pixels.get(pixeli).y;
            double[] bgr = img.get(x, y);
            totalB += bgr[0];
            totalG += bgr[1];
            totalR += bgr[2];
        }

        // normalize if values exist
        if (pixels.size() != 0) {
            totalB /= pixels.size();
            totalG /= pixels.size();
            totalR /= pixels.size();
        }

        // return averages
        return new double[]{totalR, totalG, totalB};
    }

    // calculate concentration using PLSR
    private double calc_concentration(List<double[]> results, int drug_idnx) {
        double conc;

        // get coefficients to use
        List<Float> drug_coeffs = Coefficients.get(drug_idnx);

        // first coeff is intercept
        conc = drug_coeffs.get(0);

        // do the pls coeff multiply
        for (int i = 0; i < results.size(); i++) {
            for (int j = 0; j < 3; j++) {
                conc += results.get(i)[j] * drug_coeffs.get(i * 3 + j + 1);
            }
        }

        // return concentration
        return conc;
    }

    public static PartialLeastSquares from(Context context) throws IOException {
        List<String> labels = new ArrayList<>();
        List<List<Float>> coefficients = new ArrayList<>();

        String plsFilename = "pls_coefficients.csv";  // default to the included file
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String storedPLSModel = preferences.getString("plsmodel", "none");

        if(storedPLSModel != "none"){
            plsFilename = preferences.getString(storedPLSModel + "filename", "none");
            Log.d("PLS", "Using stored PLS model: " + storedPLSModel + " with filename: " + plsFilename);
            File plsFile = new File(context.getDir("tflitemodels", Context.MODE_PRIVATE).getPath(), plsFilename);
            //try(FileInputStream input = new FileInputStream(plsFile)
            try( InputStreamReader reader = new InputStreamReader(new FileInputStream(plsFile), StandardCharsets.UTF_8)) {
                CSVReader csv = new CSVReader(reader);
                for( String[] record: csv){
                    labels.add(record[0]);
                    coefficients.add(Arrays.stream(record).skip(1).map(s -> Float.parseFloat(s)).collect(Collectors.toList()));
                }
            }
        }else{
            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open(plsFilename), StandardCharsets.UTF_8)) {
                CSVReader csv = new CSVReader(reader);
                for( String[] record: csv){
                    labels.add(record[0]);
                    coefficients.add(Arrays.stream(record).skip(1).map(s -> Float.parseFloat(s)).collect(Collectors.toList()));
                }
            }
        }

        return new PartialLeastSquares(labels, coefficients);
    }
}