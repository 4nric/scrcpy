package com.genymobile.scrcpy.video;

import android.content.res.Configuration;
import android.graphics.Rect;

import com.genymobile.scrcpy.device.DisplayInfo;
import com.genymobile.scrcpy.device.Size;
import com.genymobile.scrcpy.util.Ln;
import com.genymobile.scrcpy.util.FieldHelper;
import com.genymobile.scrcpy.wrappers.ServiceManager;

public abstract class DisplayCapture extends SurfaceCapture {

    // Source display size (before resizing/crop) for the current session
    private Size sessionDisplaySize;
    private int sessionRotation;

    private synchronized Size getSessionDisplaySize() {
        return sessionDisplaySize;
    }

    protected synchronized void setSessionDisplaySize(Size sessionDisplaySize) {
        this.sessionDisplaySize = sessionDisplaySize;
    }

    private synchronized int getSessionRotation() {
        return sessionRotation;
    }

    protected synchronized void setSessionRotation(int sessionRotation) {
        this.sessionRotation = sessionRotation;
    }

    protected void handleDisplayConfigurationChanged(Configuration newConfig) {
        Ln.d("DisplayCapture: handleDisplayConfigurationChanged using WindowManager");
        Object windowConfiguration = FieldHelper.getField(newConfig,"windowConfiguration");

        assert windowConfiguration != null;
        Integer mDisplayRotation = (Integer) FieldHelper.getField(windowConfiguration, "mDisplayRotation");
        Ln.d("mDisplayRotation="+mDisplayRotation);

        if (mDisplayRotation != getSessionRotation()) {
            setSessionRotation(mDisplayRotation);
            invalidate();
        }

        /*
         * FEATURE REQUEST: resizeable vd. when scrcpy window resizes, pass new size to server
         * and let it handle the resize (wm size WxH -d ID). base dpi should be 160, but take into
         * account the scale of mirroring device.
         */
        Rect mBounds = (Rect) FieldHelper.getField(windowConfiguration, "mBounds");
        Ln.d("mBounds="+mBounds.flattenToString());
    }

    protected void handleDisplayChanged(int displayId) {
        Ln.d("DisplayCapture: handleDisplayChanged using DisplayManager");
        DisplayInfo di = ServiceManager.getDisplayManager().getDisplayInfo(displayId);
        if (di == null) {
            Ln.w("DisplayInfo for " + displayId + " cannot be retrieved");
            // We can't compare with the current size, so reset unconditionally
            if (Ln.isEnabled(Ln.Level.VERBOSE)) {
                Ln.v(getClass().getCanonicalName() + ": requestReset(): " + getSessionDisplaySize() + " -> (unknown)");
            }
            setSessionDisplaySize(null);
            invalidate();
        } else {
            Size size = di.getSize();

            // The field is hidden on purpose, to read it with synchronization
            @SuppressWarnings("checkstyle:HiddenField")
            Size sessionDisplaySize = getSessionDisplaySize(); // synchronized

            // .equals() also works if sessionDisplaySize == null
            if (!size.equals(sessionDisplaySize)) {
                // Reset only if the size is different
                if (Ln.isEnabled(Ln.Level.VERBOSE)) {
                    Ln.v(getClass().getCanonicalName() + ": requestReset(): " + sessionDisplaySize + " -> " + size);
                }
                // Set the new size immediately, so that a future onDisplayChanged() event called before the asynchronous prepare()
                // considers that the current size is the requested size (to avoid a duplicate requestReset())
                setSessionDisplaySize(size);
                invalidate();
            } else if (Ln.isEnabled(Ln.Level.VERBOSE)) {
                Ln.v(getClass().getCanonicalName() + ": Size not changed (" + size + "): do not requestReset()");
            }
        }
    }
}
