/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tatarka.support.job.sample.service;

import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import me.tatarka.support.job.JobInfo;
import me.tatarka.support.job.JobParameters;
import me.tatarka.support.job.JobScheduler;
import me.tatarka.support.job.JobService;
import me.tatarka.support.job.sample.MainActivity;

import java.util.LinkedList;


/**
 * Service to handle callbacks from the JobScheduler. Requests scheduled with the JobScheduler
 * ultimately land on this service's "onStartJob" method. Currently all this does is post a message
 * to the app's main activity to change the state of the UI.
 */
public class TestJobService extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    /**
     * When the app's MainActivity is created, it starts this service. This is so that the activity
     * and this service can communicate back and forth. See "setUiCalback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Messenger callback = intent.getParcelableExtra("messenger");
        Message m = Message.obtain();
        m.what = MainActivity.MSG_SERVICE_OBJ;
        m.obj = this;
        try {
            callback.send(m);
        } catch (RemoteException e) {
            Log.e(TAG, "Error passing service object back to activity.");
        }
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // We don't do any real 'work' in this sample app. All we'll
        // do is track which jobs have landed on our service, and
        // update the UI accordingly.
        jobParamsMap.add(params);
        if (mActivity != null) {
            mActivity.onReceivedStartJob(params);
        }
        Log.i(TAG, "on start job: " + params.getJobId());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Stop tracking these job parameters, as we've 'finished' executing.
        jobParamsMap.remove(params);
        if (mActivity != null) {
            mActivity.onReceivedStopJob();
        }
        Log.i(TAG, "on stop job: " + params.getJobId());
        return true;
    }

    MainActivity mActivity;
    private final LinkedList<JobParameters> jobParamsMap = new LinkedList<JobParameters>();

    public void setUiCallback(MainActivity activity) {
        mActivity = activity;
    }

    /**
     * Send job to the JobScheduler.
     */
    public void scheduleJob(JobInfo t) {
        Log.d(TAG, "Scheduling job");
        JobScheduler tm = JobScheduler.getInstance(this);
        tm.schedule(t);
    }

    public boolean callJobFinished() {
        JobParameters params = jobParamsMap.poll();
        if (params == null) {
            return false;
        } else {
            Log.i(TAG, "job finished: " + params.getJobId());
            jobFinished(params, false);
            return true;
        }
    }

    public boolean callJobFailed() {
        JobParameters params = jobParamsMap.poll();
        if (params == null) {
            return false;
        } else {
            Log.i(TAG, "job failed: " + params.getJobId());
            jobFinished(params, true);
            return true;
        }
    }
}
