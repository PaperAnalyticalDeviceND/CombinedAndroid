package edu.nd.crc.paperanalyticaldevices;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public final class UploadQueueActivity extends AppCompatActivity {
    //Activity to display pending uploads in a ListView
    //details are stored in an SQLite DB, keyed by the WorkID, so we get the WorkIDs of non-finished work and query the DB to get drug name, sample id, timestamp


    ListView queueListView;
    ListView doneListView;

    WorkInfoDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_queue);



        WorkManager manager = WorkManager.getInstance(this);

        //ListView that holds the PAD data in queue
        queueListView = findViewById(R.id.queue_list);
        doneListView = findViewById(R.id.finishedListView);

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

            //Create a list of objects to display in the ListView for the queue
            ArrayList<PADDataObject> workList = new ArrayList<>();
            ArrayList<PADDataObject> doneList = new ArrayList<>();
            String[] projection = {
                    BaseColumns._ID,
                    WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME,
                    WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID,
                    WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY,
                    WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP,

            };

            for (WorkInfo workInfo : listOfWorkInfo) {
                UUID workId = workInfo.getId();
                Log.d("Queue TAG", "Work ID: " + workId);

                if (!workInfo.getState().isFinished()) {
  /*
                    String[] projection = {
                            BaseColumns._ID,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY,
                            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP,

                    };
*/
                    String selection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String[] selectionArgs = {workId.toString()};
                    String sortOrder = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " ASC";

                    try( Cursor cursor = db.query(WorkInfoContract.WorkInfoEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)) {
                        PADDataObject padInfo = new PADDataObject();
                        while (cursor.moveToNext()) {
                            String drugName = cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME));
                            padInfo.setDrugName(drugName);

                            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP));

                            String padId = "PAD ID: " + cursor.getString(cursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID));
                            padInfo.setPadId(padId);

                            Timestamp javaTimestamp = new Timestamp(timestamp);
                            Date date = new Date(javaTimestamp.getTime());

                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                            String newDate = sdf.format(date);
                            padInfo.setDatetime(newDate);
                        }
                        workList.add(padInfo);
                    }
                } else {
                    //Delete finished records from the SQLite db
                    //where they are older than one week
                    long oneWeekAgo = Calendar.getInstance().getTimeInMillis() - 604800;

                    //String deleteSelection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String deleteSelection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP + " < ?";
                    //String[] deleteSelectionArgs = {workId.toString()};
                    String[] deleteSelectionArgs = {String.valueOf(oneWeekAgo)};
                    int deletedRows = db.delete(WorkInfoContract.WorkInfoEntry.TABLE_NAME, deleteSelection, deleteSelectionArgs);
                    Log.d("Queue TAG", "Rows deleted from WorkInfo Table: " + deletedRows);

                    //change newer finished ones to WorkId = Finished
                    ContentValues cv = new ContentValues();
                    cv.put(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID, "Finished");
                    String selection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
                    String[] selectionArgs = {workId.toString()};
                    db.update(WorkInfoContract.WorkInfoEntry.TABLE_NAME, cv, selection, selectionArgs);
                }
            }

            queueListView.setAdapter(new QueueListBaseAdapter(this, workList));

            String finSelection = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " = ?";
            String[] finSelectionArgs = {"Finished"};
            String finSortOrder = WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP + " DESC";
            try(Cursor finCursor = db.query(WorkInfoContract.WorkInfoEntry.TABLE_NAME, projection, finSelection, finSelectionArgs, null, null, finSortOrder) ){

                while(finCursor.moveToNext()){
                    PADDataObject doneInfo = new PADDataObject();
                    String doneDrugName = finCursor.getString(finCursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME));
                    long doneTimestamp = finCursor.getLong(finCursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP));
                    String donePadId = "PAD ID: " + finCursor.getString(finCursor.getColumnIndexOrThrow(WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID));
                    Timestamp javaTimestamp = new Timestamp(doneTimestamp);
                    Date date = new Date(javaTimestamp.getTime());

                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    String newDate = sdf.format(date);
                    doneInfo.setDatetime(newDate);
                    doneInfo.setDrugName(doneDrugName);
                    doneInfo.setPadId(donePadId);

                    doneList.add(doneInfo);
                }
            }

            doneListView.setAdapter(new FinishedListBaseAdapter(this, doneList));

        });


    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view) {
        finish();
    }

    @Override
    public void onDestroy() {
        if (null != dbHelper) {
            dbHelper.close();
        }
        super.onDestroy();
    }

}