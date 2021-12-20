package edu.nd.crc.paperanalyticaldevices.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.semver4j.Semver;

import java.util.List;
import java.util.Map;

import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressCallback;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Tag;

public interface WebService {
    @GET("/index.php?option=com_jbackend&view=request&module=querytojson&action=get&resource=list&queryname=network_info")
    Call<ResponseList<NetworkEntry>> GetNetworkInfo(@Query("api_key") String api_key);

    @FormUrlEncoded
    @POST("/index.php?option=com_jbackend&view=request&module=querytojson&action=post&resource=upload")
    Call<JsonObject> UploadResult(@FieldMap Map<String, String> names, @Tag ProgressCallback progress);

    static WebService instantiate(OkHttpClient client) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Semver.class, new NetworkEntry.SemverDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new NetworkEntry.StringListDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pad.crc.nd.edu/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(WebService.class);
    }
}
