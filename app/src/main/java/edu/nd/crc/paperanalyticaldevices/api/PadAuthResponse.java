package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PadAuthResponse {
    /*
    {"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InB5blBmYWpZcWZROEJCOVlNVDJzTSJ9
.eyJpc3MiOiJodHRwczovL3BhcGVyLWFuYWx5dGljYWwtZGV2aWNlcy51cy5hdXRoMC5jb20vIiwic3ViIjoiVWVKR0hKR2pldnNlTHdF
eW8zOUxSc3lpOTA2NE9JVnVAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vcGFkLmNyYy5uZC5lZHUvYXBpL3YyIiwiaWF0IjoxNzI2ODU5ODM4LCJl
eHAiOjE3MjY5NDYyMzgsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyIsImF6cCI6IlVlSkdISkdqZXZzZUx3RXlvMzlMUnN5aTkwNjRPSVZ1In0.mafkpaGKrRnRhDP5CJn_a
-8JiPVOKgeWuNS7W2rX2SePwfUBVp0QBBe3Dq11NpHLV6POoozmBWsAi17_t37FMgF8scqwgvVtK1r-U0TpOOyDBqU9Q9cjpXhbhaU7lQmi4PNFbLZYSLN9it_bRN_e3N0K3Y7XYdNH
-0PrMsO6gu4NC_GDEZwuF3i3zO-1Y-CzKsPQc3vC_yGIr-X-s-Rrr0MMn4BEsAMO-OBtdtI66IMs-jL98Vw7NrA0_sG5jQNAbhXls5KeK3Lng0cSNuDHVRt2qXctRPVQi0ULJPbtIhSzHhpUbp
PLFIhyMHzJIV5bQbr0K0It6KR99hS_UDEm0A",
"expires_in":86400,
"token_type":"Bearer"}
     */

    public String toString() {
        return "PadAuthResponse{" +
                "AccessToken='" + AccessToken + '\'' +
                ", ExpiresIn=" + ExpiresIn +
                ", TokenType='" + TokenType + '\'' +
                '}';
    }

    @SerializedName("access_token")
    public String AccessToken;

    @SerializedName("expires_in")
    public Integer ExpiresIn;

    @SerializedName("token_type")
    public String TokenType;

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
