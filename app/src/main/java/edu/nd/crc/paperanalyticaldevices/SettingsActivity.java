package edu.nd.crc.paperanalyticaldevices;

import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import android.util.Log;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    public static ProgressBar progressBar;
    public static Button doneButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_constraint_layout);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        //progressBar = findViewById(R.id.simpleProgressBar);
        //progressBar.setVisibility(View.INVISIBLE);

        //doneButton = findViewById(R.id.button6);

        //put in a top toolbar with a button
        Toolbar myToolbar = findViewById(R.id.settingstoolbar);
        setSupportActionBar(myToolbar);


    }


    public void showHelp(View view){

        HelpPopup helpPopup = new HelpPopup();
        helpPopup.showPopupWindow(view);

    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view) {
        finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        ProjectsDbHelper dbHelper;

        SQLiteDatabase db;

        ListPreference projectsList;
        ListPreference networkList;
        ListPreference secondaryNetworksList;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // set the projects and neural nets lists from retrieved API data if present

            projectsList = (ListPreference) findPreference("project");
            networkList = (ListPreference) findPreference("neuralnet");
            secondaryNetworksList = (ListPreference) findPreference("secondary");
            // make an array of all projects and pass to the setEntries method

            dbHelper = new ProjectsDbHelper(getContext());
            db = dbHelper.getReadableDatabase();

            ArrayList<String> projectEntries = new ArrayList<>();
            ArrayList<String> networkEntries = new ArrayList<>();

            String[] projection = {
                    BaseColumns._ID,
                    ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME,
            };

            String sortOrder = ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME + " ASC";

            String project;
            try( Cursor cursor = db.query(ProjectContract.ProjectEntry.TABLE_NAME, projection, null, null, null, null, sortOrder)) {

                while (cursor.moveToNext()) {

                    project = cursor.getString(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_NAME_PROJECTNAME));
                    projectEntries.add(project);
                    //Log.d("DB_HELPER", project);
                }
            }

            String[] projectsArray = projectEntries.toArray(new String[projectEntries.size()]);

            if(projectsArray.length != 0){
                projectsList.setEntries(projectsArray);
                projectsList.setEntryValues(projectsArray);
            }

            String netSelection = "(" + NetworksContract.NetworksEntry.COLUMN_NAME_TYPE + " = 'tf_lite' OR " +
                    NetworksContract.NetworksEntry.COLUMN_NAME_TYPE + " = 'tensorflow') AND " +
                    NetworksContract.NetworksEntry.COLUMN_NAME_WEIGHTSURL + " != ''";
            String netSortOrder = NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME + " ASC";

            String network;

            try( Cursor netCursor = db.query(NetworksContract.NetworksEntry.TABLE_NAME, null, netSelection, null, null, null, netSortOrder)){
                while(netCursor.moveToNext()){
                    network = netCursor.getString(netCursor.getColumnIndexOrThrow(NetworksContract.NetworksEntry.COLUMN_NAME_NETWORKNAME));
                    networkEntries.add(network);
                    Log.d("DB_HELPER", network);
                }
            }

            String[] networksArray = networkEntries.toArray(new String[networkEntries.size()]);

            if(!networkEntries.isEmpty()){
                networkList.setEntries(networksArray);
                networkList.setEntryValues(networksArray);
                secondaryNetworksList.setEntries(networksArray);
                secondaryNetworksList.setEntryValues(networksArray);
            }

        }

        @Override
        public void onDestroy() {
            if (null != dbHelper) {
                dbHelper.close();
            }
            super.onDestroy();
        }
    }
}