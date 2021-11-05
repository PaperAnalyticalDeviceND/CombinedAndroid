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
    //Activity to display pending uploads in a ListView
    //details are stored in an SQLite DB, keyed by the WorkID, so we get the WorkIDs of non-finished work and query the DB to get drug name, sample id, timestamp


    ListView queueListView;

    WorkInfoDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_queue);

        WorkManager manager = WorkManager.getInstance(this);

        //ListView that holds the PAD data in queue
        queueListView = findViewById(R.id.queue_list);

        //get all the work that's tagged 'result_upload'
        LiveData<List<WorkInfo>> workInfos = manager.getWorkInfosByTagLiveData("result_upload");

        //observe the live data connection of the work items
        workInfos.observe(this, listOfWorkInfo -> {
            if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
                return;
            }

            //set up the SQLite DB connection
            dbHelper = new WorkInfoDbHelper(getBaseContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();


            int countInQueue = 0;
            //Create a list of objects to display in the ListView for the queue
            ArrayList<PADDataObject> workList = new ArrayList<PADDataObject>();


            for(WorkInfo workInfo : listOfWorkInfo){
                Data workData = workInfo.getOutputData();

                UUID workId = workInfo.getId();
                Log.d("Queue TAG", "Work ID: " + workId);

                /*
                if(!workInfo.getState().isFinished()){
                    countInQueue++;
                }
                String queueStatus = "Queue Size: " + countInQueue;
                queueText.setText(queueStatus);*/

                if(!workInfo.getState().isFinished()) {


                    String[] projection = {
                            BaseColumns._ID,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP,

                    };

                    String selection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String[] selectionArgs = {workId.toString()};
                    String sortOrder = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " ASC";

                    Cursor cursor = db.query(WorkInfoContract.WorkInfoEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                    //String workMessage = "";
                    String newDate = "";
                    String drugName = "";
                    String padId = "";

                    List items = new ArrayList<>();
                    while(cursor.moveToNext()){
                        drugName = cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME));
                        //String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP));
                        Long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP));

                        padId = "PAD ID: " + cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID));

                        //Timestamp javaTimestamp = new Timestamp(Long.parseLong(timestamp)); // if the timestamp was in a TEXT column
                        Timestamp javaTimestamp = new Timestamp(timestamp);
                        Date date = new Date(javaTimestamp.getTime());

                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                        newDate = sdf.format(date);
                        //workMessage = newDate  + " - " + drugName;
                    }

                    //workList.add(workMessage);
                    //queueText.setText(workMessage);

                    PADDataObject padInfo = new PADDataObject();
                    padInfo.setDatetime(newDate);
                    padInfo.setPadId(padId);
                    padInfo.setDrugName(drugName);

                    workList.add(padInfo);

                }else{
                    //Delete finished records from the SQLite db

                    String deleteSelection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String[] deleteSelectionArgs = { workId.toString() };
                    int deletedRows = db.delete(WorkInfoContract.WorkInfoEntry.TABLE_NAME, deleteSelection, deleteSelectionArgs);
                    Log.d("Queue TAG", "Rows deleted from WorkInfo Table: " + deletedRows);
                }
            }

            //ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.queue_listview, R.id.queue_item, workList);
            //queueListView.setAdapter(arrayAdapter);

            queueListView.setAdapter(new QueueListBaseAdapter(this, workList));

        });


    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view){
        finish();
    }

    @Override
    public void onDestroy(){
        dbHelper.close();
        super.onDestroy();
    }

}