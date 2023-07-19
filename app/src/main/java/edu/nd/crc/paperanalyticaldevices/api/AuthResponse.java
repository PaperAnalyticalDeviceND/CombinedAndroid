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

public class AuthResponse {
    @SerializedName("access_token")
    public String AccessToken;

    @SerializedName("refresh_token")
    public String RefreshToken;

    @SerializedName("token_type")
    public String TokenType;

    @SerializedName("scope")
    public String Score;

    @SerializedName("expires_in")
    public Integer ExpiresIn;

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

}


