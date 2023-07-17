package edu.nd.crc.paperanalyticaldevices

import android.database.Cursor
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.nd.crc.paperanalyticaldevices.api.ArtifactsAPIService
import edu.nd.crc.paperanalyticaldevices.api.TasksList
import kotlinx.coroutines.launch


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

class ArtifactsTaskDisplayModel(val id: Int, val sampleId: String, val drug: String, val manufacturer: String, val dose: String, val initialSelectedValue: Boolean){
    var selected by mutableStateOf(initialSelectedValue)
}

class NetworksDisplayModel(val network: String, val initialSelectedValue: Boolean){
    var selected by mutableStateOf(initialSelectedValue)
}

class ArtifactsTasksViewModel : ViewModel() {
    private val _taskList = mutableStateListOf<ArtifactsTaskDisplayModel>()

    var errorMessage: String by mutableStateOf("")
    val taskList: List<ArtifactsTaskDisplayModel>
        get() = _taskList

    var taskConfirmed by mutableStateOf(false)

    fun getTasksList(token: String, baseUrl: String, page: Int){
        Log.d("ARTIFACTS", "getTasksList")
        Log.d("ARTIFACTS", "Token: $token, baseUrl: $baseUrl")
        viewModelScope.launch {
            val apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)
            try{
                _taskList.clear()
                _taskList.addAll(getResultsAsObjects(apiService.getTasks(token = "Bearer $token", status = "awaiting", page = page)))
            }catch(e: Exception){
                errorMessage = e.message.toString()
            }
        }
    }

    private fun getResultsAsObjects(tasks: TasksList): Collection<ArtifactsTaskDisplayModel> {

        var taskCollection = ArrayList<ArtifactsTaskDisplayModel>()

        for(r in tasks.Results){
            var obj = ArtifactsTaskDisplayModel(id = r.Id, sampleId = r.Sample,
                drug = r.MainAPIs[0].Name, manufacturer = if(null != r.Manufacturer) r.Manufacturer.Name else "",
                dose = r.Dosage + "" + r.dosageType.Name,
                initialSelectedValue = false)

            taskCollection.add(obj)
        }
        return taskCollection
    }

    fun selectTask(selectedTask: ArtifactsTaskDisplayModel){
        _taskList.forEach { it.selected = false }
        _taskList.find { it.id == selectedTask.id }?.selected = true
    }

    fun confirmTask(selectedTask: ArtifactsTaskDisplayModel){
        taskConfirmed = true
    }

    fun clearConfirmation(){
        taskConfirmed = false
    }

    fun getSelected(): ArtifactsTaskDisplayModel? {
        return _taskList.find{ it.selected }
    }
}

class NetworksViewModel : ViewModel() {
    private val _networksList = mutableStateListOf<NetworksDisplayModel>()

    var networkSelected by mutableStateOf(false)
    val networkList: List<NetworksDisplayModel>
        get() = _networksList

    fun getNetworks(task: ArtifactsTaskDisplayModel, dbHelper: ProjectsDbHelper){
        viewModelScope.launch{
            val db = dbHelper.readableDatabase
            var cursor: Cursor? = db?.rawQuery("SELECT DISTINCT(${DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK}) from ${DrugsContract.DrugsEntry.TABLE_NAME} WHERE ${DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME} = ?",
                 Array<String>(1){task.drug} )
            _networksList.clear()
            if(null != cursor){
                while(cursor.moveToNext()){
                    val network = cursor.getString(cursor.getColumnIndexOrThrow(DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK))
                    _networksList.add(NetworksDisplayModel(network = network, initialSelectedValue = false))
                }
            }
        }
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