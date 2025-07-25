package edu.nd.crc.paperanalyticaldevices.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SampleNames {
    @SerializedName("sample_names")
    public List<String> SampleNames;

    public static class SampleNamesDeserializer implements JsonDeserializer<SampleNames> {
        @Override
        public SampleNames deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            SampleNames sampleNames = new SampleNames();
            sampleNames.SampleNames = new ArrayList<>();

            Log.d("SampleNames", obj.toString());
            for(String key : obj.keySet()) {
                Log.d("SampleNames keys", key);
                Log.d("SampleNames values", obj.get(key).toString());
                sampleNames.SampleNames.add(obj.get(key).toString());
            }
            JsonArray returnArray = obj.get("sample_names").getAsJsonArray();

            return sampleNames;
        }
    }

}
