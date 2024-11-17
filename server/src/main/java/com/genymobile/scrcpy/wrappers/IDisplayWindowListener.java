package com.genymobile.scrcpy.wrappers;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteException;

import java.util.List;

public class IDisplayWindowListener extends android.view.IDisplayWindowListener.Stub {

    @Override
    public void onDisplayAdded(int displayId) {

    }

    @Override
    public void onDisplayConfigurationChanged(int displayId, Configuration newConfig) {

    }

    @Override
    public void onDisplayRemoved(int displayId) {

    }

    @Override
    public void onFixedRotationStarted(int displayId, int newRotation) {

    }

    @Override
    public void onFixedRotationFinished(int displayId) {

    }

    @Override
    public void onKeepClearAreasChanged(int displayId, List<Rect> restricted, List<Rect> unrestricted) {

    }
}
