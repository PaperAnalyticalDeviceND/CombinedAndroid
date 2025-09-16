package edu.nd.crc.paperanalyticaldevices;

import android.provider.BaseColumns;

public class ProjectDrugsContract {
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + ProjectDrugsContract.ProjectDrugsEntry.TABLE_NAME +
            " (" + ProjectDrugsContract.ProjectDrugsEntry._ID + " INTEGER PRIMARY KEY, " + ProjectDrugsContract.ProjectDrugsEntry.COLUMN_NAME_PROJECT + " TEXT, " +
            ProjectDrugsContract.ProjectDrugsEntry.COLUMN_NAME_PROJECTID + " Text, " + ProjectDrugsContract.ProjectDrugsEntry.COLUMN_NAME_DRUGNAME + " TEXT)";


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ProjectDrugsContract.ProjectDrugsEntry.TABLE_NAME;

    private ProjectDrugsContract(){

    }

    public static class ProjectDrugsEntry implements BaseColumns {
        public static final String TABLE_NAME = "project_drugs";
        public static final String COLUMN_NAME_PROJECTID = "projectid";
        public static final String COLUMN_NAME_PROJECT = "project";

        public static final String COLUMN_NAME_DRUGNAME = "drugname";
    }
}
