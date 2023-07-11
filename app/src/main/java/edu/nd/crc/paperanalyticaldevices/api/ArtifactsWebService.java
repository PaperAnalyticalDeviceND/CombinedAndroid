package edu.nd.crc.paperanalyticaldevices.api;


import edu.nd.crc.paperanalyticaldevices.api.utils.ResponseCallBack;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Tag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.semver4j.Semver;

import java.util.List;


public interface ArtifactsWebService {

    @Headers(value = {
            "Cache-Control: no-cache",
            "Content-Type: application/x-www-form-urlencoded"})
    @FormUrlEncoded
    @POST("o/token/")
    Call<AuthResponse> getAuth(@Field("client_id") String clientId, @Field("code") String code,
                               @Field("code_verifier") String codeVerifier, @Field("redirect_uri") String redirectUri, @Field("grant_type") String grantType);

    @GET("tasks/")
    Call<TasksList> getTasks(@Header("Authorization") String authToken, @Query("status") String status, @Query("page") int page);

    static ArtifactsWebService instantiate(OkHttpClient client, String baseUrl) {

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Semver.class, new AuthResponse.SemverDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new AuthResponse.StringListDeserializer())
                .registerTypeAdapter(Semver.class, new TasksList.SemverDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new TasksList.StringListDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                //.baseUrl("https://api-pad.artifactsofresearch.io/")
                .baseUrl("https://" + baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(ArtifactsWebService.class);

    }


}
