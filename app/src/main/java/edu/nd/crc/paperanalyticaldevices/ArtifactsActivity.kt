package edu.nd.crc.paperanalyticaldevices

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.nd.crc.paperanalyticaldevices.api.ArtifactsAPIService
import edu.nd.crc.paperanalyticaldevices.api.AuthResponse
import edu.nd.crc.paperanalyticaldevices.ui.theme.CombinedAndroidTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.math.exp


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
            //: String? = null

    //private String clientId = "9xGhS62GK6JmxC7aB0PsiV0zJeWYhMxOOo3zEWtl";
    private val grantType = "authorization_code"
    var authToken: String = ""
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

    var taskPage: Int = 1

    val cameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            val intent = result.data

        }
        Log.d("ARTIFACTS", "Result from activity")
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
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

        /*setContent{
            CombinedAndroidTheme {
                ArtifactsTaskPage(refreshCallback = {vm.getTasksList(authToken,
                    "https://$baseUrl", 1)} , drugs = vm.getResultsAsObjects())
            }
        }*/
        setContent {
            CombinedAndroidTheme {
                //ArtifactsTaskView(vm = vm, token = authToken, baseUrl = "https://$baseUrl")
                ArtifactsLoginView(
                    baseUrl = "https://$baseUrl",
                    clientId = clientId,
                    redirectUri = redirectUri,
                    code = code,
                    codeVerifier = codeVerifier,
                    grantType = grantType,
                    authVm = authVm,
                    taskVm = vm,
                    startCamera = { startCamera() }
                )
            }
        }
    }

    fun startCamera(){
        Log.d("ARTIFACTS", "In startCamera")
        cameraResult.launch(Intent(this, Camera2Activity::class.java))
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

class ArtifactsAuthViewModel : ViewModel() {
    private val _authToken = mutableStateOf("")
    var errorMessage: String by mutableStateOf("")
    val authToken: String
        get() = _authToken.value

    fun getAuth(baseUrl: String,
                code: String,
                codeVerifier: String,
                redirectUri: String,
                clientId: String,
                grantType: String){
        Log.d("ARTIFACTS", "authVm getAuth")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                val authResponse = apiService.getAuth(clientId = clientId, code = code, codeVerifier = codeVerifier, redirectUri = redirectUri, grantType = grantType)
                _authToken.value = authResponse.AccessToken
                Log.d("ARTIFACTS", "Auth token: ${_authToken.value}")
            }catch(e: Exception){
                errorMessage = e.message.toString()
            }
        }
    }
}
class ArtifactsTasksViewModel : ViewModel() {
    private val _taskList = mutableStateListOf<TasksList>()
    private val _taskObjects = mutableStateListOf<ArtifactsTaskObject>()
    var errorMessage: String by mutableStateOf("")
    val taskList: List<TasksList>
        get() = _taskList

    val taskObjects: List<ArtifactsTaskObject>
        get() = _taskObjects

    fun getTasksList(token: String, baseUrl: String, page: Int){
        Log.d("ARTIFACTS", "getTasksList")
        Log.d("ARTIFACTS", "Token: $token, baseUrl: $baseUrl")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.add(apiService.getTasks(token = "Bearer $token", status = "awaiting", page = page))
                //_taskList.add(apiService.getDefaultTasks(token = "Bearer $token"))
            }catch(e: Exception){
                errorMessage = e.message.toString()
            }
        }
    }

    fun getResultsAsObjects(): List<ArtifactsTaskObject> {
        _taskObjects.clear()
        for(t in _taskList){
            for(r in t.Results){
                var obj = ArtifactsTaskObject()
                obj.id = r.Id
                obj.drug = r.MainAPIs[0].Name
                obj.dosage = r.Dosage + " " + r.dosageType.Name
                if(null != r.Manufacturer){
                    obj.manufacturer = r.Manufacturer.Name
                }else{
                    obj.manufacturer = ""
                }

                obj.sampleId = r.Sample
                _taskObjects.add(obj)
            }
        }
        return _taskObjects
    }
}



@Composable
fun ArtifactsLoginView(modifier: Modifier = Modifier,
                       baseUrl: String,
                       clientId: String,
                       redirectUri: String,
                       code: String,
                       codeVerifier: String,
                       grantType: String,
                       authVm: ArtifactsAuthViewModel,
                       taskVm: ArtifactsTasksViewModel,
                       startCamera: () -> Unit ){

    LaunchedEffect(Unit, block = {
        authVm.getAuth(baseUrl, code, codeVerifier, redirectUri, clientId, grantType)
    })

    //var authorized by rememberSaveable { mutableStateOf(false) }
    Surface() {
        if(authVm.authToken.isNotEmpty()){
            Log.d("ARTIFACTS", "baseUrl $baseUrl")
            Log.d("ARTIFACTS", "auth token ${authVm.authToken}")
            ArtifactsTaskView(vm = taskVm, token = authVm.authToken, baseUrl = baseUrl, startCamera = startCamera)
        }else{
            Column() {
                Row() {
                    ElevatedButton(onClick = {
                        Log.d("ARTIFACTS", "Clicked Log In")
                        authVm.getAuth(baseUrl, code, codeVerifier, redirectUri, clientId, grantType)
                        //authorized = true
                        /*if(authVm.authToken != ""){
                            Log.d("ARTIFACTS", "Marking authorized")
                            authorized = true
                        }*/
                    }) {
                        Text(text = "Log In")
                    }
                }
            }
        }
    }

}

@Composable
fun ArtifactsTaskListItem(modifier: Modifier = Modifier, task: ArtifactsTaskObject, startCamera: () -> Unit){
    var expanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(4.dp)
    ) {
        Column(){
            Row(modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ))   {
                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(imageVector = if(expanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
                }
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(text = "ID:")
                    Text(text = "API:", style = MaterialTheme.typography.bodySmall)

                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.sampleId)
                    Text(text = task.drug, style = MaterialTheme.typography.bodySmall)

                }
                //Text(text = "ID:", modifier = Modifier.padding(8.dp))
                //Text(text = task.sampleId, modifier = Modifier.padding(8.dp).weight(1f))
                ElevatedButton(onClick =
                    //{cameraResult.launch(Intent(this, Camera2Activity::class.java))}
                    //{context.startActivity( Intent(context, Camera2Activity::class.java))}
                    startCamera
                ) {
                    Text(text = "Test")
                }
            }
            if(expanded) {
                ArtifactsTaskListItemDetail(task = task)
            }
        }
    }
}

@Composable
fun ArtifactsTaskListItemDetail(modifier: Modifier = Modifier, task: ArtifactsTaskObject){
    Row(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            //Text(text = "ID:")
            //Text(text = "API:")
            Text(text = "Task ID:")
            Text(text = "Manufacturer:")
            Text(text = "Dosage:")
        }
        Column(modifier = Modifier.weight(1f)) {
            //Text(text = task.sampleId)
            //Text(text = task.drug)
            Text(text = task.id.toString())
            Text(text = task.manufacturer)
            Text(text = task.dosage)
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 120)
@Composable
fun ArtifactsTaskListItemDetailsPreview(){
    CombinedAndroidTheme() {
        var taskOne = ArtifactsTaskObject()
        taskOne.id = 89
        taskOne.sampleId = "22ETCL-17"
        taskOne.drug = "Acetaminophen"
        taskOne.manufacturer = "Pfizer"
        taskOne.dosage = "12.0"
        ArtifactsTaskListItemDetail(task = taskOne)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 120)
@Composable
fun AtrifactsTaskListItemPreview(){
    CombinedAndroidTheme() {
        var taskOne = ArtifactsTaskObject()
        taskOne.id = 89
        taskOne.sampleId = "22ETCL-17"
        taskOne.drug = "Acetaminophen"
        taskOne.manufacturer = "Pfizer"
        taskOne.dosage = "12.0"
        ArtifactsTaskListItem(task = taskOne, startCamera = {})

    }

}

@Composable
fun ArtifactsTaskList(modifier: Modifier = Modifier,
                      drugs: List<ArtifactsTaskObject> = List<ArtifactsTaskObject>(100){ ArtifactsTaskObject() },
                      startCamera: () -> Unit){
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)){
        items(items = drugs){drug ->
            ArtifactsTaskListItem(task = drug, startCamera = startCamera)
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 420)
@Composable
fun ArtifactsTaskListPreview(){

    //var tasks = List<ArtifactsTaskObject>(3)
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
        ArtifactsTaskList(drugs = taskObjects, startCamera = {})
    }

}

@Composable
fun ArtifactsTaskView(modifier: Modifier = Modifier, vm: ArtifactsTasksViewModel, token: String, baseUrl: String, startCamera: () -> Unit){
    Surface() {
        Column {
            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "ARTIFACTS")
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "Tasks")
            }
            BasicTextField( value = "Search", onValueChange = {})
            if(vm.errorMessage.isEmpty()){
                vm.getResultsAsObjects()
                ArtifactsTaskList(modifier = Modifier.weight(1f), drugs = vm.taskObjects, startCamera = startCamera)
            }else{
                Text(text = vm.errorMessage)
            }
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(1f), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Previous page")
                }
                ElevatedButton(
                    onClick = {
                        vm.getTasksList(token = token, baseUrl = baseUrl, page = 1)

                    }) {
                    Text(text = "REFRESH")
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next page")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun ArtifactswTaskViewPreview(){
    val vm = ArtifactsTasksViewModel()
    CombinedAndroidTheme() {
        ArtifactsTaskView(vm = vm, token = "", baseUrl = "", startCamera = {})
    }
}

/*@Composable
fun ArtifactsTaskPage(refreshCallback: () -> Unit, modifier: Modifier = Modifier, drugs: List<ArtifactsTaskObject> = List<ArtifactsTaskObject>(100){ ArtifactsTaskObject() }){
    Surface() {
        Column {
            Text(text = "Tasks")
            ArtifactsTaskList(modifier = Modifier.weight(1f), drugs = drugs, startCamera = startCamera)
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
