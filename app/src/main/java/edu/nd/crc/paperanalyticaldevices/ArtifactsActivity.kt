package edu.nd.crc.paperanalyticaldevices

import android.app.SearchManager
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
/*import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier*/
import androidx.preference.PreferenceManager

import edu.nd.crc.paperanalyticaldevices.api.ArtifactsWebService
import edu.nd.crc.paperanalyticaldevices.api.TasksList
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor
import edu.nd.crc.paperanalyticaldevices.api.utils.ResponseCallBack
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class ArtifactsActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    /*
    QR code will have:
    code
    code_verifier
    client_id
    host
     */
    private val redirectUri = "https://pad.artifactsofresearch.io/"
    private var baseUrl // = "https://api-pad.artifactsofresearch.io";
            : String? = null

    //private String clientId = "9xGhS62GK6JmxC7aB0PsiV0zJeWYhMxOOo3zEWtl";
    private val grantType = "authorization_code"
    var authToken: String? = ""
    var authorized = false

    //TextView authTokenView;
    var defaultPrefs: SharedPreferences? = null
    var tasks: TasksList? = null
    var taskObjects: ArrayList<ArtifactsTaskObject>? = null

    //private List<String> taskStrings;
    //private HashMap<String, List<String>> expandableDetailList;
    var taskListView: ListView? = null
    var adapter: TaskListBaseAdapter? = null

    //private ExpandableTaskListAdapter expandableAdapter;
    var responseCallback: Callback<TasksList>? = null
    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artifacts)
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val deeplinkIntent = intent
        val host = deeplinkIntent.data!!.host
        taskObjects = ArrayList()
        //taskStrings = new ArrayList<String>();
        val tempObject = ArtifactsTaskObject()
        tempObject.sampleId = "ID"
        tempObject.drug = "Drug"
        //taskObjects.add(tempObject);
        //authTokenView = findViewById(R.id.test_auth_token);
        taskListView = findViewById(R.id.tasks_mainlistview)

        //do authorization if we got here via QR code
        if (null != host) {

            /*if(isAuthValid()){
                authToken = defaultPrefs.getString("auth_token", "");
                authorized = true;
                Log.d("ARTIFACTS", "auth still good " + authToken);*/
            //}else {
            val code = deeplinkIntent.data!!.getQueryParameter("code")
            val codeVerifier = deeplinkIntent.data!!.getQueryParameter("code_verifier")
            val clientId = deeplinkIntent.data!!.getQueryParameter("client_id")
            baseUrl = deeplinkIntent.data!!.getQueryParameter("host")
            performAsyncAuthorization(code, codeVerifier, baseUrl, clientId)
            //authTokenView.setText(authToken);
            // }
        } /*else if (isAuthValid) {
            authToken = defaultPrefs.getString("auth_token", "")
            authorized = true
            Log.d("ARTIFACTS", "auth still good $authToken")
        }*/

        //setActionBar();
        val myToolbar = findViewById<Toolbar>(R.id.artifacts_toolbar)
        myToolbar.title = "Tasks"
        setSupportActionBar(myToolbar)
        adapter = TaskListBaseAdapter(this, taskObjects)
        //expandableAdapter = new ExpandableTaskListAdapter(this, taskStrings, expandableDetailList);
        taskListView!!.adapter = adapter
        refreshTasks(taskListView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.artifacts_search_menu, menu)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.task_search)
        searchView = searchMenuItem!!.actionView as SearchView
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView!!.isSubmitButtonEnabled = true
        searchView!!.setOnQueryTextListener(this)
        return true
    }

    private fun setActionBar() {
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar!!.title = "Tasks"
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
    val isAuthValid: Boolean
        get() {
            val expiryTime = defaultPrefs!!.getLong("expiry_timestamp", 0)
            Log.d("ARTIFACTS", "Expiry time: $expiryTime")
            val now = System.currentTimeMillis() / 1000
            return if (now >= expiryTime) {
                false
            } else {
                true
            }
        }

    fun performAsyncAuthorization(
        code: String?,
        codeVerifier: String?,
        host: String?,
        clientId: String?
    ) {
        val thread = Thread {
            try {
                Log.d("ARTIFACTS", "In performAuthorization")
                val client = OkHttpClient.Builder()
                    .addNetworkInterceptor(ProgressInterceptor())
                    .build()
                val service = ArtifactsWebService.instantiate(client, baseUrl)
                val response =
                    service.getAuth(clientId, code, codeVerifier, redirectUri, grantType).execute()
                Log.d("ARTIFACTS", response.toString())
                val authResponse = response.body()
                val auth = authResponse!!.AccessToken
                val expirySeconds = authResponse.ExpiresIn
                Log.d("ARTIFACTS", auth)
                Log.d("ARTIFACTS", expirySeconds.toString())
                authToken = auth
                //authTokenView.setText(authToken);
                defaultPrefs!!.edit().putString("auth_token", authToken).apply()
                val tsLong = System.currentTimeMillis() / 1000
                val expiryTime = tsLong + expirySeconds
                defaultPrefs!!.edit().putLong("expiry_timestamp", expiryTime).apply()
                authorized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    fun getTaskListAsync(callBack: ResponseCallBack) {
        val thread = Thread {
            try {
                Log.d("ARTIFACTS", "In getTaskListAsync")
                val client = OkHttpClient.Builder()
                    .addNetworkInterceptor(ProgressInterceptor())
                    .build()
                val service = ArtifactsWebService.instantiate(client, baseUrl)
                Log.d("ARTIFACTS", authToken!!)
                service.getTasks("Bearer $authToken").enqueue(object : Callback<TasksList?> {
                    override fun onResponse(
                        call: Call<TasksList?>,
                        response: Response<TasksList?>
                    ) {
                        if (response.isSuccessful) {
                            tasks = response.body()
                            callBack.onResponse(response)
                        } else {
                            Log.d("ARTIFACTS", "Failed getTaskList")
                            try {
                                Log.d("ARTIFACTS", response.errorBody()!!.string())
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onFailure(call: Call<TasksList?>, t: Throwable) {
                        Log.d("ARTIFACTS", "In onFailure")
                        callBack.onFailure(t)
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    fun getTaskList(callBack: ResponseCallBack) {
        try {
            Log.d("ARTIFACTS", "In getTaskList")
            val client = OkHttpClient.Builder()
                .addNetworkInterceptor(ProgressInterceptor())
                .build()
            val service = ArtifactsWebService.instantiate(client, baseUrl)
            service.getTasks("Bearer $authToken").enqueue(object : Callback<TasksList?> {
                override fun onResponse(call: Call<TasksList?>, response: Response<TasksList?>) {
                    if (response.isSuccessful) {
                        tasks = response.body()

                        //taskObjects = new ArrayList<ArtifactsTaskObject>();
                        for (result in tasks!!.Results) {
                            val obj = ArtifactsTaskObject()
                            obj.drug = result.MainAPIs[0].Name
                            obj.sampleId = result.Sample
                            taskObjects!!.add(obj)
                        }
                        adapter!!.notifyDataSetChanged()
                        callBack.onResponse(response)
                    } else {
                        Log.d("ARTIFACTS", "Failed getTaskList")
                        try {
                            Log.d("ARTIFACTS", response.errorBody()!!.string())
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onFailure(call: Call<TasksList?>, t: Throwable) {
                    Log.d("ARTIFACTS", "In onFailure")
                    callBack.onFailure(t)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshTasks(view: View?) {
        Log.d("ARTIFACTS", "In refreshTasks")
        //getTasksAsync();
        getTaskList(object : ResponseCallBack {
            override fun onResponse(response: Response<TasksList>) {
                Log.d("ARTIFACTS", "in callback")

                //tasks = response.body();
                taskObjects = ArrayList()
                //taskStrings = new ArrayList<String>();
                //expandableDetailList = new HashMap<String, List<String>>();
                for (result in tasks!!.Results) {
                    val obj = ArtifactsTaskObject()
                    obj.drug = result.MainAPIs[0].Name
                    obj.sampleId = result.Sample
                    obj.dosage = result.Dosage + " " + result.dosageType
                    if (null != result.Manufacturer) {
                        obj.manufacturer = result.Manufacturer.Name
                    }
                    taskObjects!!.add(obj)
                    // taskStrings.add(obj.getSampleId());
                    Log.d("ARTIFACTS", obj.sampleId)
                    val tempList: List<String> = ArrayList()
                    //tempList.add(obj.getDrug());
                    //expandableDetailList.put(obj.getSampleId(), tempList);
                }
                adapter!!.notifyDataSetChanged()
                //expandableAdapter.notifyDataSetChanged();
            }

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
                Log.e("ARTIFACTS", "refreshTasks onFailure")
            }
        })
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter!!.filter.filter(newText)
        //expandableAdapter.getFilter().filter(newText);
        return true
    }
}

/*
@Composable
fun ArtifactsTaskListItem(modifier: Modifier = Modifier, id: String, drug: String){

}*/
