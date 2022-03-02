package edu.nd.crc.paperanalyticaldevices;

import android.provider.BaseColumns;

public class DrugsContract {

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + DrugsContract.DrugsEntry.TABLE_NAME +
            " (" + DrugsContract.DrugsEntry._ID + " INTEGER PRIMARY KEY, " + DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK + " TEXT, " +
            DrugsEntry.COLUMN_NAME_DRUGID + " Text, " + DrugsEntry.COLUMN_NAME_DRUGNAME + " TEXT)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + NetworksContract.NetworksEntry.TABLE_NAME;

    private DrugsContract(){

    }

    public static class DrugsEntry implements BaseColumns {
        public static final String TABLE_NAME = "drugs";
        public static final String COLUMN_NAME_DRUGID = "drugid";
        public static final String COLUMN_NAME_NETWORK = "network";

        public static final String COLUMN_NAME_DRUGNAME = "drugname";
    }

}
