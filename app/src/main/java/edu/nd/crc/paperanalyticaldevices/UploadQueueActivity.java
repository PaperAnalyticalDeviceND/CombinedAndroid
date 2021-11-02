package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
//import androidx.work.WorkQuery;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

//import com.google.common.util.concurrent.ListenableFuture;

//import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
//import java.util.concurrent.ExecutionException;

public class UploadQueueActivity extends AppCompatActivity {

    //List<WorkInfo.State> stateList;

    ListView queueListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_queue);

        WorkManager manager = WorkManager.getInstance(this);


        //TextView queueText = findViewById(R.id.queueTextView);
        queueListView = findViewById(R.id.queue_list);

        LiveData<List<WorkInfo>> workInfos = manager.getWorkInfosByTagLiveData("result_upload");

        workInfos.observe(this, listOfWorkInfo -> {
            if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
                return;
            }


            int countInQueue = 0;
            ArrayList<String> workList = new ArrayList<String>();


            for(WorkInfo workInfo : listOfWorkInfo){
                Data workData = workInfo.getOutputData();

                UUID workId = workInfo.getId();
                Log.d("Queue TAG", "Work ID: " + workId);


                //There's no viewable data in ENQUEUED work, so for now just count them
                // Maybe store some data as they go into the worker so it can be referenced later by id?

                /*
                if(!workInfo.getState().isFinished()){
                    countInQueue++;
                }
                String queueStatus = "Queue Size: " + countInQueue;
                queueText.setText(queueStatus);*/

                if(!workInfo.getState().isFinished()) {
                    WorkInfoDbHelper dbHelper = new WorkInfoDbHelper(getBaseContext());
                    SQLiteDatabase db = dbHelper.getReadableDatabase();

                    String[] projection = {
                            BaseColumns._ID,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP,

                    };

                    String selection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String[] selectionArgs = {workId.toString()};
                    String sortOrder = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " ASC";

                    Cursor cursor = db.query(WorkInfoContract.WorkInfoEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                    String workMessage = "";
                    List items = new ArrayList<>();
                    while(cursor.moveToNext()){
                        String drugName = cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME));
                        String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP));

                        Timestamp javaTimestamp = new Timestamp(Long.parseLong(timestamp));
                        Date date = new Date(javaTimestamp.getTime());

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String newDate = sdf.format(date);
                        workMessage = newDate  + " - " + drugName;
                    }

                    workList.add(workMessage);
                    //queueText.setText(workMessage);

                }
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.queue_listview, R.id.queue_item, workList);
            queueListView.setAdapter(arrayAdapter);

        });


    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view){
        finish();
    }

}