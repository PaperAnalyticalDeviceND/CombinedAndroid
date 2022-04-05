package edu.nd.crc.paperanalyticaldevices;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.jetbrains.annotations.NotNull;

public class ProjectsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Projects.db";

    private static final String SQL_CREATE_PROJECTS = "CREATE TABLE " + ProjectContract.ProjectEntry.TABLE_NAME +
            " (" + ProjectContract.ProjectEntry._ID + " INTEGER PRIMARY KEY, " + ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTID + " TEXT, " +
            ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME + " TEXT)";

    private static final String SQL_CREATE_NETWORKS = "CREATE TABLE " + NetworksContract.NetworksEntry.TABLE_NAME +
            " (" + NetworksContract.NetworksEntry._ID + " INTEGER PRIMARY KEY, " + NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKID + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL + " Text, " + NetworksContract.NetworksEntry.COLUMN_NAME_FILENAME + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_DESCRIPTION + " TEXT, " + NetworksContract.NetworksEntry.COLUMN_NAME_VERSIONSTRING + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_TYPE + " TEXT, " + NetworksContract.NetworksEntry.COLUMN_NAME_DRUGS + " TEXT, " +
            NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME + " TEXT)";

    private static final String SQL_CREATE_DRUGS = "CREATE TABLE " + DrugsContract.DrugsEntry.TABLE_NAME +
            " (" + DrugsContract.DrugsEntry._ID + " INTEGER PRIMARY KEY, " + DrugsContract.DrugsEntry.COLUMN_NAME_NETWORK + " TEXT, " +
            DrugsContract.DrugsEntry.COLUMN_NAME_DRUGID + " Text, " + DrugsContract.DrugsEntry.COLUMN_NAME_DRUGNAME + " TEXT)";

    private static final String SQL_DELETE_PROJECTS =
            "DROP TABLE IF EXISTS " + ProjectContract.ProjectEntry.TABLE_NAME;

    private static final String SQL_DELETE_PROJECT_ROWS = "DELETE FROM " + ProjectContract.ProjectEntry.TABLE_NAME;

    private static final String SQL_DELETE_NETWORK_ROWS = "DELETE FROM " + NetworksContract.NetworksEntry.TABLE_NAME;

    private static final String SQL_DELETE_NETWORKS = "DROP TABLE IF EXISTS " + NetworksContract.NetworksEntry.TABLE_NAME;

    private static final String SQL_DELETE_DRUGS = "DROP TABLE IF EXISTS " + DrugsContract.DrugsEntry.TABLE_NAME;

    private static final String SQL_DELETE_DRUGS_ROWS = "DELETE FROM " + DrugsContract.DrugsEntry.TABLE_NAME;

    public ProjectsDbHelper(@NotNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    public void clearProjects(@NotNull SQLiteDatabase db){

        db.execSQL(SQL_DELETE_PROJECT_ROWS);
    }

    public void clearNetworks(@NotNull SQLiteDatabase db){

        db.execSQL(SQL_DELETE_NETWORK_ROWS);
    }

    public void clearDrugs(@NotNull SQLiteDatabase db){

        db.execSQL(SQL_DELETE_DRUGS_ROWS);
    }

    @Override
    public void onCreate(@NotNull SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_PROJECTS);
        db.execSQL(SQL_CREATE_NETWORKS);
        db.execSQL(SQL_CREATE_DRUGS);
    }

    @Override
    public void onUpgrade(@NotNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_PROJECTS);
        db.execSQL(SQL_DELETE_NETWORKS);
        db.execSQL(SQL_DELETE_DRUGS);
        onCreate(db);

    }

}
