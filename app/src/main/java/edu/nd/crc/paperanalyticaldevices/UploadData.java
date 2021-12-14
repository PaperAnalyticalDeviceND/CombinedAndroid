package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import androidx.preference.PreferenceManager;
import androidx.work.Data;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UploadData {
    private final Context mContext;
    private String Category, Camera, SampleName, SampleID, Notes, Quantity, Timestamp, OriginalImage, RectifiedImage;

    private UploadData(Context context) {
        mContext = context;
    }

    // Creation Functions
    @NotNull
    public static UploadData from(@NotNull Data input, @NotNull Context context) {
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

    @NotNull
    public static String MD5(@NotNull String s) throws NoSuchAlgorithmException {
        // Create MD5 Hash
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(s.getBytes());
        byte[] messageDigest = digest.digest();

        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            hexString.append(String.format("%1$02X", (0xFF & b)));
        }
        return hexString.toString().toLowerCase();
    }

    public boolean isValid() {
        return Category != null && Camera != null && SampleName != null && SampleID != null && Notes != null && Quantity != null && Timestamp != null && OriginalImage != null && RectifiedImage != null;
    }

    @NotNull
    public Bundle toBundle() {
        final Bundle retVal = new Bundle();
        retVal.putString("category", Category);
        retVal.putString("camera", Camera);
        retVal.putString("sample_name", SampleName);
        retVal.putString("sample_id", SampleID);
        retVal.putString("notes", Notes);
        retVal.putString("quantity", Quantity);
        retVal.putString("timestamp", Timestamp);
        retVal.putString("image_original", OriginalImage);
        retVal.putString("image_rectified", RectifiedImage);
        return retVal;
    }

    @NotNull
    public String toUrlEncoded() throws IOException, NoSuchAlgorithmException {
        StringBuilder sbParams = new StringBuilder();

        sbParams.append("api_key").append("=").append(URLEncoder.encode("D5HDZG76N3ICA3GBUYWC", "UTF-8")).append("&");
        sbParams.append("category_name").append("=").append(URLEncoder.encode(Category, "UTF-8")).append("&");
        sbParams.append("camera1").append("=").append(URLEncoder.encode(Camera, "UTF-8")).append("&");
        sbParams.append("test_name").append("=").append(URLEncoder.encode("12LanePADKenya2015", "UTF-8")).append("&");
        sbParams.append("sample_name").append("=").append(URLEncoder.encode(SampleName, "UTF-8")).append("&");
        sbParams.append("sampleid").append("=").append(URLEncoder.encode(SampleID, "UTF-8")).append("&");
        sbParams.append("notes").append("=").append(URLEncoder.encode(Notes, "UTF-8")).append("&");
        sbParams.append("quantity").append("=").append(URLEncoder.encode(Quantity, "UTF-8")).append("&");
        sbParams.append("file_name").append("=").append(URLEncoder.encode("capture." + Timestamp + ".png", "UTF-8")).append("&");
        sbParams.append("file_name2").append("=").append(URLEncoder.encode("rectified." + Timestamp + ".png", "UTF-8")).append("&");

        String origianlB64 = FileToBase64(mContext, Uri.parse(OriginalImage));
        sbParams.append("uploaded_file").append("=").append(URLEncoder.encode(origianlB64, "UTF-8")).append("&");
        sbParams.append("hash_file1").append("=").append(URLEncoder.encode(UploadData.MD5(origianlB64), "UTF-8")).append("&");

        String rectifiedB64 = FileToBase64(mContext, Uri.parse(RectifiedImage));
        sbParams.append("uploaded_file2").append("=").append(URLEncoder.encode(rectifiedB64, "UTF-8")).append("&");
        sbParams.append("hash_file2").append("=").append(URLEncoder.encode(UploadData.MD5(rectifiedB64), "UTF-8"));

        return sbParams.toString();
    }

    public void CleanupImages() {
        if (OriginalImage != null) {
            File file = new File(new File(OriginalImage).getParent());
            file.delete();
        }

        if (RectifiedImage != null) {
            File file = new File(new File(RectifiedImage).getParent());
            file.delete();
        }
    }
}
