package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseList<T> {
    @SerializedName("status")
    public String Status;

    @SerializedName("list")
    public List<T> Entries;
}