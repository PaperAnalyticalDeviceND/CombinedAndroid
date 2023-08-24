package edu.nd.crc.paperanalyticaldevices

import android.content.Intent


import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import edu.nd.crc.paperanalyticaldevices.ui.theme.CombinedAndroidTheme


@Composable
fun ArtifactsLoginView(modifier: Modifier = Modifier,
                       baseUrl: String,
                       clientId: String,
                       redirectUri: String,
                       code: String,
                       codeVerifier: String,
                       grantType: String,
                       tenantType: String,
                       authVm: ArtifactsAuthViewModel,
                       taskVm: PadsTaskViewModel,
                       onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                       testPressed: (ArtifactsTaskDisplayModel) -> Unit){

    LaunchedEffect(Unit, block = {
        authVm.getAuth(baseUrl, code, codeVerifier, redirectUri, clientId, grantType, tenantType)
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
            Column(verticalArrangement = Arrangement.Center) {
                Row(horizontalArrangement = Arrangement.Center) {
                    /*ElevatedButton(onClick = {
                        Log.d("ARTIFACTS", "Clicked Log In")
                        authVm.getAuth(baseUrl, code, codeVerifier, redirectUri, clientId, grantType)
                        //authorized = true
                        *//*if(authVm.authToken != ""){
                            Log.d("ARTIFACTS", "Marking authorized")
                            authorized = true
                        }*//*
                    }) {
                        Text(text = "Log In")
                    }*/
                    Text(text = "Logging In...")
                }
            }
        }
    }

}

/*
{"links":{"next":null,"previous":null},"count":9,"count_pages":1,"current_page":1,
"results":[{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":10,
"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening",
"is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1",
"initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND",
"label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},
"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],
"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,
"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},
{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":9,
"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening",
"is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},
"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame",
"label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},
"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],
"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,
"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},
{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":8,
"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},
"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame",
"label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},
"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,
"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,
"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":7,
"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},
"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame",
"label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},
"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":6,"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":5,"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":4,
"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":3,"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]},{"is_able_to_add_task_notes":true,"is_able_to_edit_task":false,"id":1,"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame","icon_text":"ND","group":2},"status":{"key":"awaiting","value":"Awaiting"},"dosage_type":null,"task_notes":[],"name":"Sample 1","weight":null,"physical_form":null,"expected_main_substances":null,"expected_other_substances":null,"submitter_code":null,"sample_attachments":[]}]}
2023-08-24 11:44:55.358  4946-5563  okhttp.OkHttpClient     edu.nd.crc.paperanalyticaldevices    I  <-- END HTTP (5729-byte body)
 */

/* Response
{"id":1,"test_type":{"id":4,"name":"idPAD","group":{"id":1,"name":"Screening","is_accurate":false,"is_visual":false},"is_other":false,"methods":[]},
"available_result":null,"result":{"key":"not_completed","value":"Not completed"},"result_name":null,"status":{"key":"completed","value":"Completed"},
"sample":"23USND-1","initial_laboratory":{"id":2,"name":"University of Notre Dame","label":"USND","label_tooltip":"University of Notre Dame",
"icon_text":"ND","icon_text_color":"#3898F3","icon_background_color":"#E5F3FE","group":{"id":2,"name":"Central Lab.","sample_creator":true,
"sample_splitter":true,"sample_retest_performer":false,"sample_retest_informer":false,"manage_test_reports":true,"manage_document_storage":true,
"manage_dashboard_analytics":true,"receiver_of_test_requests":false,"sender_of_test_requests":false,"receiver_of_purchase_requests":false,
"sender_of_purchase_requests":false,"is_able_to_view_results":true}},"dosage_type":null,"started_at":"2023-08-24T15:46:18Z",
"finished_at":"2023-08-24T15:46:18Z","step":4,"attachments":[{"id":2,"attachment_type":{"key":"file","value":"File"},"object_id":1,
"attachment_section":{"key":"test_report","value":"Test report"},"name":"rectified.png",
"link":"https://pad-artifactsofresearch-attachments.s3.amazonaws.com/screener/media/rectified.png?response-content-disposition=attachment%3B%20filename%3Drectified.png&AWSAccessKeyId=AKIAQCN7TTB64MMPC7XM&Signature=fjHqODdWIoA7onp7hmuF1%2F%2BJBqs%3D&Expires=1692892594","thumbnail_link":null,"size":"1.26 MB","instead":null,"saved":true},{"id":3,"attachment_type":{"key":"file","value":"File"},"object_id":1,"attachment_section":{"key":"test_report","value":"Test report"},"name":"original.png","link":null,"thumbnail_link":null,"size":"1.10 MB","instead":null,"saved":true}],"task_notes":[{"id":1,"user":{"id":316,"first_name":"Universityofnotredame","last_name":"Assistant"},"user_role":"laboratory_assistant","notes":"PAD ID: 16755\r\nidPAD_small_lite\r\nHeroin (0.51),\r\n(PLS 17%)","read_only":false}],"can_be_canceled":false,"is_able_to_add_task_notes":true,"is_able_to_edit_task":true,"sample_attachments":[],"available_section_settings":[{"id":13,"section":{"id":10,"name":"substances_recognized"},"title":"Substances recognized","ordering_key":1},{"id":14,"section":{"id":2,"name":"test_report"},"title":"Test report","ordering_key":2}],"sample_notes":"","name":"Sample 1","submitter_code":null,"physical_form":null,"weight":null,"expected_main_substances":null,"substance_result":[],
"expected_other_substances":null,"test_notes":"","preparation":"","test_strip_brand":"","test_strip_batch":"","result_is_not_recognized":true,"sample_id":1,"test_type_method":null}
2023-08-24 11:46:34.071  4946-6356  okhttp.OkHttpClient     edu.nd.crc.paperanalyticaldevices    I  <-- END HTTP (2674-byte body)
 */
@Composable
fun ScreenerTaskListItem(modifier: Modifier = Modifier, task: ArtifactsTaskDisplayModel,
                        onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                        testPressed: (ArtifactsTaskDisplayModel) -> Unit){

    var expanded by rememberSaveable { mutableStateOf(false) }

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
                    Text(text = "Name:", style = MaterialTheme.typography.bodySmall)

                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.sampleId)
                    Text(text = task.name, style = MaterialTheme.typography.bodySmall)

                }

                if(task.selected){
                    ElevatedButton(onClick =

                    { testPressed(task) }
                    ) {
                        Text(text = "Test")
                    }
                }
            }
            if(expanded) {
                ScreenerTaskListItemDetail(task = task)
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
fun ScreenerTaskListItemDetail(modifier: Modifier = Modifier, task: ArtifactsTaskDisplayModel){
    Row(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {

            Text(text = "Task ID:")
            Text(text = "Expected Main Substance:")
            Text(text = "Dosage:")
            Text(text = "Expected Other Substances:")
        }
        Column(modifier = Modifier.weight(1f)) {

            Text(text = task.id.toString())
            Text(text = task.expectedMainSubstances)
            Text(text = task.dose)
            Text(text = task.expectedOtherSubstances)
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
        var taskOne = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-17", name = "Acetaminophen",
            drug = "Acetaminophen", manufacturer = "Pfizer", dose = "12.00 mg", doseType = "mg",
            expectedMainSubstances = "Acetaminophen", expectedOtherSubstances = "",
            initialSelectedValue = false)
        ArtifactsTaskListItemDetail(task = taskOne)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 120)
@Composable
fun AtrifactsTaskListItemPreview(){
    CombinedAndroidTheme() {
        var taskOne = ArtifactsTaskDisplayModel(id = 89, sampleId = "22ETCL-17", name = "Acetaminophen",
            drug = "Acetaminophen", manufacturer = "Pfizer", dose = "12.00 mg",
            doseType = "mg",
            expectedMainSubstances = "Acetaminophen", expectedOtherSubstances = "",
            initialSelectedValue = false)
        ArtifactsTaskListItem(task = taskOne, onItemClicked = {}, testPressed = {})

    }

}

@Composable
fun ArtifactsTaskList(modifier: Modifier = Modifier,
                      drugs: List<ArtifactsTaskDisplayModel>,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit,
                      state: MutableState<TextFieldValue>){

    var filteredTaskList: List<ArtifactsTaskDisplayModel>

    LazyColumn(modifier = modifier.padding(vertical = 4.dp)){

        val searchText = state.value.text

        filteredTaskList = if(searchText.isEmpty()){
            drugs
        }else{
            val resultList = ArrayList<ArtifactsTaskDisplayModel>()
            for(drug in drugs){
                if(drug.sampleId.lowercase().contains(searchText.lowercase())
                    || drug.drug.lowercase().contains(searchText.lowercase())){
                    resultList.add(drug)
                }
            }

            resultList
        }

        items(items = filteredTaskList, key = { it.id }){drug ->
            if(drug.type == "street_drugs") {
                ScreenerTaskListItem(task = drug, onItemClicked = onItemClicked, testPressed = testPressed)

            }else{
                ArtifactsTaskListItem(
                    task = drug,
                    onItemClicked = onItemClicked,
                    testPressed = testPressed
                )
            }

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
fun ArtifactsTaskView(modifier: Modifier = Modifier, vm: PadsTaskViewModel,
                      token: String, baseUrl: String,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit){

    val textState = remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit, block = {
        vm.getTasksList(token = token, baseUrl = baseUrl, page = 1)
    })

    Surface() {
        Column {
            /*Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text(text = "ARTIFACTS")
            }*/
            Divider()
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text(text = "Tasks")
            }
            /*BasicTextField(modifier = Modifier.fillMaxWidth(),
                value = "Search", onValueChange = {})*/
            TaskSearchView(state = textState)
            if(vm.errorMessage.isEmpty()){
                //vm.getResultsAsObjects()
                ArtifactsTaskList(modifier = Modifier.weight(1f),
                    drugs = vm.taskList, onItemClicked = onItemClicked, testPressed = testPressed, state = textState)
            }else{
                Text(text = vm.errorMessage)
            }
            Row(modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(1f), horizontalArrangement = Arrangement.Center) {

                if(vm.previous != 0){
                    IconButton(onClick = { vm.getPreviousPage(token = token, baseUrl = baseUrl) }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous page")
                    }
                }
                ElevatedButton(
                    onClick = {
                        vm.getTasksList(token = token, baseUrl = baseUrl, page = 1)

                    }) {
                    Text(text = "REFRESH")
                }
                if(vm.next != 0){
                    IconButton(onClick = { vm.getNextPage(token = token, baseUrl = baseUrl) }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next page")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun ArtifactswTaskViewPreview(){
    val vm = ArtifactsTasksViewModel()
    vm.next = 2
    vm.previous = 1
    CombinedAndroidTheme() {
        ArtifactsTaskView(vm = vm, token = "", baseUrl = "", onItemClicked = {}, testPressed = {})
    }
}


/*@Composable
fun ArtifactsResultView(modifier: Modifier = Modifier, vm: ArtifactsTasksViewModel){
    val task = vm.getSelected()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column() {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = task!!.sampleId)
            }
        }
    }
}*/

@Composable
fun NetworksList(modifier: Modifier = Modifier,
                 networks: List<NetworksDisplayModel>,
                 taskVm: PadsTaskViewModel,
                 netVm: NetworksViewModel,
                 onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel, NetworksDisplayModel) -> Unit){
    Surface() {
        LazyColumn(modifier = Modifier.padding(vertical = 4.dp)){
            items(items = networks){ item ->
                NetworksListItem(network = item, taskVm = taskVm, netVm = netVm, onItemClicked = onItemClicked)
            }
        }
    }
}

/// List the Neural Nets for selection after task confirmation
@Composable
fun NetworkListView(modifier: Modifier = Modifier,
                    networkViewModel: NetworksViewModel,
                    taskViewModel: PadsTaskViewModel,
                    dbHelper: ProjectsDbHelper,
                    tenantType: String,
                    onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel, NetworksDisplayModel) -> Unit){
    LaunchedEffect(Unit, block = {
        if(tenantType == "street_drugs"){
            networkViewModel.getIdPadsNetworks(dbHelper = dbHelper)
        }else{
            networkViewModel.getNetworks(taskViewModel.getSelected()!!, dbHelper = dbHelper)
        }
    })

    val context = LocalContext.current

    Surface(modifier = Modifier.padding(8.dp)){
        Column() {
            if(networkViewModel.networkList.isEmpty()){
                Text("No Compatible Neural Nets Found", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Row(modifier = Modifier.fillMaxWidth() , horizontalArrangement = Arrangement.Center) {
                    ElevatedButton(onClick = {
                        taskViewModel.clearConfirmation()
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Text("Go to Settings")
                    }
                }
            }else{
                Text(text = "Select Neural Network", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                NetworksList(networks = networkViewModel.networkList, taskVm = taskViewModel, netVm = networkViewModel, onItemClicked = onItemClicked)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ElevatedButton(onClick = { taskViewModel.clearConfirmation() }) {
                        Text("Cancel")
                    }
                }
            }

        }
    }
}

@Composable
fun NetworksListItem(modifier: Modifier = Modifier,
                     network: NetworksDisplayModel,
                     taskVm: PadsTaskViewModel,
                     netVm: NetworksViewModel,
                     onItemClicked: (ArtifactsTaskDisplayModel, NetworksDisplayModel, NetworksDisplayModel) -> Unit){
    Surface(modifier = Modifier
        .padding(4.dp)
        .clickable(true,
            onClick = {
                onItemClicked(
                    taskVm.getSelected()!!,
                    network,
                    netVm.getConcNetwork(taskVm.getSelected()!!)
                )
                taskVm.clearConfirmation()
            }),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsTopBar(){
    TopAppBar(
        title = {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text("ARTIFACTS Verify", fontSize = 18.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.surface)
    )


}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenerTopBar(){
    TopAppBar(
        title = {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                Text("ARTIFACTS Screenr", fontSize = 18.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.surface)
    )
}

@Preview(showBackground = true)
@Composable
fun ArtifactsTopBarPreview(){
    CombinedAndroidTheme {
        ArtifactsTopBar()
    }
}

@Composable
fun TaskSearchView(state: MutableState<TextFieldValue>){
    TextField(value = state.value,
    onValueChange = {value -> state.value = value},
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null)
    },
    trailingIcon = {
        if (state.value != TextFieldValue("")) {
            IconButton(
                onClick = {
                    state.value =
                        TextFieldValue("")
                // Remove text from TextField when you press the 'X' icon
                }
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "",
                    modifier = Modifier
                        .padding(15.dp)
                        .size(24.dp)
                )
            }
        }
    },

    )
}
@Preview(showBackground = true)
@Composable
fun TaskSearchPreview(){
    val textState = remember { mutableStateOf(TextFieldValue(""))}
    CombinedAndroidTheme() {

        TaskSearchView(state = textState)
    }
}


@Composable
fun ScreenerMainView(modifier: Modifier = Modifier,
                     baseUrl: String,
                     clientId: String,
                     redirectUri: String,
                     code: String,
                     codeVerifier: String,
                     grantType: String,
                     authVm: ArtifactsAuthViewModel,
                     taskVm: ScreenerTaskViewModel,
                     networksVm: NetworksViewModel,
                     dbHelper: ProjectsDbHelper,
                     onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                     testPressed: (ArtifactsTaskDisplayModel) -> Unit,
                     onNetworkPressed: (ArtifactsTaskDisplayModel, NetworksDisplayModel, NetworksDisplayModel) -> Unit){

    Scaffold(
        topBar = { ScreenerTopBar() },
        content = {
                paddingValues -> Column(modifier = Modifier.padding(paddingValues)){

                }
        }
    )
}
@Composable
fun ArtifactsMainView(modifier: Modifier = Modifier,
                      baseUrl: String,
                      clientId: String,
                      redirectUri: String,
                      code: String,
                      codeVerifier: String,
                      grantType: String,
                      tenantType: String,
                      authVm: ArtifactsAuthViewModel,
                      taskVm: PadsTaskViewModel,
                      networksVm: NetworksViewModel,
                      dbHelper: ProjectsDbHelper,
                      onItemClicked: (ArtifactsTaskDisplayModel) -> Unit,
                      testPressed: (ArtifactsTaskDisplayModel) -> Unit,
                      onNetworkPressed: (ArtifactsTaskDisplayModel, NetworksDisplayModel, NetworksDisplayModel) -> Unit){

    Scaffold(
        topBar = { if(tenantType == "street_drugs") ScreenerTopBar()
                 else ArtifactsTopBar()},
        content = {
            paddingValues -> Column(modifier = Modifier.padding(paddingValues)){
                if(taskVm.taskConfirmed){
                    NetworkListView(
                        networkViewModel = networksVm,
                        taskViewModel = taskVm,
                        dbHelper = dbHelper,
                        tenantType = tenantType,
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
                        tenantType = tenantType,
                        authVm = authVm,
                        taskVm = taskVm,
                        onItemClicked = onItemClicked,
                        testPressed = testPressed
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(top = 18.dp, bottom = 36.dp)
        )//Scaffold
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

@Composable
fun ResultFieldsView(modifier: Modifier = Modifier,
                     imageUri: Uri,
                     stateDrug: String,
                     predictedDrug: String,
                     predictedConc: String,
                     safe: String,
                    vm: ArtifactsResultViewModel){
    Surface(modifier = Modifier.padding(4.dp),
        color = MaterialTheme.colorScheme.primary){
        Column(modifier = Modifier.padding(4.dp)) {

            Image(painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(data = imageUri).build(),
                contentScale = ContentScale.Fit), contentDescription = "Result Image")
            Surface(modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.background) {
                Column() {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text(text = "Stated Drug:", style = MaterialTheme.typography.bodySmall)
                    }
                    Row() {
                        Text(text = stateDrug)
                    }
                }
            }
            Surface(modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.background) {
                Column() {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text(text = "Predicted Conc%:", style = MaterialTheme.typography.bodySmall)
                    }
                    Row() {
                        Text(text = predictedConc)
                    }
                }
            }
            Surface(modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.background) {
                Column() {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text(text = "Predicted Drug:", style = MaterialTheme.typography.bodySmall)
                    }
                    Row() {
                        Text(text = predictedDrug)
                    }
                }
            }
            Surface(modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.background) {
                Column() {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text(text = "Suspected Unsafe:", style = MaterialTheme.typography.bodySmall)
                    }
                    Row() {
                        Text(text = safe)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                ElevatedButton(modifier = Modifier.weight(1f), onClick = { /*TODO*/ }) {
                    Row() {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard")
                        Text(text = "Discard")
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                ElevatedButton(modifier = Modifier.weight(1f), onClick = { /*TODO*/ }) {
                    Row() {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                        Text(text = "Upload")
                    }
                }
            }
        }
    }
}

/*@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun ResultFieldsViewPreview(){
    CombinedAndroidTheme() {
        ResultFieldsView(image = , stateDrug = "Amoxicilin", predictedDrug = "Albendazole", predictedConc = "100%", safe = "Suspected unsafe")
    }
}*/
