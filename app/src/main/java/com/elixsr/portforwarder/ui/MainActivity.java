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

package com.elixsr.portforwarder.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import com.elixsr.portforwarder.FwdApplication;
import com.elixsr.portforwarder.R;
import com.elixsr.portforwarder.dao.RuleDao;
import com.elixsr.portforwarder.db.RuleDbHelper;
import com.elixsr.portforwarder.forwarding.ForwardingManager;
import com.elixsr.portforwarder.forwarding.ForwardingService;
import com.elixsr.portforwarder.models.RuleModel;
import com.elixsr.portforwarder.adapters.RuleListAdapter;
import com.elixsr.portforwarder.ui.intro.MainIntro;
import com.elixsr.portforwarder.ui.preferences.HelpActivity;
import com.elixsr.portforwarder.ui.preferences.SettingsActivity;
import com.elixsr.portforwarder.ui.rules.NewRuleActivity;
//import com.google.android.gms.analytics.Tracker;


public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final String FORWARDING_MANAGER_KEY = "ForwardingManager";
    private static final String FORWARDING_SERVICE_KEY = "ForwardingService";

    private List<RuleModel> ruleModels;
    private static RuleListAdapter ruleListAdapter;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private ForwardingManager forwardingManager;
    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton fab;

    private Intent forwardingServiceIntent;
    private RuleDao ruleDao;
    private PercentRelativeLayout mRuleListEmptyView;
    //private Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.forwardingManager = ForwardingManager.getInstance();


        setContentView(R.layout.activity_main);
        setSupportActionBar(getActionBarToolbar());
//        getActionBarToolbar().setTitle(R.string.app_tag);
        getActionBarToolbar().setTitle("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_nav_logo);

        //determine if this is first start - and whether to show app intro
        onFirstStart();

        final Intent newRuleIntent = new Intent(this, NewRuleActivity.class);

        // move to the new rule activity
        this.fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(newRuleIntent);
            }
        });

        // hide the fab if forwarding is enabled
        // the user should not be able to add/delete rules
        if (this.forwardingManager.isEnabled()) {
            //if the forwarding service is enabled, then we should ensure not to show the fab
            fab.hide();
        } else {
            fab.show();
        }

        //get all models from the data store
        ruleDao = new RuleDao(new RuleDbHelper(this));
        ruleModels = ruleDao.getAllRuleModels();

        //set up rule list and empty view
        mRecyclerView = (RecyclerView) findViewById(R.id.rule_recycler_view);
        mRuleListEmptyView = (PercentRelativeLayout) findViewById(R.id.rule_list_empty_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        ruleListAdapter = new RuleListAdapter(ruleModels, forwardingManager, getApplicationContext());
        mRecyclerView.setAdapter(ruleListAdapter);

        //store the coordinator layout for snackbar
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator_layout);


        forwardingServiceIntent = new Intent(this, ForwardingService.class);


        /*
            Service stuff
         */

        //handle intents
        IntentFilter mStatusIntentFilter = new IntentFilter(
                ForwardingService.BROADCAST_ACTION);

        // Instantiates a new ForwardingServiceResponseReceiver
        ForwardingServiceResponseReceiver forwardingServiceResponseReceiver =
                new ForwardingServiceResponseReceiver();

        // Registers the ForwardingServiceResponseReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                forwardingServiceResponseReceiver,
                mStatusIntentFilter);

        // Get tracker.
        //tracker = ((FwdApplication) this.getApplication()).getDefaultTracker();

        Log.i(TAG, "Finished onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        this.ruleModels.clear();
        this.ruleModels.addAll(ruleDao.getAllRuleModels());
        this.ruleListAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();

        //decide whether to show the rule list or the empty view
        if (this.ruleModels.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mRuleListEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mRuleListEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_toggle_forwarding).setTitle(generateForwardingActionMenuText(forwardingManager.isEnabled()));

        //setup the start forwarding button
        MenuItem toggleForwarding = menu.findItem(R.id.action_toggle_forwarding);

        int enabledRuleModels = 0;

        for (RuleModel ruleModel : ruleModels) {
            if(ruleModel.isEnabled()) {
                ++enabledRuleModels;
            }
        }

        //it should not be able to start if there are no rules
        if (enabledRuleModels <= 0) {
            toggleForwarding.setVisible(false);
        } else {
            toggleForwarding.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent prefIntent = new Intent(this, SettingsActivity.class);
                startActivity(prefIntent);
                break;
            case R.id.action_toggle_forwarding:
                handleForwardingButton(item);
                break;
            case R.id.action_help:
                Intent helpActivityIntent = new Intent(this, HelpActivity.class);
                startActivity(helpActivityIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleForwardingButton(MenuItem item) {


        if (!forwardingManager.isEnabled()) {
//                    startPortForwarding();

            Snackbar.make(this.coordinatorLayout, R.string.snackbar_port_forwarding_started_text, Snackbar.LENGTH_LONG)
                    .setAction("Stop", null).show();

            fab.hide();

            startService(forwardingServiceIntent);
        } else {
            //stop forwarding
            fab.show();

            Snackbar.make(this.coordinatorLayout, R.string.snackbar_port_forwarding_stopped_text, Snackbar.LENGTH_LONG).show();

            stopService(forwardingServiceIntent);
        }

        Log.i(TAG, "Forwarding Enabled: " + forwardingManager.isEnabled());
        item.setTitle(generateForwardingActionMenuText(forwardingManager.isEnabled()));
    }


    private String generateForwardingActionMenuText(boolean forwardingFlag) {
        if (forwardingFlag) {
            return this.getString(R.string.action_stop_forwarding);
        }
        return this.getString(R.string.action_start_forwarding);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(FORWARDING_MANAGER_KEY, this.forwardingManager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        stopService(forwardingServiceIntent);
        Log.i(TAG, "Destroyed");
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class ForwardingServiceResponseReceiver extends BroadcastReceiver {
        // Prevents instantiation
        private ForwardingServiceResponseReceiver() {
        }

        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * Handle Intents here.
             */

            if (intent.getExtras().containsKey(ForwardingService.PORT_FORWARD_SERVICE_STATE)) {
                Log.i(TAG, "Response from ForwardingService, Forwarding status has changed.");
                Log.i(TAG, "Forwarding status has changed to " + String.valueOf(intent.getExtras().getBoolean(ForwardingService.PORT_FORWARD_SERVICE_STATE)));
                invalidateOptionsMenu();
            }

            if (intent.getExtras().containsKey(ForwardingService.PORT_FORWARD_SERVICE_ERROR_MESSAGE)) {

                Toast.makeText(context, intent.getExtras().getString(ForwardingService.PORT_FORWARD_SERVICE_ERROR_MESSAGE),
                        Toast.LENGTH_SHORT).show();
                Snackbar.make(coordinatorLayout, R.string.snackbar_port_forwarding_stopped_text, Snackbar.LENGTH_SHORT).show();
                fab.show();

            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        
        finish();
    }

    private void onFirstStart() {
        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (isFirstStart) {

                    //  Launch app intro
                    Intent i = new Intent(MainActivity.this, MainIntro.class);
                    startActivity(i);

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", false);

                    //  Apply changes
                    e.apply();
                }
            }
        });

        // Start the thread
        t.start();
    }
}
