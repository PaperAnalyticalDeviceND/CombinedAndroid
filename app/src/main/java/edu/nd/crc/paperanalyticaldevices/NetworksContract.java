package edu.nd.crc.paperanalyticaldevices;

import android.provider.BaseColumns;

public final class NetworksContract {

    //class defining an SQLite database table to store the project names

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + NetworksContract.NetworksEntry.TABLE_NAME +
            " (" + NetworksContract.NetworksEntry._ID + " INTEGER PRIMARY KEY, " + NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKID + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL + " Text, " + NetworksContract.NetworksEntry.COLUMN_NAME_FILENAME + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_DESCRIPTION + " TEXT, " + NetworksContract.NetworksEntry.COLUMN_NAME_VERSIONSTRING + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_TYPE + " TEXT, " + NetworksContract.NetworksEntry.COLUMN_NAME_DRUGS + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME + " TEXT)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + NetworksContract.NetworksEntry.TABLE_NAME;


    private NetworksContract() {
    }

    public static class NetworksEntry implements BaseColumns {

        public static final String TABLE_NAME = "networks";
        public static final String COLUMN_NAME_NETWORKID = "networkid";
        public static final String COLUMN_NAME_NETWORKNAME = "networkname";

        public static final String COLUMN_NAME_WEIGHTSURL = "weightsurl";
        public static final String COLUMN_NAME_FILENAME = "filename";
        public static final String COLUMN_NAME_VERSIONSTRING = "versionstring";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
        public static final String COLUMN_NAME_DRUGS = "drugs";
        public static final String COLUMN_NAME_TYPE = "type";
    }


}
