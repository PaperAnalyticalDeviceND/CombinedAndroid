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
import java.util.HashMap;
import java.util.Map;

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

    public static Map<String, String> asMap(@NotNull Data input, @NotNull Context context) throws Exception {
        final String origianlB64 = FileToBase64(context, Uri.parse(input.getString("ORIGINAL_IMAGE")));
        final String rectifiedB64 = FileToBase64(context, Uri.parse(input.getString("RECTIFIED_IMAGE")));

        Map<String, String> retVal = new HashMap<>();
        retVal.put("api_key", "5NWT4K7IS60WMLR3J2LV");
        retVal.put("category_name", PreferenceManager.getDefaultSharedPreferences(context).getString("project", "JCSTest"));
        retVal.put("camera1", Build.MANUFACTURER + " " + Build.MODEL);
        retVal.put("test_name", "12LanePADKenya2015");
        retVal.put("sample_name", input.getString("SAMPLE_NAME"));
        retVal.put("sampleid", input.getString("SAMPLE_ID"));
        retVal.put("notes", input.getString("NOTES"));
        retVal.put("quantity", input.getString("QUANTITY"));
        retVal.put("file_name", "capture." + input.getString("TIMESTAMP") + ".png");
        retVal.put("file_name2", "rectified." + input.getString("TIMESTAMP") + ".png");
        retVal.put("uploaded_file", origianlB64);
        retVal.put("hash_file1", UploadData.MD5(origianlB64));
        retVal.put("uploaded_file2", rectifiedB64);
        retVal.put("hash_file2", UploadData.MD5(rectifiedB64));

        if (retVal.containsValue(null)) throw new Exception("Invalid or missing data");

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
