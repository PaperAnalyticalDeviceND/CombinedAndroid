package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import androidx.preference.PreferenceManager;
import androidx.work.Data;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UploadData {
    protected Context mContext;
    protected String Category, Camera, SampleName, SampleID, Notes, Quantity, Timestamp, OriginalImage, RectifiedImage;

    public UploadData(Context context){
        mContext = context;
    }

    public boolean isValid(){
        return Category != null && Camera != null && SampleName != null && SampleID != null && Notes != null && Quantity != null && Timestamp != null && OriginalImage != null && RectifiedImage != null;
    }

    public Bundle toBundle(){
        Bundle retVal = new Bundle();
        retVal.putString("category", this.Category);
        retVal.putString("camera", this.Camera);
        retVal.putString("sample_name", this.SampleName);
        retVal.putString("sample_id", this.SampleID);
        retVal.putString("notes", this.Notes);
        retVal.putString("quantity", this.Quantity);
        retVal.putString("timestamp", this.Timestamp);
        retVal.putString("image_original", this.OriginalImage);
        retVal.putString("image_rectified", this.RectifiedImage);
        return retVal;
    }

    public String toUrlEncoded() throws IOException, NoSuchAlgorithmException {
        StringBuilder sbParams = new StringBuilder();

        sbParams.append("api_key").append("=").append(URLEncoder.encode("D5HDZG76N3ICA3GBUYWC", "UTF-8")).append("&");
        sbParams.append("category_name").append("=").append(URLEncoder.encode(this.Category, "UTF-8")).append("&");
        sbParams.append("camera1").append("=").append(URLEncoder.encode(this.Camera, "UTF-8")).append("&");
        sbParams.append("test_name").append("=").append(URLEncoder.encode("12LanePADKenya2015", "UTF-8")).append("&");
        sbParams.append("sample_name").append("=").append(URLEncoder.encode(this.SampleName, "UTF-8")).append("&");
        sbParams.append("sampleid").append("=").append(URLEncoder.encode(this.SampleID, "UTF-8")).append("&");
        sbParams.append("notes").append("=").append(URLEncoder.encode(this.Notes, "UTF-8")).append("&");
        sbParams.append("quantity").append("=").append(URLEncoder.encode(this.Quantity, "UTF-8")).append("&");
        sbParams.append("file_name").append("=").append(URLEncoder.encode("capture." + this.Timestamp + ".png", "UTF-8")).append("&");
        sbParams.append("file_name2").append("=").append(URLEncoder.encode("rectified." + this.Timestamp + ".png", "UTF-8")).append("&");

        String origianlB64 = FileToBase64(mContext, Uri.parse(this.OriginalImage));
        sbParams.append("uploaded_file").append("=").append(URLEncoder.encode(origianlB64, "UTF-8")).append("&");
        sbParams.append("hash_file1").append("=").append(URLEncoder.encode(UploadData.MD5(origianlB64), "UTF-8")).append("&");

        String rectifiedB64 = FileToBase64(mContext, Uri.parse(this.RectifiedImage));
        sbParams.append("uploaded_file2").append("=").append(URLEncoder.encode(rectifiedB64, "UTF-8")).append("&");
        sbParams.append("hash_file2").append("=").append(URLEncoder.encode(UploadData.MD5(rectifiedB64), "UTF-8"));

        return sbParams.toString();
    }

    public void CleanupImages() {
        if( this.OriginalImage != null ){
            File file = new File(new File(this.OriginalImage).getParent());
            file.delete();
        }

        if( this.RectifiedImage != null ){
            File file = new File(new File(this.RectifiedImage).getParent());
            file.delete();
        }
    }

    // Creation Functions
    public static UploadData from(final Data input, final Context context) {
        UploadData retVal = new UploadData(context);
        retVal.Category = PreferenceManager.getDefaultSharedPreferences(context).getString("project", "JCSTest");
        retVal.Camera = Build.MANUFACTURER + " " + Build.MODEL;
        retVal.SampleName = input.getString("SAMPLE_NAME");
        retVal.SampleID = input.getString("SAMPLE_ID");
        retVal.Notes = input.getString("NOTES");
        retVal.Quantity = input.getString("QUANTITY");
        retVal.Timestamp = input.getString("TIMESTAMP");
        retVal.OriginalImage = input.getString("ORIGINAL_IMAGE");
        retVal.RectifiedImage = input.getString("RECTIFIED_IMAGE");
        return retVal;
    }

    // Helper functions
    private static String FileToBase64(final Context ctx, final Uri path) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream inputStream = ctx.getContentResolver().openInputStream(path);

        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

        byte[] bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static String MD5(final String s) throws NoSuchAlgorithmException {
        // Create MD5 Hash
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(s.getBytes());
        byte messageDigest[] = digest.digest();

        // Create Hex String
        StringBuffer hexString = new StringBuffer();
        for (int i=0; i<messageDigest.length; i++) {
            hexString.append(String.format("%1$02X",(0xFF & messageDigest[i])));
        }
        return hexString.toString().toLowerCase();
    }
}
