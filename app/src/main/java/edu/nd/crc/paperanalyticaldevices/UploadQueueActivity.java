package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import android.os.Bundle;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UploadQueueActivity extends AppCompatActivity {

    //List<WorkInfo.State> stateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_queue);

        WorkManager manager = WorkManager.getInstance(this);
        //WorkInfo.State state = WorkInfo.State.ENQUEUED;
        //stateList.add(state);
        // =
        //WorkQuery query = new WorkQuery.Builder.addStates(Arrays.asList(WorkInfo.State.ENQUEUED)).build();
        /*
        WorkQuery query = new WorkQuery.Builder.fromStates(Arrays.asList(WorkInfo.State.ENQUEUED, WorkInfo.State.FAILED)).build();


        ListenableFuture<List<WorkInfo>> workInfosListenable = manager.getWorkInfos(query);

        if(workInfosListenable != null){

            //for( work : workInfos){
            //    Data workData = work.get
            //}
            try {
                List<WorkInfo> workInfos = workInfosListenable.get();

                for(WorkInfo work : workInfos){
                    Data workData = work.getOutputData();
                    String tag = workData.getString("SAMPLE_NAME");
                    Log.d("Queue TAG", tag);

                }

            }catch(InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }
*/

        LiveData<List<WorkInfo>> workInfos = manager.getWorkInfosByTagLiveData("result_upload");

        workInfos.observe(this, listOfWorkInfo -> {
            if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
                return;
            }

            for(WorkInfo workInfo : listOfWorkInfo){
                Data workData = workInfo.getOutputData();
                String tag = workData.getString("SAMPLE_NAME");
                if(tag == null){
                    Log.d("Queue TAG", "Null SAMPLE.");
                }else {
                    Log.d("Queue TAG", tag);
                }

                String sampleId = workData.getString("SAMPLE_ID");
                if(sampleId == null){
                    Log.d("Queue TAG", "Null Sample ID.");
                }else{
                    Log.d("Queue TAG", sampleId);
                }

                String workString = workData.toString();
                if(workString == null){
                    Log.d("Queue TAG", "Null String.");
                }else{
                    Log.d("Queue TAG", workString);
                }
            }

        });


    }
}