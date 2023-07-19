package edu.nd.crc.paperanalyticaldevices

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import edu.nd.crc.paperanalyticaldevices.ArtifactsActivity.Companion.EXTRA_AUTH_TOKEN
import edu.nd.crc.paperanalyticaldevices.ArtifactsActivity.Companion.EXTRA_BASE_URL
import edu.nd.crc.paperanalyticaldevices.ArtifactsActivity.Companion.EXTRA_NEURAL_NET
import edu.nd.crc.paperanalyticaldevices.ArtifactsActivity.Companion.EXTRA_TASK_ID
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

class DualResultActivity : AppCompatActivity(){

    val notesVersion = 1
    val buildNumber = 17 // update this to follow the gradle version code


    var mPreferences: SharedPreferences? = null
    var defaultPrefs: SharedPreferences? = null

    var qr = ""
    var timestamp = ""
    var predictedDrug = ""
    var probability = 0.000
    var nnConcentration = 0
    var plsConc = 0
    var plsUsed = false

    var unsafeForConsumption = false

    var neuralnet = ""

    var statedDrug = ""

    // Artifacts stuff
    var authToken = ""

    var artifactsTaskId = 0

    var baseUrl = "api-pad.artifactsofresearch.io"

    var newDate = ""

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)


        // Setup Preferences
        mPreferences = getSharedPreferences(MainActivity.PROJECT, MODE_PRIVATE)
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        var resultIntent = intent
        var imageData = intent.data
        var sPredicted = ""
        if(intent.hasExtra(MainActivity.EXTRA_PREDICTED)){
            sPredicted = intent.getStringExtra(MainActivity.EXTRA_PREDICTED).toString()
        }
        if(intent.hasExtra(MainActivity.EXTRA_SAMPLEID)){
            qr = parseQR(intent.getStringExtra(MainActivity.EXTRA_SAMPLEID)!!)!!
        }

        if (intent.hasExtra(MainActivity.EXTRA_NN_CONC)) {
            //nnConcentration = intent.getStringExtra(MainActivity.EXTRA_NN_CONC);
            nnConcentration = intent.getIntExtra(MainActivity.EXTRA_NN_CONC, 0)
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLS_CONC)) {
            //plsConc = intent.getStringExtra(MainActivity.EXTRA_PLS_CONC);
            plsConc = intent.getIntExtra(MainActivity.EXTRA_PLS_CONC, 0)
        }

        if (intent.hasExtra(MainActivity.EXTRA_PREDICTED_DRUG) && intent.getStringExtra(MainActivity.EXTRA_PREDICTED_DRUG) != null) {
            predictedDrug = intent.getStringExtra(MainActivity.EXTRA_PREDICTED_DRUG)!!
        }

        if (intent.hasExtra(MainActivity.EXTRA_PROBABILITY)) {
            //probability = intent.getStringExtra(MainActivity.EXTRA_PROBABILITY);
            probability = intent.getDoubleExtra(MainActivity.EXTRA_PROBABILITY, 0.000)
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLS_USED)) {
            plsUsed = intent.getBooleanExtra(MainActivity.EXTRA_PLS_USED, false)
        }

        if (intent.hasExtra(EXTRA_AUTH_TOKEN)) {
            authToken = intent.getStringExtra(EXTRA_AUTH_TOKEN)!!
        }

        if (intent.hasExtra(EXTRA_BASE_URL)) {
            baseUrl = intent.getStringExtra(EXTRA_BASE_URL)!!
        }

        if (intent.hasExtra(EXTRA_TASK_ID)) {
            artifactsTaskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        }

        neuralnet = if (intent.hasExtra(EXTRA_NEURAL_NET)) {
            intent.getStringExtra(EXTRA_NEURAL_NET)!!
        } else {
            defaultPrefs!!.getString("neuralnet", "None")!!
        }

        if(intent.hasExtra(MainActivity.EXTRA_TIMESTAMP)){
            timestamp = intent.getStringExtra(MainActivity.EXTRA_TIMESTAMP)!!
            val javaTimestamp = Timestamp(timestamp.toLong())
            val date = Date(javaTimestamp.time)

            val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
            newDate = sdf.format(date);
        }

        if(intent.hasExtra(MainActivity.EXTRA_STATED_DRUG) && intent.getStringExtra(MainActivity.EXTRA_STATED_DRUG) != null){
            statedDrug = intent.getStringExtra(MainActivity.EXTRA_STATED_DRUG).toString()
        }

        unsafeForConsumption = predictedDrug.substring(0, statedDrug.length) != statedDrug

        val artifactsVm = ArtifactsResultViewModel()

        setContent {
            ResultFieldsView(
                imageUri = imageData!!,
                stateDrug = statedDrug,
                predictedDrug = predictedDrug,
                predictedConc = nnConcentration.toString(),
                safe = if(unsafeForConsumption) "Unsafe" else "Safe",
                vm = artifactsVm
            )
        }
    }

    private fun parseQR(qr: String): String? {
        return if (qr.startsWith("padproject.nd.edu/?s=") || qr.startsWith("padproject.nd.edu/?t=")) {
            qr.substring(21)
        } else qr
    }
}