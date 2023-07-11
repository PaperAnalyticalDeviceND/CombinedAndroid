package edu.nd.crc.paperanalyticaldevices.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
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