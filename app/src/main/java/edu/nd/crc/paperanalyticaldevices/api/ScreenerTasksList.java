package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.vdurmont.semver4j.Semver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScreenerTasksList {

    public static class StringListDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Arrays.stream(json.getAsJsonPrimitive().getAsString().split(",")).collect(Collectors.toList());
        }
    }

    public static class SemverDeserializer implements JsonDeserializer<Semver> {
        @Override
        public Semver deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Semver(json.getAsJsonPrimitive().getAsString(), Semver.SemverType.LOOSE);
        }
    }

    @SerializedName("links")
    public TasksList.Links Links;

    @SerializedName("count_pages")
    public Integer CountPages;

    @SerializedName("current_page")
    public Integer CurrentPage;

    @SerializedName("results")
    public ArrayList<ScreenerResult> Results;

    public class ScreenerResult {

        public String toString(){
            return Id + " " + Sample + " " + Name;
        }
        @SerializedName("id")
        public Integer Id;

        @SerializedName("sample")
        public String Sample;

        @SerializedName("dosage_type")
        public TasksList.Result.DosageType dosageType;

        @SerializedName("name")
        public String Name;

        @SerializedName("expected_main_substances")
        public String ExpectedMainSubstances;

        @SerializedName("expected_other_substances")
        public String ExpectedOtherSubstances;
    }
}
