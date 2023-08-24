package edu.nd.crc.paperanalyticaldevices

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import edu.nd.crc.paperanalyticaldevices.api.ArtifactsAPIService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject


class ArtifactsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val baseUrl = inputData.getString(BASE_URL)
        val authToken = inputData.getString(AUTH_TOKEN)
        val taskId = inputData.getInt(TASK_ID, 0)
        val timestamp = inputData.getString(TIMESTAMP)
        val result = inputData.getString(TASK_RESULT)
        val taskNotes = inputData.getString(TASK_NOTES)
        val testDate = inputData.getString(TEST_DATE)
        val tenantType = inputData.getString(TENANT_TYPE)
        val predictedDrug = inputData.getString(PREDICTED_DRUG)
        val concentration = inputData.getString(CONCENTRATION)

        return try{

            if(tenantType == "street_drugs"){
                uploadScreenerData(authToken = authToken!!, baseUrl = baseUrl!!, taskId = taskId,
                    timestamp = timestamp!!, testDate = testDate!!, taskNotes = taskNotes!!,
                predictedDrug = predictedDrug!!, concentration = concentration!!)
            }else {
                uploadArtifactsData(
                    authToken = authToken!!, baseUrl = baseUrl!!, taskId = taskId,
                    timestamp = timestamp!!, testDate = testDate!!, taskNotes = taskNotes!!,
                    result = result!!
                )
            }
            Result.success()
        } catch(throwable: Throwable){
            Result.failure()
        }
    }

    private fun uploadArtifactsData(authToken: String, baseUrl: String, taskId: Int,
                                    timestamp: String, testDate: String,
                                    taskNotes: String, result: String): Boolean {
        val targetDir: File = File(applicationContext.filesDir, timestamp)
        val rectifiedFile = File(targetDir, "rectified.png")
        val originalFile = File(targetDir, "original.png")
        val dateField = prepareStringPart("test_date", testDate)
        val taskNotesField = prepareStringPart("task_notes", taskNotes)
        val resultField = prepareStringPart("result", result)
        val rectFileField = prepareFilePart( "files",  rectifiedFile)
        val rawFileField = prepareFilePart( "files",  originalFile)
        var apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)

        val re = apiService.sendArtifactsResult(token = authToken, taskId = taskId,
            rectFile = rectFileField, rawFile = rawFileField, testDate = dateField!!,
            taskNotes = taskNotesField!!, result = resultField!!)
        Log.d("ARTIFACTS", "Executing API call")
        val response = re.execute()
        Log.d("ARTIFACTS", response.toString())

        return true
    }

    private fun uploadScreenerData(authToken: String, baseUrl: String, taskId: Int,
                                   timestamp: String, testDate: String,
                                   taskNotes: String, predictedDrug: String,
                                    concentration: String): Boolean {
        val targetDir: File = File(applicationContext.filesDir, timestamp)
        val rectifiedFile = File(targetDir, "rectified.png")
        val originalFile = File(targetDir, "original.png")
        val dateField = prepareStringPart("test_date", testDate)
        val taskNotesField = prepareStringPart("task_notes", taskNotes)

        var drugObj = JSONObject()
        drugObj.put("name", predictedDrug)
        //drugObj.put("precentage", concentration)
        var determinedObj = JSONObject()
        determinedObj.put("is_not_determined", true)
        var substanceArray = JSONArray()
        substanceArray.put(drugObj)
        //substanceArray.put(determinedObj)
        val substanceString = substanceArray.toString()
        Log.d("ARTIFACTS", substanceString)
        val substancesField = prepareStringPart("substances", substanceString)
        val fieldString = substancesField.body.toString()
        Log.d("ARTIFACTS", fieldString)

        val rectFileField = prepareFilePart( "files",  rectifiedFile)
        val rawFileField = prepareFilePart( "files",  originalFile)
        var apiService = ArtifactsAPIService.getInstance(baseUrl = baseUrl)

        val re = apiService.sendScreenerResult(token = authToken, taskId = taskId,
            rectFile = rectFileField, rawFile = rawFileField, testDate = dateField!!,
            taskNotes = taskNotesField!!, substances = substancesField)
        Log.d("ARTIFACTS", "Executing Screener API call")
        val response = re.execute()
        Log.d("ARTIFACTS", response.toString())

        return true
    }

    private fun prepareStringPart(partName: String, text: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(partName, text)
    }

    private fun prepareFilePart(partName: String, file: File): MultipartBody.Part {

        val requestFile = file.asRequestBody("image/png".toMediaType())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }
}