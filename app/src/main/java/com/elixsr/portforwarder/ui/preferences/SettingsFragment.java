/*
 * Fwd: the port forwarding app
 * Copyright (C) 2016  Elixsr Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.elixsr.portforwarder.ui.preferences;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.elixsr.portforwarder.FwdApplication;
import com.elixsr.portforwarder.R;
import com.elixsr.portforwarder.db.RuleContract;
import com.elixsr.portforwarder.db.RuleDbHelper;
import com.elixsr.portforwarder.forwarding.ForwardingManager;
//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.Tracker;

/**
 * Created by Niall McShane on 29/02/2016.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    private static final String CLEAR_RULES_COMPLETE_MESSAGE = "All rules have been removed";

    private static final String CATEGORY_RULES = "Rules";
    private static final String ACTION_DELETE = "Clear";
    private static final String LABEL_DELETE_RULE = "Delete All Rules";
    public static final String DARK_MODE_BROADCAST = "com.elixsr.DARK_MODE_TOGGLE";
    private static final String CATEGORY_THEME = "Theme";
    private static final String ACTION_CHANGE = "Change";

    private LocalBroadcastManager localBroadcastManager;
    private ForwardingManager forwardingManager;
    private Preference clearRulesButton;
    private Preference versionNamePreference;



    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferencesListener;

    //private Tracker tracker;
    private Preference changeThemeToggle;
    Toast toast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        forwardingManager = ForwardingManager.getInstance();
        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity().getBaseContext());

        toast = Toast.makeText(getActivity(), "",
                Toast.LENGTH_SHORT);

        // Get tracker.
        //tracker = ((FwdApplication) getActivity().getApplication()).getDefaultTracker();

        clearRulesButton = (Preference)findPreference(getString(R.string.pref_clear_rules));

        clearRulesButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //code for what you want it to do

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.alert_dialog_delete_all_rules_title)
                        .setMessage(R.string.alert_dialog_delete_all_rules_text)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                // set up the database
                                SQLiteDatabase db = new RuleDbHelper(getActivity()).getReadableDatabase();


                                db.delete(RuleContract.RuleEntry.TABLE_NAME, null, null);

                                db.close();

                                clearRulesButton.setEnabled(false);

                                // Build and send an Event.
                                //tracker.send(new HitBuilders.EventBuilder()
                                        //.setCategory(CATEGORY_RULES)
                                        //.setAction(ACTION_DELETE)
                                        //.setLabel(LABEL_DELETE_RULE)
                                        //.build());

                                Toast.makeText(getActivity(), CLEAR_RULES_COMPLETE_MESSAGE,
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .show();
                return true;
            }
        });

        versionNamePreference = (Preference)findPreference(getString(R.string.pref_version));

        versionNamePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            int versionPrefClicks = 0;

            @Override
            public boolean onPreferenceClick(Preference preference) {

                if(versionPrefClicks >= 2 && versionPrefClicks <= 3 ) {
                    toast.setText(4 - versionPrefClicks + " more...");
                    toast.show();
                }
                if(++versionPrefClicks == 5) {
                    versionPrefClicks = 0;
                    toast.setText("...");
                    toast.show();
                    Intent advancedSettingsActivity = new Intent(getActivity(), AdvancedSettingsActivity.class);
                    startActivity(advancedSettingsActivity);
                    return true;
                }
                return false;
            }


        });

        // set up click of help button - show webview
//        Preference helpButton = (Preference) findPreference(getString(R.string.pref_help_link));
//        helpButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                //code for what you want it to do
//                Intent helpActivityIntent = new Intent(getActivity(), HelpActivity.class);
//                startActivity(helpActivityIntent);
//                return true;
//            }
//        });

        // set up click of about elixsr button - show webview
        Preference aboutElixsrButton = (Preference) findPreference(getString(R.string.pref_about_link));
        aboutElixsrButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //code for what you want it to do
                Intent aboutActivityIntent = new Intent(getActivity(), AboutElixsrActivity.class);
                startActivity(aboutActivityIntent);
                return true;
            }
        });


        changeThemeToggle = (Preference) findPreference(getString(R.string.pref_dark_theme));

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Recreate our activity if we changed to dark theme
        sharedPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("pref_dark_theme")) {


                    // Build and send an Event.
                    //tracker.send(new HitBuilders.EventBuilder()
                            //.setCategory(CATEGORY_THEME)
                            //.setAction(ACTION_CHANGE)
                            //.setLabel("Dark Mode: " + sharedPreferences
                                    //.getBoolean("pref_dark_theme", false))
                            //.build());

                    Intent intent = new Intent();
                    intent.setAction(DARK_MODE_BROADCAST);
                    localBroadcastManager.sendBroadcast(intent);

                }
            }
        };

        //prevent garbage collection
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(forwardingManager.isEnabled()){
            clearRulesButton.setEnabled(false);
//            changeThemeToggle.setEnabled(false);
        }else{
            clearRulesButton.setEnabled(true);
//            changeThemeToggle.setEnabled(true);
        }

        String versionName = "Version ";
        try {
            versionName = versionName + getActivity().getBaseContext().getPackageManager()
                    .getPackageInfo(getActivity().getBaseContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "Application Version could not be found.", e);
            versionName = versionName + "not found";
        }
        versionNamePreference.setTitle(versionName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Ensure we unregister our previous listener - as it now points to a null activity
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }
}
