package edu.nd.crc.paperanalyticaldevices.api;

import android.util.Log;

import com.google.gson.JsonArray;
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

public class NetworkV2 {

    @SerializedName("id")
    public int Id;

    @SerializedName("name")
    public String Name;

    @SerializedName("drugs_size")
    public int DrugsSize;

    @SerializedName("drugs")
    public List<String> Drugs;

    @SerializedName("labels")
    public List<String> Labels;

    @SerializedName("lanes_excluded")
    public String LanesExcluded;

    @SerializedName("weights_url")
    public String Weights;

    @SerializedName("image_size")
    public String ImageSize;

    @SerializedName("brightness")
    public Float Brightness;

    @SerializedName("type")
    public String Type;

    @SerializedName("description")
    public String Description;

    @SerializedName("test")
    public String Test;

    @SerializedName("version")
    public Semver Version;

    @SerializedName("SHA256")
    public String Hash;

    public static class StringListDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonArray jsonArray = json.getAsJsonArray();
            ArrayList<String> returnArray = new ArrayList<>();
            for(JsonElement element : jsonArray) {
                //Log.d("NetworkV2", element.getAsString());
                returnArray.add(element.getAsString());
            }
            return returnArray;
        }
    }

}
