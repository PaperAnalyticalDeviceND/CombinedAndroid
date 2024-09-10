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

public class TasksList {

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
    public Links Links;

    @SerializedName("count_pages")
    public Integer CountPages;

    @SerializedName("current_page")
    public Integer CurrentPage;

    @SerializedName("results")
    public ArrayList<Result> Results;

    public class Links {

        @SerializedName("next")
        public String next;

        @SerializedName("previous")
        public String previous;
    }

    public class Result {
        @SerializedName("id")
        public Integer Id;

        @SerializedName("sample")
        public String Sample;

        @SerializedName("manufacturer")
        public DrugManufacturer Manufacturer;

        @SerializedName("dosage")
        public String Dosage;

        @SerializedName("dosage_type")
        public DosageType dosageType;

        @SerializedName("main_apis")
        public List<MainApi> MainAPIs;

        public class DrugManufacturer {
            @SerializedName("id")
            public Integer Id;

            @SerializedName("name")
            public String Name;
        }


        public class DosageType {
            @SerializedName("name")
            public String Name;
        }

        public class MainApi {
            @SerializedName("id")
            public Integer Id;

            @SerializedName("name")
            public String Name;
        }

    }
}




