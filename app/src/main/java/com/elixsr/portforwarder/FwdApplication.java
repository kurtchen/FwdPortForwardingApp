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

package com.elixsr.portforwarder;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.elixsr.portforwarder.forwarding.ForwardingService;
//import com.google.android.gms.analytics.GoogleAnalytics;
//import com.google.android.gms.analytics.Logger;
//import com.google.android.gms.analytics.Tracker;

/**
 * Created by Niall McShane on 13/03/2016.
 */
public class FwdApplication extends Application {

    private static final String TAG = "FwdApplication";
    //private Tracker mTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    //synchronized public Tracker getDefaultTracker() {
        //if (mTracker == null) {
            //GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            //// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG

            //if (BuildConfig.DEBUG) {
                //mTracker = analytics.newTracker(R.xml.analytics_debug);
            //}else{
                //mTracker = analytics.newTracker(R.xml.analytics);
            //}
            //mTracker.enableAutoActivityTracking(true);
        //}
        //return mTracker;
    //}


}
