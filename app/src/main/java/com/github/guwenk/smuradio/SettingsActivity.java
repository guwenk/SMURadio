package com.github.guwenk.smuradio;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by student1 on 17.01.17.
 */

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
