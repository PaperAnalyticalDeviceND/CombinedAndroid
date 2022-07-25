package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;


import java.io.IOException;
import java.util.ArrayList;

import edu.nd.crc.paperanalyticaldevices.api.ArtifactsWebService;
import edu.nd.crc.paperanalyticaldevices.api.AuthResponse;
import edu.nd.crc.paperanalyticaldevices.api.TasksList;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor;
import edu.nd.crc.paperanalyticaldevices.api.utils.ResponseCallBack;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtifactsActivity extends AppCompatActivity {

    private String redirectUri = "https://pad.artifactsofresearch.io/";

    private String baseUrl = "https://api-pad.artifactsofresearch.io";

    private String clientId = "9xGhS62GK6JmxC7aB0PsiV0zJeWYhMxOOo3zEWtl";

    private String grantType = "authorization_code";

    public String authToken = "";

    public Boolean authorized = false;

    //TextView authTokenView;

    SharedPreferences defaultPrefs = null;

    TasksList tasks;

    ArrayList<ArtifactsTaskObject> taskObjects;

    ListView taskListView;

    TaskListBaseAdapter adapter;

    Callback<TasksList> responseCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artifacts);

        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Intent deeplinkIntent = getIntent();
        String host = deeplinkIntent.getData().getHost();

        taskObjects = new ArrayList<ArtifactsTaskObject>();
        ArtifactsTaskObject tempObject = new ArtifactsTaskObject();
        tempObject.setSampleId("ID");
        tempObject.setDrug("Drug");
        taskObjects.add(tempObject);
        //authTokenView = findViewById(R.id.test_auth_token);

        taskListView = findViewById(R.id.tasks_listview);

        //do authorization if we got here via QR code
        if(null != host ) {

            if(isAuthValid()){
                authToken = defaultPrefs.getString("auth_token", "");
                authorized = true;
            }else {
                String code = deeplinkIntent.getData().getQueryParameter("code");
                String codeVerifier = deeplinkIntent.getData().getQueryParameter("code_verifier");

                performAsyncAuthorization(code, codeVerifier);
                //authTokenView.setText(authToken);
            }
        }
        //getTasksAsync();
        refreshTasks(taskListView);
        adapter = new TaskListBaseAdapter(this, taskObjects);

        taskListView.setAdapter(adapter);


    }



    private String performAuthorization(String code, String codeVerifier){

        Log.d("ARTIFACTS", "In performAuthorization");
        final OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new ProgressInterceptor())
                .build();

        final ArtifactsWebService service = ArtifactsWebService.instantiate(client);

        try {
            Response<AuthResponse> response = service.getAuth(clientId, code, codeVerifier, redirectUri, grantType).execute();
            Log.d("ARTIFACTS", response.toString());
            AuthResponse authResponse = response.body();

            String authToken = authResponse.AccessToken;
            Log.d("ARTIFACTS", authToken);
            return authToken;

        }catch(Exception e){
            e.printStackTrace();
        }

        return "";
    }

    public Boolean isAuthValid(){

        Long expiryTime = defaultPrefs.getLong("expiry_timestamp", 0);
        Long now = System.currentTimeMillis() / 1000;

        if(now >= expiryTime){
            return false;
        }else{
            return true;
        }

    }

    public void performAsyncAuthorization(String code, String codeVerifier){

        Thread thread = new Thread(new Runnable() {
           @Override
           public void run(){
               try{
                   Log.d("ARTIFACTS", "In performAuthorization");
                   final OkHttpClient client = new OkHttpClient.Builder()
                           .addNetworkInterceptor(new ProgressInterceptor())
                           .build();

                   final ArtifactsWebService service = ArtifactsWebService.instantiate(client);

                   Response<AuthResponse> response = service.getAuth(clientId, code, codeVerifier, redirectUri, grantType).execute();
                   Log.d("ARTIFACTS", response.toString());
                   AuthResponse authResponse = response.body();

                   String auth = authResponse.AccessToken;
                   Integer expirySeconds = authResponse.ExpiresIn;
                   Log.d("ARTIFACTS", auth);

                   authToken = auth;
                   //authTokenView.setText(authToken);
                   defaultPrefs.edit().putString("auth_token", authToken).apply();


                   Long tsLong = System.currentTimeMillis() / 1000;
                   Long expiryTime = tsLong + expirySeconds;
                   defaultPrefs.edit().putLong("expiry_timestamp", expiryTime).apply();

                   authorized = true;

               }catch(Exception e){
                   e.printStackTrace();
               }
           }
        });
        thread.start();
    }

    public void getTaskList(ResponseCallBack callBack){

        try{
            Log.d("ARTIFACTS", "In getTaskList");
            final OkHttpClient client = new OkHttpClient.Builder()
                    .addNetworkInterceptor(new ProgressInterceptor())
                    .build();

            final ArtifactsWebService service = ArtifactsWebService.instantiate(client);

            service.getTasks("Bearer " + authToken).enqueue( new Callback<TasksList>() {
                @Override
                public void onResponse(Call<TasksList> call, Response<TasksList> response) {
                    if(response.isSuccessful()){
                        tasks = response.body();

                        for (TasksList.Result result : tasks.Results) {
                            ArtifactsTaskObject obj = new ArtifactsTaskObject();
                            obj.setDrug(result.MainAPIs.get(0).Name);
                            obj.setSampleId(result.Sample);
                            taskObjects.add(obj);
                        }

                        //adapter.notifyDataSetChanged();
                        callBack.onResponse(response);
                    }else{
                        Log.d("ARTIFACTS", "Failed getTaskList");
                        try {
                            Log.d("ARTIFACTS", response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<TasksList> call, Throwable t) {
                    Log.d("ARTIFACTS", "In onFailure");
                    callBack.onFailure(t);
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }

    }



    public void refreshTasks(View view){
        Log.d("ARTIFACTS", "In refreshTasks");
        //getTasksAsync();
        getTaskList(new ResponseCallBack() {
            @Override
            public void onResponse(Response<TasksList> response) {
                Log.d("ARTIFACTS", "in callback");
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("ARTIFACTS", "onFailuer");
            }
        });
    }
}