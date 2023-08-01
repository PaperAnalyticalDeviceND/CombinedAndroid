package edu.nd.crc.paperanalyticaldevices.api

import com.google.firebase.crashlytics.internal.network.HttpResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtifactsAPIService {

    @Headers("Cache-Control: no-cache",
        "Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("o/token/")
    suspend fun getAuth(@Field("client_id") clientId: String,
                        @Field("code") code: String,
                        @Field("code_verifier") codeVerifier: String,
                        @Field("redirect_uri") redirectUri: String,
                        @Field("grant_type") grantType: String): AuthResponse

    @GET("tasks/")
    suspend fun getTasks(@Header("Authorization") token: String,
                         @Query("status") status: String,
                         @Query("page") page: Int): TasksList

    @GET("tasks/?status=awaiting")
    suspend fun getDefaultTasks(@Header("Authorization") token: String): TasksList

   /* @Multipart
    @POST("/tasks/{task_id}/test_automatic")
    fun sendArtifactsResult(@Header("Authorization") token: String,
                                    @Path("task_id") taskId: Int, @PartMap params: Map<String, RequestBody>
                                    ): ResponseBody*/

    @Multipart
    @POST("/tasks/{task_id}/test_automatic/")
    fun sendArtifactsResult(@Header("Authorization") token: String,
                                    @Path("task_id") taskId: Int,
                            @Part rectFile: MultipartBody.Part,
                            @Part rawFile: MultipartBody.Part,
                            @Part testDate: MultipartBody.Part,
                            @Part taskNotes: MultipartBody.Part,
                            @Part result: MultipartBody.Part
                                    ):  Call<ResponseBody>
    /*@Part("files") rectFile: MultipartBody.Part,
                                    @Part("files") rawFile: MultipartBody.Part,
                                    @Part("test_date") testDate: MultipartBody.Part,
                                    @Part("task_notes") taskNotes: MultipartBody.Part,
                                    @Part("result") result: MultipartBody.Part
     */

    companion object {
        var apiService: ArtifactsAPIService? = null
        fun getInstance(baseUrl: String): ArtifactsAPIService {
            if(apiService == null){
                apiService = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(ArtifactsAPIService::class.java)
            }
            return apiService!!
        }
    }
}