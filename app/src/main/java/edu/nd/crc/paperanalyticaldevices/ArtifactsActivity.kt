package edu.nd.crc.paperanalyticaldevices

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.SearchView
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import edu.nd.crc.paperanalyticaldevices.api.ArtifactsWebService
import edu.nd.crc.paperanalyticaldevices.api.TasksList
import edu.nd.crc.paperanalyticaldevices.api.utils.ProgressInterceptor
import edu.nd.crc.paperanalyticaldevices.api.utils.ResponseCallBack
import edu.nd.crc.paperanalyticaldevices.ui.theme.CombinedAndroidTheme
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
    //"https://api-pad.artifactsofresearch.io"
    private var baseUrl: String = "api-pad.artifactsofresearch.io"

    //private String clientId = "9xGhS62GK6JmxC7aB0PsiV0zJeWYhMxOOo3zEWtl";
    private val grantType = "authorization_code"
    var authToken: String = ""
    var authorized = false


    var defaultPrefs: SharedPreferences? = null
    var tasks: TasksList? = null
    var taskObjects: ArrayList<ArtifactsTaskObject>? = null


    var taskListView: ListView? = null
    var adapter: TaskListBaseAdapter? = null


    var responseCallback: Callback<TasksList>? = null
    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null

    var taskPage: Int = 1

    private var selectedTask: ArtifactsTaskDisplayModel? = null
    private var selectedNetwork: NetworksDisplayModel? = null

    private var tensorflowView: PredictionModel? = null

    //ProjectsDbHelper dbHelper
    var dbHelper: ProjectsDbHelper? = null
    //SQLiteDatabase db
    var db: SQLiteDatabase? = null

    //var networksForDrug: ArrayList<String>? = null

    //var testPressed: Boolean = false

    val cameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->

        Log.d("ARTIFACTS", "Result from activity ${result.resultCode}")
        Log.d("ARTIFACTS", "Selected Task: ${selectedTask?.sampleId}")
        Log.d("ARTIFACTS", "Selected network ${selectedNetwork!!.network}")
        if(result.resultCode == Activity.RESULT_OK){
            Log.d("ARTIFACTS", "Result OK")
            val intent = result.data

            tensorflowView!!.LoadModelForArtifacts(defaultPrefs, selectedNetwork!!.network)
            tensorflowView!!.predict(intent)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        dbHelper = ProjectsDbHelper(this)
        db = dbHelper?.readableDatabase

        val deeplinkIntent = intent
        val host = deeplinkIntent.data!!.host
        taskObjects = ArrayList()

        /*if (null != host) {

            val code = deeplinkIntent.data!!.getQueryParameter("code")
            val codeVerifier = deeplinkIntent.data!!.getQueryParameter("code_verifier")
            val clientId = deeplinkIntent.data!!.getQueryParameter("client_id")
            baseUrl = deeplinkIntent.data!!.getQueryParameter("host").toString()
            Log.d("ARTIFACTS", "Base URL $baseUrl")
            performAsyncAuthorization(code, codeVerifier, baseUrl, clientId)

        }*/
        val code = deeplinkIntent.data!!.getQueryParameter("code").toString()
        val codeVerifier = deeplinkIntent.data!!.getQueryParameter("code_verifier").toString()
        val clientId = deeplinkIntent.data!!.getQueryParameter("client_id").toString()
        baseUrl = deeplinkIntent.data!!.getQueryParameter("host").toString()

        Log.d("ARTIFACTS", "Base URL $baseUrl")
        //loadTasks()
        val vm = ArtifactsTasksViewModel()
        //vm.getTasksList(authToken, "https://$baseUrl", 1)
        val authVm = ArtifactsAuthViewModel()
        val netVm = NetworksViewModel()


        tensorflowView = ViewModelProvider(owner = this)[PredictionModel::class.java]

        tensorflowView!!.result.observe(this) {

            //fun onChanged(result: PredictionModel.Result?) {
            val result = it
            Log.d("ARTIFACTS", "PredictionModel Observer onChanged")
            var intent = Intent(this, ResultActivity::class.java)
            intent.data = result!!.RectifiedImage
            intent.putExtra(MainActivity.EXTRA_PREDICTED, result.Predicted)
            if (result.QRCode.isPresent) intent.putExtra(
                MainActivity.EXTRA_SAMPLEID,
                result.QRCode.get()
            )
            if (result.Timestamp.isPresent) intent.putExtra(
                MainActivity.EXTRA_TIMESTAMP,
                result.Timestamp.get()
            )
            if (result.Labels.size > 0) intent.putExtra(
                MainActivity.EXTRA_LABEL_DRUGS,
                result.Labels
            )

            intent.putExtra(MainActivity.EXTRA_NN_CONC, result.Concentration)
            intent.putExtra(MainActivity.EXTRA_PREDICTED_DRUG, result.PredictedDrug)
            if(result.PLS != null) {
                intent.putExtra(MainActivity.EXTRA_PLS_CONC, result.PLS)
            }
            intent.putExtra(MainActivity.EXTRA_PROBABILITY, result.Probability)
            if (tensorflowView!!.usePls) {
                intent.putExtra(MainActivity.EXTRA_PLS_USED, true)
            } else {
                intent.putExtra(MainActivity.EXTRA_PLS_USED, false)
            }
            intent.putExtra(MainActivity.EXTRA_STATED_DRUG, vm.getSelected()?.drug)
            intent.putExtra(MainActivity.EXTRA_STATED_CONC, 100)

            startActivity(intent)
            // }
        }

        setContent {
            CombinedAndroidTheme {
                //ArtifactsTaskView(vm = vm, token = authToken, baseUrl = "https://$baseUrl")
                /*
                    ArtifactsLoginView(
                        baseUrl = "https://$baseUrl",
                        clientId = clientId,
                        redirectUri = redirectUri,
                        code = code,
                        codeVerifier = codeVerifier,
                        grantType = grantType,
                        authVm = authVm,
                        taskVm = vm,
                        onItemClicked = vm::selectTask,
                        testPressed =  this::pressedTestButton
                    )
                */
                ArtifactsMainView(
                    baseUrl = "https://$baseUrl",
                    clientId = clientId,
                    redirectUri = redirectUri,
                    code = code,
                    codeVerifier = codeVerifier,
                    grantType = grantType,
                    authVm = authVm,
                    taskVm = vm,
                    networksVm = netVm,
                    dbHelper = dbHelper!!,
                    onItemClicked = vm::selectTask,
                    testPressed = vm::confirmTask,
                    onNetworkPressed = this::startCamera
                )
            }
        }
    }

    private fun startCamera(task: ArtifactsTaskDisplayModel, network: NetworksDisplayModel){
        //testPressed = false
        Log.d("ARTIFACTS", "In startCamera")
        Log.d("ARTIFACTS", "Task: ${task.sampleId}")
        Log.d("ARTIFACTS", "Network: ${network.network}")
        // Store the selected task so we can refernce it when we handle the camera activity result
        selectedTask = task
        selectedNetwork = network
        cameraResult.launch(Intent(this, Camera2Activity::class.java))
    }

    /*fun pressedTestButton(task: ArtifactsTaskDisplayModel){
        //networksForDrug = getNetworksForDrug(task = task)

        testPressed = true
    }*/

    fun getNetworksForDrug(task: ArtifactsTaskDisplayModel): ArrayList<String>{
        selectedTask = task
        var networks = ArrayList<String>()

        var cursor: Cursor? = db?.rawQuery("SELECT DISTINCT(${DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK}) from ${DrugsContract.DrugsEntry.TABLE_NAME} WHERE ${DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME} = ${task.drug}", null)

        if (cursor != null) {
            while(cursor.moveToNext()){
                networks.add(cursor.getString(cursor.getColumnIndexOrThrow(DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK)))
            }
        }
        return networks
    }

    fun onCreateOld(savedInstanceState: Bundle?) {
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
            baseUrl = deeplinkIntent.data!!.getQueryParameter("host").toString()
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
                service.getTasks("Bearer $authToken", "awaiting", 1).enqueue(object : Callback<TasksList?> {
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
            service.getTasks("Bearer $authToken", "awaiting", 1).enqueue(object : Callback<TasksList?> {
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

    fun loadTasks(){
        Log.d("ARTIFACTS", "In loadTasks")
        getTaskList(object : ResponseCallBack {
            override fun onResponse(response: Response<TasksList>) {
                Log.d("ARTIFACTS", "in callback")

                //tasks = response.body();
                taskObjects = ArrayList()
                //taskStrings = new ArrayList<String>();
                //expandableDetailList = new HashMap<String, List<String>>();
                for (result in tasks!!.Results) {
                    val obj = ArtifactsTaskObject()
                    obj.id = result.Id
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
                Log.e("ARTIFACTS", "loadTasks onFailure")
            }
        })
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
                    obj.id = result.Id
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

/*class ArtifactsTasksViewModel : ViewModel() {
    private val _taskList = mutableStateListOf<ArtifactsTaskObject>()
    var errorMessage: String by mutableStateOf("")
    val taskList: List<ArtifactsTaskObject>
        get() = _taskList

    fun getTaskList(authToken: String, baseUrl: String){
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(ProgressInterceptor())
            .build()
        viewModelScope.launch {
            val apiService = ArtifactsWebService.instantiate(client, baseUrl)
            try{
                _taskList.clear()
                val tasks = apiService.getTasks(authToken, "awaiting", 1)
                for (task in tasks!!.Results){

                }
            }catch(e: Exception){
                errorMessage = e.message.toString()
            }
        }
    }
}*/







/*@Composable
fun ArtifactsTaskPage(refreshCallback: () -> Unit, modifier: Modifier = Modifier,
drugs: List<ArtifactsTaskObject> = List<ArtifactsTaskObject>(100){ ArtifactsTaskObject() }){
    Surface() {
        Column {
            Text(text = "Tasks")
            ArtifactsTaskList(modifier = Modifier.weight(1f),
            drugs = drugs, startCamera = startCamera)
            Spacer(modifier = Modifier)
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(1f)) {
                ElevatedButton(onClick = refreshCallback ) {
                    Text(text = "Refresh")
                }
            }
        }
    }
}*/

/*
@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun ArtifactsTaskPagePreview(){
    CombinedAndroidTheme() {
        var taskObjects = ArrayList<ArtifactsTaskObject>()
        var taskOne = ArtifactsTaskObject()
        taskOne.id = 1
        taskOne.sampleId = "22ETCL-17"
        taskOne.drug = "Diphenhydramine"
        taskOne.manufacturer = "Pfizer"
        taskOne.dosage = "12.00000"
        taskObjects.add(taskOne)
        var taskTwo = ArtifactsTaskObject()
        taskTwo.id = 2
        taskTwo.sampleId = "22ETCL-18"
        taskTwo.drug = "Acetaminophen"
        taskTwo.manufacturer = "Teva"
        taskTwo.dosage = "100.000"
        taskObjects.add(taskTwo)
        ArtifactsTaskPage(refreshCallback = { */
/*TODO*//*
 }, drugs = taskObjects)
    }
}*/
