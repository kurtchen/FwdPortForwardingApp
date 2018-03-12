package com.elixsr.portforwarder.forwarding;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import com.elixsr.portforwarder.db.RuleDbHelper;
import com.elixsr.portforwarder.dao.RuleDao;
import com.elixsr.portforwarder.models.RuleModel;

public class ExternalCommandReceiver extends BroadcastReceiver {
    private static String TAG = "PortFwdExternalCommandReceiver";

    private static String INTENT_EXTERNAL_COMMAND = "com.elixsr.portforwarder.forwarding.EXTERNAL_COMMAND";
    private static String EXTERNAL_COMMAND_KEY_START_SERVICE = "start_service";
    private static String EXTERNAL_COMMAND_KEY_STOP_SERVICE = "stop_service";
    private static String EXTERNAL_COMMAND_KEY_CHECK_SERVICE = "check_service";
    private static String EXTERNAL_COMMAND_KEY_UPDATE_RULES = "update_rules";
    private static String EXTERNAL_COMMAND_KEY_LIST_RULES = "list_rules";

    private static String DEFAULT_UPDATE_RULES_FILE = "/sdcard/port_fwd_update_rules.sql";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (INTENT_EXTERNAL_COMMAND.equals(intent.getAction())) {
            ForwardingManager forwardingManager = ForwardingManager.getInstance();

            if (null != intent.getStringExtra(EXTERNAL_COMMAND_KEY_START_SERVICE)) {
                if (!forwardingManager.isEnabled()) {
                    Log.d(TAG, "Starting service...");
                    Intent serviceIntent = new Intent(context, ForwardingService.class);
                    context.startService(serviceIntent);
                } else {
                    Log.d(TAG, "Service already started");
                }
            } else if (null != intent.getStringExtra(EXTERNAL_COMMAND_KEY_STOP_SERVICE)) {
                if (forwardingManager.isEnabled()) {
                    Log.d(TAG, "Stopping service...");
                    Intent serviceIntent = new Intent(context, ForwardingService.class);
                    context.stopService(serviceIntent);
                } else {
                    Log.d(TAG, "Service already stopped");
                }
            } else if (null != intent.getStringExtra(EXTERNAL_COMMAND_KEY_CHECK_SERVICE)) {
                Log.d(TAG, "Service status: " + (forwardingManager.isEnabled() ? "STARTED" : "STOPPED"));
            } else if (null != intent.getStringExtra(EXTERNAL_COMMAND_KEY_UPDATE_RULES)) {
                String sqlFile = intent.getStringExtra(EXTERNAL_COMMAND_KEY_UPDATE_RULES);
                if (sqlFile.trim().length() == 0) {
                    sqlFile = DEFAULT_UPDATE_RULES_FILE;
                }
                Log.d(TAG, "Update rules with " + sqlFile);
                File f = new File(sqlFile);
                if (f.exists()) {
                    BufferedReader br = null;
                    String line = null;
                    RuleDbHelper dbHelper = new RuleDbHelper(context);
                    SQLiteDatabase db = null;
                    try {
                        db = dbHelper.getWritableDatabase();
                        if (db != null) {
                            db.beginTransaction();
                            br = new BufferedReader(new FileReader(f));
                            line = br.readLine();
                            do {
                                if (line.trim().length() > 0 && !line.trim().startsWith("#")) {
                                    db.execSQL(line);
                                }
                                line = br.readLine();
                            } while (line != null);
                            db.setTransactionSuccessful();
                            Log.d(TAG, "Rules updated");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to update the rules: " + e.getMessage());
                    } finally {
                        if (db != null) {
                            db.endTransaction();
                        }

                        try{ dbHelper.close(); } catch (Exception e1) {}

                        if (br != null) {
                            try{ br.close(); } catch (Exception e1) {}
                        }
                    }
                } else {
                    Log.d(TAG, "File not found: " + sqlFile);
                }
            } else if (null != intent.getStringExtra(EXTERNAL_COMMAND_KEY_LIST_RULES)) {
                RuleDao ruleDao = new RuleDao(new RuleDbHelper(context));
                List<RuleModel> ruleModels = ruleDao.getAllRuleModels();
                if (ruleModels != null && ruleModels.size() > 0) {
                    Log.d(TAG, "rule_id|name|is_tcp|is_udp|from_interface_name|from_port|target_ip_address|target_port|is_enabled");
                    for (RuleModel r : ruleModels) {
                        Log.d(TAG, "" + r.getId() + "|" + r.getName() + "|"
                                + r.isTcp() + "|" + r.isUdp() + "|" + r.getFromInterfaceName()
                                + "|" + r.getFromPort() + "|" + r.getTargetIpAddress()
                                + "|" + r.getTargetPort() + "|" + r.isEnabled());
                    }
                } else {
                    Log.d(TAG, "No rules found");
                }
            } else {
                Log.d(TAG, "Unknown command");
            }
        }
    }
}
