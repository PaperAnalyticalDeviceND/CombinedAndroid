package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar myToolbar = findViewById(R.id.abouttoolbar);
        setSupportActionBar(myToolbar);


        String privacyLabel = getResources().getString(R.string.privacy_policy_label);
        SpannableString privacyText = new SpannableString(privacyLabel);
        privacyText.setSpan(new URLSpan("https://www.privacypolicies.com/live/452ed6eb-223c-4d0d-b6e5-1e4eea18ecf6"), 0, privacyLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView privacyView = findViewById(R.id.privacy_link);
        privacyView.setText(privacyText);

        privacyView.setMovementMethod(LinkMovementMethod.getInstance());

        String homeLabel = getResources().getString(R.string.homepage_label);
        SpannableString homeText = new SpannableString(homeLabel);
        homeText.setSpan(new URLSpan("https://padproject.nd.edu/"), 0, homeLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView homepageView = findViewById(R.id.homepage_link);

        homepageView.setText(homeText);

        homepageView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView versionView = findViewById(R.id.version_textview);

        String versionString = "PADReader Version: " + BuildConfig.VERSION_NAME;
        versionView.setText(versionString);
    }

    public void checkForUpdates(View view){
        Toast.makeText(AboutActivity.this, "Downloading Projects", Toast.LENGTH_SHORT).show();
        String[] nets;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // these will be the exact names coming from the "networks" API
        String neuralNetName = prefs.getString("neuralNet", "");
        String secondaryNeuralNetName = prefs.getString("secondarynet", "");

        nets = new String[]{neuralNetName, secondaryNeuralNetName};

        Constraints constraints = new Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        WorkRequest myUpdateWork = new OneTimeWorkRequest.Builder(UpdatesWorker.class).setConstraints(constraints)
                .addTag("neuralnet_updates").setInputData(new Data.Builder()
                        .putStringArray("projectkeys", nets)
                        .build()
                )
                .build();

        WorkManager.getInstance(this).enqueue(myUpdateWork);
    }

    public void finish(View view) {
        finish();
    }
}