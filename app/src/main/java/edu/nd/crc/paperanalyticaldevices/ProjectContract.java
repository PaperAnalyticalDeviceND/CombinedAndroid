package edu.nd.crc.paperanalyticaldevices;

import android.provider.BaseColumns;

public final class ProjectContract {

    //class defining an SQLite database table to store the project names

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + ProjectContract.ProjectEntry.TABLE_NAME +
            " (" + ProjectContract.ProjectEntry._ID + " INTEGER PRIMARY KEY, " + ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTID + " TEXT, " +
             ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME + " TEXT)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ProjectContract.ProjectEntry.TABLE_NAME;


    private ProjectContract() {
    }

    public static class ProjectEntry implements BaseColumns {

        public static final String TABLE_NAME = "projects";
        public static final String COLUMN_NAME_PROJECTID = "projectid";
        public static final String COLUMN_NAME_PROJECTNAME = "projectname";


    }


}
