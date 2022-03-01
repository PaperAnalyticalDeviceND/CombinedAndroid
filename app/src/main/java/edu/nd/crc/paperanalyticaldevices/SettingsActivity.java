package edu.nd.crc.paperanalyticaldevices;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

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

        progressBar = findViewById(R.id.simpleProgressBar);
        progressBar.setVisibility(View.INVISIBLE);

        doneButton = findViewById(R.id.button6);
    }

    /*
    Set as onClick for the Done button so that this activity can be reached from Main and Result activities and always return to the calling activity on close
     */
    public void finish(View view) {
        finish();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}