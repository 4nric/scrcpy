package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IInterface;

import com.genymobile.scrcpy.AndroidVersions;
import com.genymobile.scrcpy.util.FieldHelper;
import com.genymobile.scrcpy.util.Ln;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("PrivateApi,BlockedPrivateApi")
public final class ActivityTaskManager {

    private final IInterface manager;
    private Method getFocusedRootTaskInfoMethod;
    private Method getAllRootTaskInfosMethod;
    private Method getAllRootTaskInfosOnDisplayMethod;
    private Method removeTaskMethod;
    private Method setFocusedTaskMethod;
    private Method moveRootTaskToDisplayMethod;

    static ActivityTaskManager create() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityTaskManager");
            Method getServiceMethod = cls.getDeclaredMethod("getService");
            IInterface atm = (IInterface) getServiceMethod.invoke(null);
            return new ActivityTaskManager(atm);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private ActivityTaskManager(IInterface manager) {
        this.manager = manager;
    }

    public static class RootTaskInfo {

        //From android.app.WindowConfiguration
        /** Activity type is currently not defined. */
        public static final int ACTIVITY_TYPE_UNDEFINED = 0;
        /** Standard activity type. Nothing special about the activity... */
        public static final int ACTIVITY_TYPE_STANDARD = 1;
        /** Home/Launcher activity type. */
        public static final int ACTIVITY_TYPE_HOME = 2;
        /** Recents/Overview activity type. There is only one activity with this type in the system. */
        public static final int ACTIVITY_TYPE_RECENTS = 3;
        /** Assistant activity type. */
        public static final int ACTIVITY_TYPE_ASSISTANT = 4;
        /** Dream activity type. */
        public static final int ACTIVITY_TYPE_DREAM = 5;

        //ActivityManager.StackInfo <= Android 11
        //ActivityTaskManager.RootTaskInfo >= Android 12
        private final Object rawRootTaskInfo;

        public RootTaskInfo(Object rawRootTaskInfo) {
            this.rawRootTaskInfo = rawRootTaskInfo;
        }

        public Object getField(String fieldName) {
            return FieldHelper.getField(rawRootTaskInfo, fieldName);
        }

        public int getTaskId() {
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12 ) {
                return (Integer) getField("taskId");
            }
            //TODO confirm if stackId == taskIds[0]
            return (Integer) getField("stackId");
        }

        public int getDisplayId() {
            return (Integer) getField("displayId");
        }

        public long getLastActiveTime() {
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12) {
                return (Long) getField("lastActiveTime");
            }
            else return 0L; //no such field before A12
        }

        public ComponentName getBaseActivity() {
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12) {
                return (ComponentName) getField("baseActivity");
            } else {
                //TODO confirm
                String[] taskNames = (String[]) getField("taskNames");
                String[] baseActivitySplit = taskNames[0].split("/");
                return new ComponentName(baseActivitySplit[0], baseActivitySplit[1]);
            }
        }

        public ComponentName getTopActivity() {
            return (ComponentName) getField("topActivity");
        }

        public Boolean getVisible(){
            return (Boolean) getField("visible");
        }

        public Configuration getConfiguration(){
            return (Configuration) getField("configuration");
        }

        public Integer getActivityType(){
            Object windowConfiguration = FieldHelper.getField(getConfiguration(),"windowConfiguration");
            return (Integer) FieldHelper.getField(windowConfiguration,"mActivityType");
        }
    }

    private Method getFocusedRootTaskInfoMethod() throws NoSuchMethodException {
        if (getFocusedRootTaskInfoMethod == null){
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12){
                getFocusedRootTaskInfoMethod = manager.getClass().getMethod("getFocusedRootTaskInfo");
            } else { //ActivityManager.StackInfo
                getFocusedRootTaskInfoMethod = manager.getClass().getMethod("getFocusedStackInfo");
            }
        }
        return getFocusedRootTaskInfoMethod;
    }

    public RootTaskInfo getFocusedRootTaskInfo(){
        try {
            Method method = getFocusedRootTaskInfoMethod();
            Object rawFocusedRootTaskInfo = method.invoke(manager);
            return new RootTaskInfo(rawFocusedRootTaskInfo);
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    private Method getAllRootTaskInfosMethod() throws NoSuchMethodException {
        if (getAllRootTaskInfosMethod == null){
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12){
                getAllRootTaskInfosMethod = manager.getClass().getMethod("getAllRootTaskInfos");
            } else { //List<ActivityManager.StackInfo>
                getAllRootTaskInfosMethod = manager.getClass().getMethod("getAllStackInfos");
            }
        }
        return getAllRootTaskInfosMethod;
    }

    public List<RootTaskInfo> getAllRootTaskInfos(){
        try {
            Method method = getAllRootTaskInfosMethod();
            List<?> rawRootTaskInfos = (List<?>) method.invoke(manager);

            List<RootTaskInfo> rootTaskInfos = new ArrayList<>();
            for (Object rawRootTaskInfo : rawRootTaskInfos) {
                rootTaskInfos.add(new RootTaskInfo(rawRootTaskInfo));
            }
            return rootTaskInfos;
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    private Method getAllRootTaskInfosOnDisplayMethod() throws NoSuchMethodException {
        if (getAllRootTaskInfosOnDisplayMethod == null){
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12) {
                getAllRootTaskInfosOnDisplayMethod = manager.getClass().getMethod("getGetAllRootTaskInfosOnDisplay", int.class);
            } else { //A11
                getAllRootTaskInfosOnDisplayMethod = manager.getClass().getMethod("getAllStackInfosOnDisplay", int.class);
            }
        }
        return getAllRootTaskInfosOnDisplayMethod;
    }

    public List<RootTaskInfo> getAllRootTaskInfosOnDisplay(int displayId){
        try {
            //No such method in A10, so simulate
            if (Build.VERSION.SDK_INT == AndroidVersions.API_29_ANDROID_10){
                List<RootTaskInfo> rootTaskInfos = getAllRootTaskInfos();
                List<RootTaskInfo> displayIdFiltered = new ArrayList<>();

                for (RootTaskInfo rootTaskInfo : rootTaskInfos){
                    if (rootTaskInfo.getDisplayId() == displayId){
                        displayIdFiltered.add(rootTaskInfo);
                    }
                }
                return displayIdFiltered;
            }

            Method method = getAllRootTaskInfosOnDisplayMethod();
            List<?> rootTaskInfos = (List<?>) method.invoke(manager,displayId);
            List<RootTaskInfo> filtered = new ArrayList<>();

            for (Object rootTaskInfo : rootTaskInfos) {
                filtered.add(new RootTaskInfo(rootTaskInfo));
            }
            return filtered;
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    private Method removeTaskMethod() throws NoSuchMethodException {
        if (removeTaskMethod == null){
            removeTaskMethod = manager.getClass().getMethod("removeTask", int.class);
        }
        return removeTaskMethod;
    }

    public Boolean removeTask(int taskId){
        try {
            Method method = removeTaskMethod();
            return (Boolean) method.invoke(manager, taskId);
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    private Method setFocusedTaskMethod() throws NoSuchMethodException {
        if (setFocusedTaskMethod == null){
            setFocusedTaskMethod = manager.getClass().getMethod("setFocusedTask", int.class);
        }
        return setFocusedTaskMethod;
    }

    public void setFocusedTask(int taskId){
        try {
            Method method = setFocusedTaskMethod();
            method.invoke(manager, taskId);
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
        }
    }

    private Method moveRootTaskToDisplayMethod() throws NoSuchMethodException {
        if (moveRootTaskToDisplayMethod == null){
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_31_ANDROID_12){
                moveRootTaskToDisplayMethod = manager.getClass().getMethod("moveRootTaskToDisplay", int.class, int.class);
            } else {
                moveRootTaskToDisplayMethod = manager.getClass().getMethod("moveStackToDisplay", int.class, int.class);
            }
        }
        return moveRootTaskToDisplayMethod;
    }

    public void moveRootTaskToDisplay(int taskId, int displayId){
        try {
            Method method = moveRootTaskToDisplayMethod();
            method.invoke(manager, taskId, displayId);
        } catch (ReflectiveOperationException e) {
            Ln.e("Could not invoke method", e);
        }
    }
}
