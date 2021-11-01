package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
//import androidx.work.WorkQuery;

import android.os.Bundle;
//import android.util.Log;
import android.widget.TextView;

//import com.google.common.util.concurrent.ListenableFuture;

//import java.util.Arrays;
import java.util.List;
//import java.util.concurrent.ExecutionException;

public class UploadQueueActivity extends AppCompatActivity {

    //List<WorkInfo.State> stateList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_queue);

        WorkManager manager = WorkManager.getInstance(this);


        TextView queueText = findViewById(R.id.queueTextView);

        LiveData<List<WorkInfo>> workInfos = manager.getWorkInfosByTagLiveData("result_upload");

        workInfos.observe(this, listOfWorkInfo -> {
            if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
                return;
            }


            int countInQueue = 0;

            for(WorkInfo workInfo : listOfWorkInfo){
                Data workData = workInfo.getOutputData();
/*
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

                String workInfoString = workInfo.toString();
                if(workInfoString == null){
                    Log.d("Queue TAG", "Null Info String.");
                }else {
                    Log.d("Queue TAG", workInfoString);
                }*/

                //int countInQueue = listOfWorkInfo.size();

                //There's no viewable data in ENQUEUED work, so for now just count them
                //@TODO Maybe store some data as they go into the worker so it can be referenced later by id?

                if(!workInfo.getState().isFinished()){
                    countInQueue++;
                }
                String queueStatus = "Queue Size: " + countInQueue;
                queueText.setText(queueStatus);
            }

        });


    }
}