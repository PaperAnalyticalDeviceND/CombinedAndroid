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
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Tag;

public interface WebService {
//    @GET("/index.php?option=com_jbackend&view=request&module=querytojson&action=get&resource=list&queryname=network_info")
//    Call<ResponseList<NetworkEntry>> GetNetworkInfo(@Query("api_key") String api_key);
//
//    @GET("/index.php?option=com_jbackend&view=request&module=querytojson&action=get&resource=list&queryname=projects")
//    Call<ResponseList<String[]>> GetProjects(@Query("api_key") String api_key);
//
//    @GET("/index.php?option=com_jbackend&view=request&module=querytojson&action=get&resource=list&queryname=networks")
//    Call<ResponseList<String[]>> GetNetworkNames(@Query("api_key") String api_key);

    @FormUrlEncoded
    @POST("/index.php?option=com_jbackend&view=request&module=querytojson&action=post&resource=upload")
    Call<JsonObject> UploadResult(@FieldMap Map<String, String> names, @Tag ProgressCallback progress);

    @POST("/api/v2/cards")
    Call<JsonObject> UploadResultV2(@Body UploadRequest request, @Tag ProgressCallback progress);

    @GET("/api/v2/neural-networks")
    Call<List<NetworkV2>> GetNeuralNetsV2();

    @GET("/api/v2/projects")
    Call<List<ProjectV2>> GetProjectsV2();

    static WebService instantiate(OkHttpClient client) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Semver.class, new NetworkEntry.SemverDeserializer())
                //.registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new NetworkEntry.StringListDeserializer())
                .registerTypeAdapter(SampleNames.class, new SampleNames.SampleNamesDeserializer())
                .registerTypeAdapter(TypeToken.getParameterized(List.class, String.class).getType(), new NetworkV2.StringListDeserializer())
                .create();
//http://pad-naxos.crc.nd.edu/
        //https://pad.crc.nd.edu/
        //.baseUrl("https://pad.crc.nd.edu/")
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pad.crc.nd.edu/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(WebService.class);
    }
}
