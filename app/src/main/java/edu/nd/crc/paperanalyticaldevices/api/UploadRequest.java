package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.annotations.SerializedName;

public class UploadRequest {
    /*
    {
  "file_name": "string",
  "file_name2": "",
  "test_name": "unknown",
  "category_name": "unknown",
  "camera1": "",
  "camera2": "",
  "notes": "",
  "sample_name": "",
  "sampleid": 0,
  "uploaded_file": "string",
  "uploaded_file2": "",
  "quantity": 100,
  "hash_file1": "",
  "hash_file2": ""
}
     */
    public String toString(){
        return "UploadRequest{" +
                "FileName='" + FileName + '\'' +
                ", FileName2='" + FileName2 + '\'' +
                ", TestName='" + TestName + '\'' +
                ", CategoryName='" + CategoryName + '\'' +
                ", Camera1='" + Camera1 + '\'' +
                ", Camera2='" + Camera2 + '\'' +
                ", Notes='" + Notes + '\'' +
                ", SampleName='" + SampleName + '\'' +
                ", SampleId=" + SampleId +
                //", UploadedFile='" + UploadedFile + '\'' +
                //", UploadedFile2='" + UploadedFile2 + '\'' +
                ", Quantity=" + Quantity +
                ", HashFile1='" + HashFile1 + '\'' +
                ", HashFile2='" + HashFile2 + '\'' +
                '}';
    }

    @SerializedName("file_name")
    public String FileName;

    @SerializedName("file_name2")
    public String FileName2;

    @SerializedName("test_name")
    public String TestName;

    @SerializedName("category_name")
    public String CategoryName;

    @SerializedName("camera1")
    public String Camera1;

    @SerializedName("camera2")
    public String Camera2;

    @SerializedName("notes")
    public String Notes;

    @SerializedName("sample_name")
    public String SampleName;

    @SerializedName("sampleid")
    public int SampleId;

    @SerializedName("uploaded_file")
    public String UploadedFile;

    @SerializedName("uploaded_file2")
    public String UploadedFile2;

    @SerializedName("quantity")
    public int Quantity;

    @SerializedName("hash_file1")
    public String HashFile1;

    @SerializedName("hash_file2")
    public String HashFile2;


}
