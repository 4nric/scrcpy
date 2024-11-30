package com.genymobile.scrcpy.device;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;

import com.genymobile.scrcpy.FakeContext;
import com.genymobile.scrcpy.util.Ln;
import com.genymobile.scrcpy.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IntentQuery {

    public static final int QUERY_LABEL = 0;
    public static final int QUERY_PACKAGE = 1;

    private IntentQuery() {
        // not instantiable
    }

    private static String queryTypeToString(int queryType){
        return queryType== QUERY_PACKAGE ? "package name" : queryType== QUERY_LABEL ? "app name" : "?";
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static Intent getIntentFromClassName(String packageName, String className, boolean skipCheck){
        Intent intent = new Intent();
        className = className.startsWith(".")?packageName+className:className;
        intent.setClassName(packageName, className);
        if (skipCheck) {
            return intent;
        }

        Context context = FakeContext.get();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (list.size() == 1){
            Ln.i("Starting activity {"+packageName+"/"+className+"}");
            return buildReturnIntent(list.get(0).activityInfo, getCurrentModeType(context), pm)
                    .setClassName(packageName, className);
        } else {
            Ln.e("Activity class {"+packageName+"/"+className+"} does not exist");
            return null;
        }
    }

    public static Intent getLaunchIntent(String query, int queryType){
        Context context = FakeContext.get();
        PackageManager pm = context.getPackageManager();
        int uiModeType = getCurrentModeType(context);

        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> similar = new ArrayList<>();
        List<ApplicationInfo> launchableSimilar = new ArrayList<>();
        List<ApplicationInfo> launchableExact = new ArrayList<>();
        List<ApplicationInfo> notLaunchableExact = new ArrayList<>();

        for (ApplicationInfo appInfo : list) {
            String appInfoPkg = appInfo.packageName;
            String labelLowerCase = "";
            String appInfoLabelLowerCase = "";

            if (queryType== QUERY_LABEL){
                labelLowerCase = query.toLowerCase(Locale.getDefault());
                appInfoLabelLowerCase = appInfo.loadLabel(pm).toString().toLowerCase(Locale.getDefault());
            }

            if ((queryType== QUERY_PACKAGE && !appInfoPkg.contains(query)) ||
                    (queryType== QUERY_LABEL && !appInfoLabelLowerCase.contains(labelLowerCase))) {
                continue;
            }

            similar.add(appInfo);
            Intent launchIntent = getLaunchIntentForUIModeType(appInfoPkg, uiModeType, pm);

            boolean isEquals = (queryType == QUERY_PACKAGE && appInfoPkg.equals(query)) ||
                    (queryType == QUERY_LABEL && appInfoLabelLowerCase.equals(labelLowerCase));
            if (launchIntent != null) {
                if (isEquals){
                    launchableExact.add(appInfo);
                } else {
                    launchableSimilar.add(appInfo);
                }
            } else if (isEquals){
                notLaunchableExact.add(appInfo);
            }
        }

        Ln.d("list="+list.size()
                +" similar="+similar.size()
                + " launchableSimilar="+launchableSimilar.size()
                + " launchableExact="+launchableExact.size()
                + " notLaunchableExact="+notLaunchableExact.size()
        );

        String queryLogStr = queryTypeToString(queryType)+" \""+query+"\"";
        if (launchableExact.isEmpty() && notLaunchableExact.isEmpty()) {
            Ln.e("No app found with exact "+queryLogStr);
        } else if (!notLaunchableExact.isEmpty()){
            String title = "There's no launch intent for:";
            Ln.w(LogUtils.buildAppListMessage(title, notLaunchableExact, pm));
        }

        if (!launchableSimilar.isEmpty()){
            int count = launchableSimilar.size();
            String warningMsg = "Found " + count + " other similar "+(count == 1 ? "app that includes " : "apps that include ") + queryLogStr;
            Ln.w(LogUtils.buildAppListMessage(warningMsg,launchableSimilar,pm));
        }

        // There must be at least 1 exact match from here:
        // Exactly 1 if querying for a package name, 1 or more if querying for a label.
        if (queryType== QUERY_PACKAGE){
            return buildReturnIntent(launchableExact.get(0), uiModeType, pm);
        } else if (queryType== QUERY_LABEL){
            int count = launchableExact.size();
            if (count == 1) {
                return buildReturnIntent(launchableExact.get(0), uiModeType, pm);
            } else if (count > 1) {
                String title = "Found multiple apps with exact "+queryLogStr;
                Ln.e(LogUtils.buildAppListMessage(title, launchableExact, pm));
                return null;
            }
        }
        return null;
    }

    public static Intent getLaunchIntentForUIModeType(String packageName, int uiModeType, PackageManager pm){
        return uiModeType == Configuration.UI_MODE_TYPE_TELEVISION ?
                pm.getLeanbackLaunchIntentForPackage(packageName):
                pm.getLaunchIntentForPackage(packageName);
    }

    public static int getCurrentModeType(Context context){
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType();
    }

    @SuppressLint("QueryPermissionsNeeded")
    private static List<ResolveInfo> getDrawerApps() {
        Context context = FakeContext.get();
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        intent.addCategory(uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION?
                Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER);

        return pm.queryIntentActivities(intent, 0);
    }

    private static Intent buildReturnIntent(Object info, int uiModeType, PackageManager pm){
        Class<?> infoClass = info.getClass();
        String appLabel = "";
        String packageName = "";

        if (infoClass == ApplicationInfo.class) {
            ApplicationInfo appInfo = (ApplicationInfo) info;
            appLabel = appInfo.loadLabel(pm).toString();
            packageName = appInfo.packageName;
        } else if (infoClass == ActivityInfo.class) {
            ActivityInfo appInfo = (ActivityInfo) info;
            appLabel = appInfo.loadLabel(pm).toString();
            packageName = appInfo.packageName;
        }

        return getLaunchIntentForUIModeType(packageName, uiModeType, pm)
                .putExtra("APP_LABEL", appLabel);
    }
}
