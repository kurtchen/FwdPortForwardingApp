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

package com.elixsr.portforwarder.ui.rules;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.elixsr.core.common.widgets.SwitchBar;
import com.elixsr.portforwarder.FwdApplication;
import com.elixsr.portforwarder.R;
import com.elixsr.portforwarder.db.RuleContract;
import com.elixsr.portforwarder.models.RuleModel;
import com.elixsr.portforwarder.db.RuleDbHelper;
import com.elixsr.portforwarder.ui.MainActivity;
import com.elixsr.portforwarder.util.RuleHelper;
//import com.google.android.gms.analytics.HitBuilders;
//import com.google.android.gms.analytics.Tracker;

/**
 * Created by Niall McShane on 02/03/2016.
 */
public class EditRuleActivity extends BaseRuleActivity {

    private static final String TAG = "EditRuleActivity";

    private static final String NO_RULE_ID_FOUND_LOG_MESSAGE = "No ID was supplied to EditRuleActivity";
    private static final String NO_RULE_ID_FOUND_TOAST_MESSAGE = "Could not locate rule";

    private static final String ACTION_DELETE = "Delete";
    private static final String LABEL_DELETE_RULE = "Delete Rule";
    private static final String LABEL_UPDATE_RULE = "Rule Updated";


    private RuleModel ruleModel;

    private long ruleModelId;

    private SQLiteDatabase db;
    //private Tracker tracker;
    private SwitchBar switchBar;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //if we can't locate the id, then we can't continue
        if(!getIntent().getExtras().containsKey(RuleHelper.RULE_MODEL_ID)){

            /// show toast containing message to the user
            Toast.makeText(this, NO_RULE_ID_FOUND_TOAST_MESSAGE,
                    Toast.LENGTH_SHORT).show();

            Log.e(TAG, NO_RULE_ID_FOUND_LOG_MESSAGE);

            onBackPressed();

            //return from the method - ensure we don't continue
            return;
        }
        ruleModelId = getIntent().getExtras().getLong(RuleHelper.RULE_MODEL_ID);

        setContentView(R.layout.edit_rule_activity);

        //set up toolbar
        Toolbar toolbar = getActionBarToolbar();
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_close_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });




        //use the base class to construct the common UI
        constructDetailUi();

        //TODO: move this
        this.db = new RuleDbHelper(this).getReadableDatabase();

        Cursor cursor = db.query(
                RuleContract.RuleEntry.TABLE_NAME,
                RuleDbHelper.generateAllRowsSelection(),
                RuleContract.RuleEntry.COLUMN_NAME_RULE_ID + "=?",
                new String[]{String.valueOf(ruleModelId)},
                null,
                null,
                null
        );

        cursor.moveToFirst();

        this.ruleModel = RuleHelper.cursorToRuleModel(cursor);
        Log.i(TAG, Boolean.toString(ruleModel.isEnabled()));
        //close the DB
        cursor.close();
        db.close();

        // Set up the switchBar for enabling/disabling
        switchBar = (SwitchBar) findViewById(R.id.switch_bar);
        switchBar.show();
        switchBar.setChecked(this.ruleModel.isEnabled());
        /*
        Set the text fields content
         */
        TextInputEditText newRuleNameEditText = (TextInputEditText) findViewById(R.id.new_rule_name);
        newRuleNameEditText.setText(ruleModel.getName());

        TextInputEditText newRuleFromPortEditText = (TextInputEditText) findViewById(R.id.new_rule_from_port);
        newRuleFromPortEditText.setText(String.valueOf(ruleModel.getFromPort()));

        TextInputEditText newRuleTargetIpAddressEditText = (TextInputEditText) findViewById(R.id.new_rule_target_ip_address);
        newRuleTargetIpAddressEditText.setText(ruleModel.getTargetIpAddress());

        TextInputEditText newRuleTargetPortEditText = (TextInputEditText) findViewById(R.id.new_rule_target_port);
        newRuleTargetPortEditText.setText(String.valueOf(ruleModel.getTargetPort()));

        /*
        Set the spinners content
         */
        //from interface spinner
        Log.i(TAG, "FROM SPINNER : " + fromInterfaceSpinner.toString());
        Log.i(TAG, "FROM INTERFACE : " + this.ruleModel.getFromInterfaceName());
        fromInterfaceSpinner.setSelection(fromSpinnerAdapter.getPosition(this.ruleModel.getFromInterfaceName()));

        //protocol spinner
        protocolSpinner.setSelection(protocolAdapter.getPosition(RuleHelper.getRuleProtocolFromModel(this.ruleModel)));
        
        
        //set up tracking
        // Get tracker.
        //tracker = ((FwdApplication) this.getApplication()).getDefaultTracker();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_rule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_save_rule:
                Log.i(TAG, "Save Menu Button Clicked");


                //set the item to disabled while saving
                item.setEnabled(false);
                saveEditedRule();
                item.setEnabled(true);
                break;
            case R.id.action_delete_rule:
                deleteRule();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveEditedRule(){
        this.ruleModel = generateNewRule();

        if(ruleModel.isValid()) {
            // Determine if rule is enabled
            this.ruleModel.setEnabled(switchBar.isChecked());

            Log.i(TAG, "Rule " + ruleModel.getName() + " is valid, time to update.");
            SQLiteDatabase db = new RuleDbHelper(this).getReadableDatabase();

            Log.i(TAG, "Is enabled is: " + this.ruleModel.isEnabled() );

            // New model to store
            ContentValues values = RuleHelper.ruleModelToContentValues(this.ruleModel);

            // Which row to update, based on the ID
            String selection = RuleContract.RuleEntry.COLUMN_NAME_RULE_ID + "=?";
            String[] selectionArgs = {String.valueOf(this.ruleModelId)};

            this.db = new RuleDbHelper(this).getReadableDatabase();

            int count = db.update(
                    RuleContract.RuleEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

            //close db
            db.close();

            // Build and send an Event.
            //tracker.send(new HitBuilders.EventBuilder()
                    //.setCategory(CATEGORY_RULES)
                    //.setAction(ACTION_SAVE)
                    //.setLabel(LABEL_UPDATE_RULE)
                    //.build());


            // move to main activity
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
            startActivity(mainActivityIntent);
            finish();
        }else{
            Toast.makeText(this, R.string.toast_error_rule_not_valid_text,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void deleteRule(){

        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_dialog_delete_entry_title)
                .setMessage(R.string.alert_dialog_delete_entry_text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete

                        //TODO: add exception handling
                        //TODO: add db delete
//                        MainActivity.RULE_MODELS.remove(ruleModelLocation);
//                        MainActivity.ruleListAdapter.notifyItemRemoved(ruleModelLocation);

                        //construct the db
                        db = new RuleDbHelper(getBaseContext()).getReadableDatabase();

                        // Define 'where' part of query.
                        String selection = RuleContract.RuleEntry.COLUMN_NAME_RULE_ID + "=?";
                        // Specify arguments in placeholder order.
                        String[] selectionArgs = { String.valueOf(ruleModelId) };
                        // Issue SQL statement.
                        db.delete(RuleContract.RuleEntry.TABLE_NAME, selection, selectionArgs);

                        //close the db
                        db.close();

                        // Build and send an Event.
                        //tracker.send(new HitBuilders.EventBuilder()
                                //.setCategory(CATEGORY_RULES)
                                //.setAction(ACTION_DELETE)
                                //.setLabel(LABEL_DELETE_RULE)
                                //.build());

                        // move to main activity
                        Intent mainActivityIntent = new Intent(getBaseContext(), MainActivity.class);
                        finish();
                        startActivity(mainActivityIntent);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();


    }

}
