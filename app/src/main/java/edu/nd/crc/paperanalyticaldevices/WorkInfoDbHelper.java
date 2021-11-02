package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WorkInfoDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WorkInfo.db";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + WorkInfoContract.WorkInfoEntry.TABLE_NAME +
            " (" + WorkInfoContract.WorkInfoEntry._ID + " INTEGER PRIMARY KEY, " + WorkInfoContract.WorkInfoEntry.COLUMN_NAME_WORKID + " TEXT, " +
            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLEID + " TEXT, " + WorkInfoContract.WorkInfoEntry.COLUMN_NAME_SAMPLENAME + " TEXT, " +
            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_QUANTITY + " TEXT, " + WorkInfoContract.WorkInfoEntry.COLUMN_NAME_NOTES + " TEXT, " +
            WorkInfoContract.WorkInfoEntry.COLUMN_NAME_TIMESTAMP + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WorkInfoContract.WorkInfoEntry.TABLE_NAME;

    public WorkInfoDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);

    }
}
