package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.annotations.SerializedName;

public class PadAuthRequest {
    /*
    {"client_id":"UeJGHJGjevseLwEyo39LRsyi9064OIVu",
    "client_secret":"voEywl0Z86Niwt-nK8JGPheEx2S2ZVkKVonvugKaJHbkfPNoE0WtfJlOT_m538yw",
    "audience":"https://pad.crc.nd.edu/api/v2",
    "grant_type":"client_credentials"}'
     */
    public String toString() {
        return "PadAuthRequest{" +
                "ClientId='" + ClientId + '\'' +
                ", ClientSecret='" + ClientSecret + '\'' +
                ", Audience='" + Audience + '\'' +
                ", GrantType='" + GrantType + '\'' +
                '}';
    }

    @SerializedName("client_id")
    public String ClientId;

    @SerializedName("client_secret")
    public String ClientSecret;

    @SerializedName("audience")
    public String Audience;

    @SerializedName("grant_type")
    public String GrantType;

    public PadAuthRequest(String clientId, String clientSecret, String audience, String grantType) {
        this.ClientId = clientId;
        this.ClientSecret = clientSecret;
        this.Audience = audience;
        this.GrantType = grantType;
    }


}
