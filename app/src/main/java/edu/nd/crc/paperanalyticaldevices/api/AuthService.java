package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.semver4j.Semver;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthService {
        /*
    curl --request POST \
>   --url https://paper-analytical-devices.us.auth0.com/oauth/token \
>   --header 'content-type: application/json' \
>   --data '{"client_id":"UeJGHJGjevseLwEyo39LRsyi9064OIVu","client_secret":"voEywl0Z86Niwt-nK8JGPheEx2S2ZVkKVonvugKaJHbkfPNoE0WtfJlOT_m538yw",
"audience":"https://pad.crc.nd.edu/api/v2","grant_type":"client_credentials"}'
{"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InB5blBmYWpZcWZROEJCOVlNVDJzTSJ9
.eyJpc3MiOiJodHRwczovL3BhcGVyLWFuYWx5dGljYWwtZGV2aWNlcy51cy5hdXRoMC5jb20vIiwic3ViIjoiVWVKR0hKR2pldnNlTHdF
eW8zOUxSc3lpOTA2NE9JVnVAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vcGFkLmNyYy5uZC5lZHUvYXBpL3YyIiwiaWF0IjoxNzI2ODU5ODM4LCJl
eHAiOjE3MjY5NDYyMzgsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyIsImF6cCI6IlVlSkdISkdqZXZzZUx3RXlvMzlMUnN5aTkwNjRPSVZ1In0.mafkpaGKrRnRhDP5CJn_a
-8JiPVOKgeWuNS7W2rX2SePwfUBVp0QBBe3Dq11NpHLV6POoozmBWsAi17_t37FMgF8scqwgvVtK1r-U0TpOOyDBqU9Q9cjpXhbhaU7lQmi4PNFbLZYSLN9it_bRN_e3N0K3Y7XYdNH
-0PrMsO6gu4NC_GDEZwuF3i3zO-1Y-CzKsPQc3vC_yGIr-X-s-Rrr0MMn4BEsAMO-OBtdtI66IMs-jL98Vw7NrA0_sG5jQNAbhXls5KeK3Lng0cSNuDHVRt2qXctRPVQi0ULJPbtIhSzHhpUbp
PLFIhyMHzJIV5bQbr0K0It6KR99hS_UDEm0A","expires_in":86400,"token_type":"Bearer"}
     */

    @Headers({"content-type: application/json"})
    @POST("/oauth/token")
    Call<PadAuthResponse> getToken(@Body PadAuthRequest request);

    static AuthService instantiate(OkHttpClient client) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new PadAuthResponse.StringListDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://paper-analytical-devices.us.auth0.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(AuthService.class);
    }
}
