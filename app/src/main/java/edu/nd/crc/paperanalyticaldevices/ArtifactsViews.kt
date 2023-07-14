package edu.nd.crc.paperanalyticaldevices

import android.util.Log
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
import edu.nd.crc.paperanalyticaldevices.ui.theme.CombinedAndroidTheme

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
                       onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                       testPressed: (ArtifactsTaskDisplayModel) -> Unit){

    LaunchedEffect(Unit, block = {
        authVm.getAuth(baseUrl, code, codeVerifier, redirectUri, clientId, grantType)
    })

    //var authorized by rememberSaveable { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf(0) }

    Surface() {
        if(authVm.authToken.isNotEmpty()){
            Log.d("ARTIFACTS", "baseUrl $baseUrl")
            Log.d("ARTIFACTS", "auth token ${authVm.authToken}")
            ArtifactsTaskView(vm = taskVm, token = authVm.authToken, baseUrl = baseUrl,
                onItemClicked = onItemClicked, testPressed = testPressed)
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
fun ArtifactsTaskListItem(modifier: Modifier = Modifier, task: ArtifactsTaskDisplayModel,
                          onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                          testPressed: (ArtifactsTaskDisplayModel) -> Unit){
    var expanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    Surface(color = if(task.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .padding(4.dp)
            .clickable(true, onClick = { onItemClicked(task) })
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
                    Icon(imageVector = if(expanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand")
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
                if(task.selected){
                    ElevatedButton(onClick =
                    //{cameraResult.launch(Intent(this, Camera2Activity::class.java))}
                    //{context.startActivity( Intent(context, Camera2Activity::class.java))}
                    { testPressed(task) }
                    ) {
                        Text(text = "Test")
                    }
                }
            }
            if(expanded) {
                ArtifactsTaskListItemDetail(task = task)
            }
        }
    }
}

@Composable
fun ArtifactsTaskListItemDetail(modifier: Modifier = Modifier, task: ArtifactsTaskDisplayModel){
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
            Text(text = task.dose)
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 120)
@Composable
fun ArtifactsTaskListItemDetailsPreview(){
    CombinedAndroidTheme() {
        /*var taskOne = ArtifactsTaskObject()
        taskOne.id = 89
        taskOne.sampleId = "22ETCL-17"
        taskOne.drug = "Acetaminophen"
        taskOne.manufacturer = "Pfizer"
        taskOne.dosage = "12.0"*/
        var taskOne = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-17",
            drug = "Acetaminophen", manufacturer = "Pfizer", dose = "12.00 mg",
            initialSelectedValue = false)
        ArtifactsTaskListItemDetail(task = taskOne)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 120)
@Composable
fun AtrifactsTaskListItemPreview(){
    CombinedAndroidTheme() {
        var taskOne = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-17",
            drug = "Acetaminophen", manufacturer = "Pfizer", dose = "12.00 mg",
            initialSelectedValue = false)
        ArtifactsTaskListItem(task = taskOne, onItemClicked = {}, testPressed = {})

    }

}

@Composable
fun ArtifactsTaskList(modifier: Modifier = Modifier,
                      drugs: List<ArtifactsTaskDisplayModel>,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit){
    LazyColumn(modifier = modifier.padding(vertical = 4.dp)){
        items(items = drugs, key = { it.id }){drug ->
            ArtifactsTaskListItem(task = drug, onItemClicked = onItemClicked, testPressed = testPressed)
        }
    }
}

/*@Preview(showBackground = true, widthDp = 320, heightDp = 420)
@Composable
fun ArtifactsTaskListPreview(){

    //var tasks = List<ArtifactsTaskObject>(3)
    CombinedAndroidTheme() {
        var taskObjects = ArrayList<ArtifactsTaskDisplayModel>()
        var taskOne = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-17",
            drug = "Acetaminophen", manufacturer = "Pfizer", dose = "12.00 mg",
            initialSelectedValue = false)
        taskObjects.add(taskOne)
        var taskTwo = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-27",
            drug = "Albuterol", manufacturer = "Pfizer", dose = "1.00 mg",
            initialSelectedValue = false)
        taskObjects.add(taskTwo)
        ArtifactsTaskList(drugs = taskObjects, onItemClicked = {}, testPressed = {})
    }

}*/

@Composable
fun ArtifactsTaskView(modifier: Modifier = Modifier, vm: ArtifactsTasksViewModel,
                      token: String, baseUrl: String,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit){
    Surface() {
        Column {
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text(text = "ARTIFACTS")
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text(text = "Tasks")
            }
            BasicTextField(modifier = Modifier.fillMaxWidth(),
                value = "Search", onValueChange = {})
            if(vm.errorMessage.isEmpty()){
                //vm.getResultsAsObjects()
                ArtifactsTaskList(modifier = Modifier.weight(1f),
                    drugs = vm.taskList, onItemClicked = onItemClicked, testPressed = testPressed)
            }else{
                Text(text = vm.errorMessage)
            }
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(1f), horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous page")
                }
                ElevatedButton(
                    onClick = {
                        vm.getTasksList(token = token, baseUrl = baseUrl, page = 1)

                    }) {
                    Text(text = "REFRESH")
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next page")
                }
            }
        }
    }
}

/*@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun ArtifactswTaskViewPreview(){
    val vm = ArtifactsTasksViewModel()
    CombinedAndroidTheme() {
        ArtifactsTaskView(vm = vm, token = "", baseUrl = "", onItemClicked = {}, testPressed = {})
    }
}*/


@Composable
fun ArtifactsResultView(modifier: Modifier = Modifier, vm: ArtifactsTasksViewModel){
    val task = vm.getSelected()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column() {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = task!!.sampleId)
            }
        }
    }
}

@Composable
fun NetworksList(modifier: Modifier = Modifier,
                 networks: List<NetworksDisplayModel>,
                 taskVm: ArtifactsTasksViewModel,
                 onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel) -> Unit){
    Surface() {
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)){
            items(items = networks){ item ->
                NetworksListItem(network = item, taskVm = taskVm, onItemClicked = onItemClicked)
            }
        }
    }
}

@Composable
fun NetworkListView(modifier: Modifier = Modifier, networkViewModel: NetworksViewModel,
                    taskViewModel: ArtifactsTasksViewModel,
                    dbHelper: ProjectsDbHelper,
                    onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel) -> Unit){
    LaunchedEffect(Unit, block = {
        networkViewModel.getNetworks(taskViewModel.getSelected()!!, dbHelper = dbHelper)
    })

    Surface(modifier = Modifier.padding(8.dp)){
        Column() {
            Text(text = "Select Neural Network")
            NetworksList(networks = networkViewModel.networkList, taskVm = taskViewModel, onItemClicked = onItemClicked)
        }
    }
}

@Composable
fun NetworksListItem(modifier: Modifier = Modifier,
                     network: NetworksDisplayModel,
                     taskVm: ArtifactsTasksViewModel,
                     onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel) -> Unit){
    Surface(modifier = Modifier
        .padding(4.dp)
        .clickable(true,
            onClick = { onItemClicked(taskVm.getSelected()!!, network) }),
        color = MaterialTheme.colorScheme.primary) {

        Row(modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = network.network)
        }

    }
}

/*@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun NetworksListPreview(){
    CombinedAndroidTheme() {
        val networks: Array<NetworksDisplayModel> = arrayOf(NetworksDisplayModel(network = "fh360", initialSelectedValue = false),
            NetworksDisplayModel(network = "mzTanzania", initialSelectedValue = false),
            NetworksDisplayModel(network = "idPads", initialSelectedValue = false))
        NetworksList(networks = networks.toList(), taskVm = ArtifactsTasksViewModel(), onItemClicked = {})
    }
}*/

@Composable
fun ArtifactsMainView(modifier: Modifier = Modifier,
                      baseUrl: String,
                      clientId: String,
                      redirectUri: String,
                      code: String,
                      codeVerifier: String,
                      grantType: String,
                      authVm: ArtifactsAuthViewModel,
                      taskVm: ArtifactsTasksViewModel,
                      networksVm: NetworksViewModel,
                      dbHelper: ProjectsDbHelper,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit,
                      onNetworkPressed: (ArtifactsTaskDisplayModel, NetworksDisplayModel) -> Unit){
    if(taskVm.taskConfirmed){
        NetworkListView(
            networkViewModel = networksVm,
            taskViewModel = taskVm,
            dbHelper = dbHelper,
            onItemClicked = onNetworkPressed
        )
    }else{
        ArtifactsLoginView(
            baseUrl = baseUrl,
            clientId = clientId,
            redirectUri = redirectUri,
            code = code,
            codeVerifier = codeVerifier,
            grantType = grantType,
            authVm = authVm,
            taskVm = taskVm,
            onItemClicked = onItemClicked,
            testPressed = testPressed
        )
    }
}

@Composable
fun TestNetworkListItem(network: String){
    Surface(modifier = Modifier.padding(4.dp),
        color = MaterialTheme.colorScheme.primary) {

        Row(modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = network)
        }

    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 50)
@Composable
fun TestNetworkListItemPreview(){
    CombinedAndroidTheme() {
        TestNetworkListItem(network = "testing_network")
        
    }
}

@Composable
fun TestNetworkList(networks: List<String>){
    Surface() {
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)){
            items(items = networks){ item ->
                TestNetworkListItem(network = item)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 250)
@Composable
fun TestNetworkListPreview(){
    val networks = ArrayList<String>()
    networks.add("fhi360")
    networks.add("tanzania")
    networks.add("idpads")
    CombinedAndroidTheme() {
        TestNetworkList(networks = networks)
    }
}