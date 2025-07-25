package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.vdurmont.semver4j.Semver;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectV2 {
    @SerializedName("id")
    public int Id;

    @SerializedName("user_name")
    public String UserName;

    @SerializedName("project_name")
    public String ProjectName;

    @SerializedName("annotation")
    public String Annotation;

    @SerializedName("test_name")
    public String TestName;

    @SerializedName("sample_names")
    public SampleNames SampleNames;

    @SerializedName("neutral_filler")
    public String NeutralFiller;

    @SerializedName("qpc20")
    public int Qpc20;

    @SerializedName("qpc50")
    public int Qpc50;

    @SerializedName("qpc80")
    public int Qpc80;

    @SerializedName("qpc100")
    public int Qpc100;

    @SerializedName("notes")
    public String Notes;


}
