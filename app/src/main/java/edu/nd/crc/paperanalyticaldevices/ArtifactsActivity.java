package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.nd.crc.paperanalyticaldevices.api.ArtifactsWebService;
import edu.nd.crc.paperanalyticaldevices.api.AuthResponse;
import edu.nd.crc.paperanalyticaldevices.api.TasksList;
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor;
import edu.nd.crc.paperanalyticaldevices.api.utils.ResponseCallBack;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtifactsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    /*
    QR code will have:
    code
    code_verifier
    client_id
    host
     */
    private String redirectUri = "https://pad.artifactsofresearch.io/";

    private String baseUrl;// = "https://api-pad.artifactsofresearch.io";

    //private String clientId = "9xGhS62GK6JmxC7aB0PsiV0zJeWYhMxOOo3zEWtl";

    private String grantType = "authorization_code";

    public String authToken = "";

    public Boolean authorized = false;

    //TextView authTokenView;

    SharedPreferences defaultPrefs = null;

    TasksList tasks;

    ArrayList<ArtifactsTaskObject> taskObjects;
    //private List<String> taskStrings;
    //private HashMap<String, List<String>> expandableDetailList;

    ListView taskListView;

    TaskListBaseAdapter adapter;
    //private ExpandableTaskListAdapter expandableAdapter;

    Callback<TasksList> responseCallback;

    private SearchView searchView;
    private MenuItem searchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artifacts);

        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Intent deeplinkIntent = getIntent();
        String host = deeplinkIntent.getData().getHost();

        taskObjects = new ArrayList<ArtifactsTaskObject>();
        //taskStrings = new ArrayList<String>();
        ArtifactsTaskObject tempObject = new ArtifactsTaskObject();
        tempObject.setSampleId("ID");
        tempObject.setDrug("Drug");
        //taskObjects.add(tempObject);
        //authTokenView = findViewById(R.id.test_auth_token);

        taskListView = findViewById(R.id.tasks_mainlistview);

        //do authorization if we got here via QR code
        if(null != host ) {

            /*if(isAuthValid()){
                authToken = defaultPrefs.getString("auth_token", "");
                authorized = true;
                Log.d("ARTIFACTS", "auth still good " + authToken);*/
            //}else {
                String code = deeplinkIntent.getData().getQueryParameter("code");
                String codeVerifier = deeplinkIntent.getData().getQueryParameter("code_verifier");
                String clientId = deeplinkIntent.getData().getQueryParameter("client_id");
                baseUrl = deeplinkIntent.getData().getQueryParameter("host");


                performAsyncAuthorization(code, codeVerifier, baseUrl, clientId);
                //authTokenView.setText(authToken);
           // }
        }else if(isAuthValid()){
            authToken = defaultPrefs.getString("auth_token", "");
            authorized = true;
            Log.d("ARTIFACTS", "auth still good " + authToken);
        }

        //setActionBar();
        Toolbar myToolbar = findViewById(R.id.artifacts_toolbar);
        myToolbar.setTitle("Tasks");
        setSupportActionBar(myToolbar);

        adapter = new TaskListBaseAdapter(this, taskObjects);
        //expandableAdapter = new ExpandableTaskListAdapter(this, taskStrings, expandableDetailList);

        taskListView.setAdapter(adapter);

        refreshTasks(taskListView);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.artifacts_search_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.task_search);
        searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    private void setActionBar(){
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Tasks");
    }

    /*private String performAuthorization(String code, String codeVerifier){

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
    }*/

    public Boolean isAuthValid(){

        Long expiryTime = defaultPrefs.getLong("expiry_timestamp", 0);
        Log.d("ARTIFACTS", "Expiry time: " + expiryTime.toString());
        Long now = System.currentTimeMillis() / 1000;

        if(now >= expiryTime){
            return false;
        }else{
            return true;
        }

    }

    public void performAsyncAuthorization(String code, String codeVerifier, String host, String clientId){

        Thread thread = new Thread(new Runnable() {
           @Override
           public void run(){
               try{
                   Log.d("ARTIFACTS", "In performAuthorization");
                   final OkHttpClient client = new OkHttpClient.Builder()
                           .addNetworkInterceptor(new ProgressInterceptor())
                           .build();

                   final ArtifactsWebService service = ArtifactsWebService.instantiate(client, baseUrl);

                   Response<AuthResponse> response = service.getAuth(clientId, code, codeVerifier, redirectUri, grantType).execute();
                   Log.d("ARTIFACTS", response.toString());
                   AuthResponse authResponse = response.body();

                   String auth = authResponse.AccessToken;
                   Integer expirySeconds = authResponse.ExpiresIn;
                   Log.d("ARTIFACTS", auth);
                   Log.d("ARTIFACTS", expirySeconds.toString());

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

    public void getTaskListAsync(ResponseCallBack callBack){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.d("ARTIFACTS", "In getTaskListAsync");
                    final OkHttpClient client = new OkHttpClient.Builder()
                            .addNetworkInterceptor(new ProgressInterceptor())
                            .build();

                    final ArtifactsWebService service = ArtifactsWebService.instantiate(client, baseUrl);
                    Log.d("ARTIFACTS", authToken);

                    service.getTasks("Bearer " + authToken).enqueue( new Callback<TasksList>() {
                        @Override
                        public void onResponse(Call<TasksList> call, Response<TasksList> response) {
                            if(response.isSuccessful()){
                                tasks = response.body();
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
        });
        thread.start();
    }

    public void getTaskList(ResponseCallBack callBack){

        try{
            Log.d("ARTIFACTS", "In getTaskList");
            final OkHttpClient client = new OkHttpClient.Builder()
                    .addNetworkInterceptor(new ProgressInterceptor())
                    .build();

            final ArtifactsWebService service = ArtifactsWebService.instantiate(client, baseUrl);

            service.getTasks("Bearer " + authToken).enqueue( new Callback<TasksList>() {
                @Override
                public void onResponse(Call<TasksList> call, Response<TasksList> response) {
                    if(response.isSuccessful()){
                        tasks = response.body();

                        //taskObjects = new ArrayList<ArtifactsTaskObject>();

                        for (TasksList.Result result : tasks.Results) {
                            ArtifactsTaskObject obj = new ArtifactsTaskObject();
                            obj.setDrug(result.MainAPIs.get(0).Name);
                            obj.setSampleId(result.Sample);
                            taskObjects.add(obj);
                        }

                        adapter.notifyDataSetChanged();
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

                //tasks = response.body();
                taskObjects = new ArrayList<ArtifactsTaskObject>();
                //taskStrings = new ArrayList<String>();
                //expandableDetailList = new HashMap<String, List<String>>();

                for (TasksList.Result result : tasks.Results) {
                    ArtifactsTaskObject obj = new ArtifactsTaskObject();
                    obj.setDrug(result.MainAPIs.get(0).Name);
                    obj.setSampleId(result.Sample);
                    obj.setDosage(result.Dosage + " " + result.dosageType);
                    if(null != result.Manufacturer) {
                        obj.setManufacturer(result.Manufacturer.Name);
                    }
                    taskObjects.add(obj);
                   // taskStrings.add(obj.getSampleId());
                    Log.d("ARTIFACTS", obj.getSampleId());
                    List<String> tempList = new ArrayList<String>();
                    //tempList.add(obj.getDrug());
                    //expandableDetailList.put(obj.getSampleId(), tempList);
                }
                adapter.notifyDataSetChanged();
                //expandableAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Log.e("ARTIFACTS", "refreshTasks onFailure");
            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.getFilter().filter(newText);
        //expandableAdapter.getFilter().filter(newText);
        return true;
    }


}