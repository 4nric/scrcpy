package com.genymobile.scrcpy.wrappers;

import android.app.IProcessObserver;

public class ProcessObserver extends IProcessObserver.Stub {

    @Override
    public void onProcessStarted(int pid, int processUid, int packageUid, String packageName, String processName) {
        // empty default implementation
    }

    @Override
    public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
        // empty default implementation
    }

    @Override
    public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        // empty default implementation
    }

    @Override
    public void onProcessDied(int pid, int uid) {
        // empty default implementation
    }
}
