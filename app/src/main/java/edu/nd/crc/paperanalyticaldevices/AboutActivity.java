package edu.nd.crc.paperanalyticaldevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

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

    public void finish(View view) {
        finish();
    }
}