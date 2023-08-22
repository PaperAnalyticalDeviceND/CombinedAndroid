package edu.nd.crc.paperanalyticaldevices

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import edu.nd.crc.paperanalyticaldevices.api.ArtifactsAPIService
import edu.nd.crc.paperanalyticaldevices.api.ScreenerTasksList
import edu.nd.crc.paperanalyticaldevices.api.TasksList
import kotlinx.coroutines.launch
import java.io.File


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
                grantType: String,
                tenantType: String){
        Log.d("ARTIFACTS", "authVm getAuth $baseUrl")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                val authResponse = apiService.getAuth(clientId = clientId, code = code,
                    codeVerifier = codeVerifier, redirectUri = redirectUri,
                    grantType = grantType, tenantType = tenantType)
                _authToken.value = authResponse.AccessToken
                Log.d("ARTIFACTS", "Auth token: ${_authToken.value}")
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.e("ARTIFACTS", errorMessage)
            }
        }
    }
}

class ArtifactsTaskDisplayModel(val id: Int, val sampleId: String,val name: String,
                                val drug: String,
                                val manufacturer: String, val expectedMainSubstances: String,
                                val expectedOtherSubstances: String,
                                val doseType: String, val dose: String,
                                val type: String = "legal_drugs",
                                val initialSelectedValue: Boolean){
    var selected by mutableStateOf(initialSelectedValue)
}

/*class ScreenerTaskDisplayModel(val id: Int, val sampleId: String, val name: String,
                               val expectedMainSubstances: String, val expectedOtherSubstances: String,
                               val doseType: String, val initialSelectedValue: Boolean){
    var selected by mutableStateOf(initialSelectedValue)
}*/

class NetworksDisplayModel(val network: String, val initialSelectedValue: Boolean){
    var selected by mutableStateOf(initialSelectedValue)
}


abstract class PadsTaskViewModel : ViewModel() {
    val _taskList = mutableStateListOf<ArtifactsTaskDisplayModel>()

    var errorMessage: String by mutableStateOf("")

    val taskList: List<ArtifactsTaskDisplayModel>
        get() = _taskList

    var taskConfirmed by mutableStateOf(false)

    var currentPage: Int by mutableStateOf(1)

    var next: Int by mutableStateOf(0)
    var previous: Int by mutableStateOf(0)

    abstract fun getTasksList(token: String, baseUrl: String, page: Int)

    abstract fun getNextPage(token: String, baseUrl: String)

    abstract fun getPreviousPage(token: String, baseUrl: String)

    abstract fun selectTask(selectedTask: ArtifactsTaskDisplayModel)

    abstract fun confirmTask(selectedTask: ArtifactsTaskDisplayModel)

    abstract fun clearConfirmation()

    abstract fun getSelected(): ArtifactsTaskDisplayModel?
}
class ArtifactsTasksViewModel : PadsTaskViewModel() {
    //private val _taskList = mutableStateListOf<ArtifactsTaskDisplayModel>()

    /*var errorMessage: String by mutableStateOf("")

    val taskList: List<ArtifactsTaskDisplayModel>
        get() = _taskList

    var taskConfirmed by mutableStateOf(false)

    var currentPage: Int by mutableStateOf(1)

    var next: Int by mutableStateOf(0)
    var previous: Int by mutableStateOf(0)*/

    override fun getTasksList(token: String, baseUrl: String, page: Int){
        Log.d("ARTIFACTS", "getTasksList")
        Log.d("ARTIFACTS", "Token: $token, baseUrl: $baseUrl")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getTasks(token = "Bearer $token", status = "awaiting", page = page)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    override fun getNextPage(token: String, baseUrl: String){
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getTasks(token = "Bearer $token", status = "awaiting", page = next)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    override fun getPreviousPage(token: String, baseUrl: String){
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getTasks(token = "Bearer $token", status = "awaiting", page = previous)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    private fun getResultsAsObjects(tasks: TasksList): Collection<ArtifactsTaskDisplayModel> {

        var taskCollection = ArrayList<ArtifactsTaskDisplayModel>()
        Log.d("ARTIFACTS", tasks.toString())
        for(r in tasks.Results){
            var obj = ArtifactsTaskDisplayModel(id = r.Id, sampleId = r.Sample, name = r.MainAPIs[0].Name,
                drug = r.MainAPIs[0].Name, manufacturer = if(null != r.Manufacturer) r.Manufacturer.Name else "",
                dose = r.Dosage + "" + r.dosageType.Name, doseType = r.dosageType.Name, expectedMainSubstances = r.MainAPIs[0].Name,
                expectedOtherSubstances = "",
                initialSelectedValue = false)

            //DEBUG TESTING
            /*var obj2 = ArtifactsTaskDisplayModel(id = r.Id * 4, sampleId = "1234",
                drug = "Losartan", manufacturer = if(null != r.Manufacturer) r.Manufacturer.Name else "",
                dose = r.Dosage + "" + r.dosageType.Name,
                initialSelectedValue = false)

            var obj3 = ArtifactsTaskDisplayModel(id = r.Id * 7, sampleId = "abcd",
                drug = "Aluterol", manufacturer = if(null != r.Manufacturer) r.Manufacturer.Name else "",
                dose = r.Dosage + "" + r.dosageType.Name,
                initialSelectedValue = false)*/

            taskCollection.add(obj)
            //DEBUG TESTING
            /*taskCollection.add(obj2)
            taskCollection.add(obj3)*/
        }
        if(tasks.Links != null){
            if(tasks.Links.next != null && tasks.CountPages > currentPage){
                //next = tasks.Links.next
                next = currentPage + 1
            }
            if(tasks.Links.previous != null && currentPage > 1){
                //previous = tasks.Links.previous
                previous = currentPage - 1
            }
        }
        return taskCollection
    }

    override fun selectTask(selectedTask: ArtifactsTaskDisplayModel){
        Log.d("ARTIFACTS", "Select Task")
        _taskList.forEach { it.selected = false }
        _taskList.find { it.id == selectedTask.id }?.selected = true
    }

    override fun confirmTask(selectedTask: ArtifactsTaskDisplayModel){
        Log.d("ARTIFACTS", "Confirm Task")
        taskConfirmed = true
    }

    override fun clearConfirmation(){
        taskConfirmed = false
    }

    override fun getSelected(): ArtifactsTaskDisplayModel? {
        return _taskList.find{ it.selected }
    }
}

class ScreenerTaskViewModel() : PadsTaskViewModel() {
    //private val _taskList = mutableStateListOf<ArtifactsTaskDisplayModel>()

    /*var errorMessage: String by mutableStateOf("")

    val taskList: List<ArtifactsTaskDisplayModel>
        get() = _taskList

    var taskConfirmed by mutableStateOf(false)

    var currentPage: Int by mutableStateOf(1)

    var next: Int by mutableStateOf(0)
    var previous: Int by mutableStateOf(0)*/

    private fun getResultsAsObjects(tasks: ScreenerTasksList): Collection<ArtifactsTaskDisplayModel> {
        var taskCollection = ArrayList<ArtifactsTaskDisplayModel>()

        Log.d("ARTIFACTS", tasks.toString())
        for(r in tasks.Results){
            Log.d("ARTIFACTS", r.toString())
            var obj = ArtifactsTaskDisplayModel(id = r.Id, sampleId = r.Sample,
                drug = if(null != r.ExpectedMainSubstances) r.ExpectedMainSubstances else "",
                name = r.Name,
                expectedMainSubstances = if(null != r.ExpectedMainSubstances) r.ExpectedMainSubstances else "",
                expectedOtherSubstances = if(null != r.ExpectedOtherSubstances) r.ExpectedOtherSubstances else "",
                dose = "", manufacturer = "",
                doseType = if(null != r.dosageType) r.dosageType.Name else "", type = "street_drugs", initialSelectedValue = false)

            taskCollection.add(obj)
        }
        if(tasks.Links != null){
            if(tasks.Links.next != null && tasks.CountPages > currentPage){
                //next = tasks.Links.next
                next = currentPage + 1
            }
            if(tasks.Links.previous != null && currentPage > 1){
                //previous = tasks.Links.previous
                previous = currentPage - 1
            }
        }
        return taskCollection
    }

    override fun getTasksList(token: String, baseUrl: String, page: Int){
        Log.d("ARTIFACTS", "getTasksList")
        Log.d("ARTIFACTS", "Token: $token, baseUrl: $baseUrl")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getScreenerTasks(token = "Bearer $token", status = "awaiting", page = page)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    override fun getNextPage(token: String, baseUrl: String){
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getScreenerTasks(token = "Bearer $token", status = "awaiting", page = next)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    override fun getPreviousPage(token: String, baseUrl: String){
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getScreenerTasks(token = "Bearer $token", status = "awaiting", page = previous)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Task error response: $errorMessage")
            }
        }
    }

    override fun selectTask(selectedTask: ArtifactsTaskDisplayModel){
        Log.d("ARTIFACTS", "Select Task")
        _taskList.forEach { it.selected = false }
        _taskList.find { it.id == selectedTask.id }?.selected = true
    }

    override fun confirmTask(selectedTask: ArtifactsTaskDisplayModel){
        Log.d("ARTIFACTS", "Confirm Task")
        taskConfirmed = true
    }

    override fun clearConfirmation(){
        taskConfirmed = false
    }

    override fun getSelected(): ArtifactsTaskDisplayModel? {
        return _taskList.find{ it.selected }
    }
}


class NetworksViewModel(application: Application) : AndroidViewModel(application) {
    private val _networksList = mutableStateListOf<NetworksDisplayModel>()

    var networkSelected by mutableStateOf(false)

    //val sharedPreferences = application.getSharedPreferences("default", 0)
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val context = getApplication<Application>().applicationContext
    val networkList: List<NetworksDisplayModel>
        get() = _networksList

    fun getNetworks(task: ArtifactsTaskDisplayModel, dbHelper: ProjectsDbHelper){
        viewModelScope.launch{
            val db = dbHelper.readableDatabase
            var cursor: Cursor? = db?.rawQuery(
                "SELECT DISTINCT(${DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK}) from ${DrugsContract.DrugsEntry.TABLE_NAME} WHERE ${DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME} = ?",
                 Array<String>(1){task.drug} )
            _networksList.clear()
            if(null != cursor){
                while(cursor.moveToNext()){
                    val network = cursor.getString(cursor.getColumnIndexOrThrow(DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK))
                    if(haveNetwork(sharedPrefs, network = network)){
                        _networksList.add(NetworksDisplayModel(network = network, initialSelectedValue = false))
                    }
                }
            }
        }
    }

    fun getConcNetwork(task: ArtifactsTaskDisplayModel) : NetworksDisplayModel {
        var concNet = NetworksDisplayModel(network = "None", initialSelectedValue = false)
        for (n in _networksList){
            if(n.network.contains("conc")){
                concNet = NetworksDisplayModel(network = n.network, initialSelectedValue = false)
            }
        }
        return concNet
    }

    private fun haveNetwork(sharedPreferences: SharedPreferences, network: String) : Boolean{
        val networkFile: File = File(
            getApplication<Application>().applicationContext
                .getDir("tflitemodels", Context.MODE_PRIVATE).getPath(),
            sharedPreferences.getString(network + "filename", "notafile")
        )
        return networkFile.exists()
    }

    fun selectNetwork(selectedNetwork: NetworksDisplayModel){
        _networksList.forEach { it.selected = false }
        _networksList.find { it.network == selectedNetwork.network }?.selected = true
        networkSelected = true
    }

    fun resetSelected(){
        _networksList.forEach { it.selected = false }
    }

    fun getSelected(): NetworksDisplayModel? {
        return _networksList.find { it.selected }
    }
}


class ArtifactsResultViewModel(application: Application) : ViewModel() {

    var errorMessage: String by mutableStateOf("")

    private val workManager = WorkManager.getInstance(application)
    fun sendResult(context: Context, authToken: String, baseUrl: String, timestamp: String,
                   testDate: String, taskId: Int,
                   taskNotes: String, result: String, rectFileUri: Uri,
                   rawFileUri: Uri, rectFile: File, rawFile: File){
        viewModelScope.launch {

            Log.d("ARTIFACTS", "Starting Artifacts send")
            try{

                val re = sendArtifactsResult(authToken = "Bearer $authToken", baseUrl = baseUrl,
                    taskId = taskId, timestamp = timestamp, testDate = testDate,
                    taskNotes = taskNotes, result = result)


            }catch(e: Exception){
                errorMessage = e.message.toString()
                Log.d("ARTIFACTS", "Send Error: $errorMessage")
            }
        }

    }

    private fun sendArtifactsResult(authToken: String, baseUrl: String, taskId: Int,
                            timestamp: String, testDate: String,
                            taskNotes: String, result: String){
        val inputData = Data.Builder()
            .putString(AUTH_TOKEN, authToken)
            .putString(BASE_URL, baseUrl)
            .putInt(TASK_ID, taskId)
            .putString(TIMESTAMP, timestamp)
            .putString(TEST_DATE, testDate)
            .putString(TASK_NOTES, taskNotes)
            .putString(TASK_RESULT, result)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ArtifactsWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueue(oneTimeWorkRequest)
    }
}

class ArtifactsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if(modelClass.isAssignableFrom(ArtifactsResultViewModel::class.java)){
            ArtifactsResultViewModel(application) as T
        }else{
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class NetworksViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if(modelClass.isAssignableFrom((NetworksViewModel::class.java))){
            NetworksViewModel(application) as T
        }else{
            throw java.lang.IllegalArgumentException("Unknown ViewModel class")
        }
    }
}