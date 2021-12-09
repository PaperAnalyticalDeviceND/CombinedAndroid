package edu.nd.crc.paperanalyticaldevices;

import android.provider.BaseColumns;

public final class WorkInfoContract {

    //class defining an SQLite database table to store the PAD details when added to the upload work queue

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + WorkInfoEntry.TABLE_NAME +
            " (" + WorkInfoEntry._ID + " INTEGER PRIMARY KEY, " + WorkInfoEntry.COLUMN_NAME_WORKID + " TEXT, " +
            WorkInfoEntry.COLUMN_NAME_SAMPLEID + " TEXT, " + WorkInfoEntry.COLUMN_NAME_SAMPLENAME + " TEXT, " +
            WorkInfoEntry.COLUMN_NAME_QUANTITY + " TEXT, " + WorkInfoEntry.COLUMN_NAME_NOTES + " TEXT, " +
            WorkInfoEntry.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WorkInfoEntry.TABLE_NAME;


    private WorkInfoContract(){}

    public static class WorkInfoEntry implements BaseColumns {

        public static final String TABLE_NAME = "workinfo";
        public static final String COLUMN_NAME_WORKID = "workid";
        public static final String COLUMN_NAME_SAMPLENAME = "samplename";
        public static final String COLUMN_NAME_SAMPLEID = "sampleid";
        public static final String COLUMN_NAME_TIMESTAMP = "sampletimestamp";
        public static final String COLUMN_NAME_NOTES = "samplenotes";
        public static final String COLUMN_NAME_QUANTITY = "samplequantity";

    }
}
