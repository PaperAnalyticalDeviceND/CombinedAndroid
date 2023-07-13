package edu.nd.crc.paperanalyticaldevices

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

class ArtifactsTasksViewModel : ViewModel() {
    private val _taskList = mutableStateListOf<ArtifactsTaskDisplayModel>()
    //private val _taskObjects = mutableStateListOf<ArtifactsTaskObject>()
    var errorMessage: String by mutableStateOf("")
    val taskList: List<ArtifactsTaskDisplayModel>
        get() = _taskList

    //val taskObjects: List<ArtifactsTaskObject>
    //    get() = _taskObjects

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
}